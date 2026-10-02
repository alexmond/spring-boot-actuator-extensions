# Actuator MCP starter — design

Date: 2026-10-02
Status: awaiting review

## Goal

Add a published starter, `spring-boot-actuator-mcp-starter`, that exposes Spring Boot Actuator
endpoints to AI assistants as MCP (Model Context Protocol) tools, from inside the application.

It serves two users with one set of safe defaults:

- a developer whose coding assistant inspects an app running locally;
- an operator whose assistant inspects deployed services.

Done means: an MCP client (for example Claude Code) connects to the sample app on its management
port, lists the actuator tools, and calls health, metrics and loggers; sensitive values are masked;
the starter is released on Maven Central for the Boot 4.1 (`main`) and Boot 4.0 (`4.0`) lines.
Boot 3.5 is out of scope (end-of-life).

## Decisions

| Topic | Decision |
|---|---|
| Boot lines | 4.1 (`main`) first, then ported to `4.0`. Not `3.5`. |
| MCP library | Spring AI's MCP WebMVC transport (`org.springframework.ai:mcp-spring-webmvc`, 2.0.x) plus the MCP Java SDK. **Not** Spring AI's auto-configuring server starter — see "Refinement" below. |
| MCP server | A **dedicated** MCP server for actuator tools. The app's own Spring AI MCP server, if any, is not touched. |
| Port | The **management** port. Never the main application port when the two differ. |
| Path | `/actuator/mcp` — `<management base path>/mcp`. |
| Tool shape | Generic discovery of every included endpoint, plus tuned descriptions for common endpoints. |
| Exposure | `management.endpoints.mcp.exposure.include/exclude`. Include is empty by default. |
| Write operations | No separate switch. Follow Boot's per-endpoint access settings. |
| Transport | Streamable HTTP only. |

## Prior art

No published, maintained in-process starter was found (checked 2026-10-02). The nearest projects:
`sigachev/spring-ops-mcp` (hand-written tools, Spring AI 1.1, not on Maven Central, inactive) and
`cote/ai-mgmt-probe` (a probe app using a custom endpoint discoverer; no licence, so ideas only,
no code reuse). The others are external sidecars that call actuator over HTTP.

## Feasibility probe (2026-10-02)

A throwaway app was built and run on Boot 4.0.8 and 4.1.1 with Spring AI 2.0.1. Results on both:

- Spring AI 2.0.1 builds and starts (it is itself built against Boot 4.1.1).
- Boot's endpoint discovery classes can be used outside web/JMX; a real `tools/call` for health
  returned the health JSON.
- A `RouterFunction` bean declared in a `@ManagementContextConfiguration` class is served on the
  management port. The main port returned 404 for the MCP path.
- The configuration class must sit outside the app's component scan, registered through
  `META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports`.
- Operation results must be serialised with the Jackson 3 `JsonMapper`; `toString()` is useless.

The probe used Spring AI's single auto-configured server. The dedicated server in this design is
built from the same classes but is not yet proven; it is the first implementation step.

## Refinement found while planning (2026-10-02)

Spring AI's `McpServerAutoConfiguration` creates its server with `@ConditionalOnMissingBean` on the
type `McpSyncServer`, and collects **every** `SyncToolSpecification` bean in the context. So:

- The starter depends on `mcp-spring-webmvc` (the transport class only, no auto-configuration),
  not on `spring-ai-starter-mcp-server-webmvc`. Adding the starter would start an empty Spring AI
  server on the main port in every consumer app.
- The dedicated server, its transport and its tool list are **not** registered as beans of the
  MCP types. They live inside one holder bean, `ActuatorMcpServer`. This way an app that adds
  Spring AI's server itself keeps it, with only its own tools.

This resolves open risk 1 below.

## Architecture

Package `org.alexmond.actuator.mcp`. Each unit has one job.

1. **`McpEndpointProperties`** — `@ConfigurationProperties("management.endpoints.mcp")`.
   - `enabled` (default `true`; the starter is still inert until something is included)
   - `exposure.include`, `exposure.exclude` (default: empty)
   - `max-response-chars` (default `20000`)
2. **`McpEndpointDiscoverer`** — finds the endpoints and operations to expose.
   - Built on Boot's `EndpointDiscoverer` infrastructure.
   - Endpoint filter: include/exclude from the properties.
   - Operation filter: Boot's `OperationFilter.byAccess(EndpointAccessResolver)`.
   - Skips endpoints whose output cannot be sent as text (`heapdump`, `logfile`).
   - Technology-specific endpoints (`@WebEndpoint`, `@JmxEndpoint`, for example `prometheus`) and
     web extensions are not exposed; tools call the core `@Endpoint` operations.
3. **`ActuatorToolFactory`** — turns one operation into one MCP tool specification.
   - Name: `actuator_<endpoint>` for a single read operation; `actuator_<endpoint>_<operation>`
     when an endpoint has several operations.
   - Input schema: generated from the operation's parameters (name, type, required).
   - Description: from `ToolDescriptions` when present, else generated from the endpoint id,
     operation type and parameters.
   - Call handler: invoke the operation in-process, serialise with `JsonMapper`, pass through
     `ResponseLimiter`. Failures become an MCP error result with a short message.
4. **`ToolDescriptions`** — hand-written descriptions for `health`, `info`, `metrics`, `loggers`,
   `env`, `configprops`, `beans`, `mappings`. They steer the assistant to narrow calls (one metric
   by name, one logger by name).
5. **`ResponseLimiter`** — cuts a response at `max-response-chars` and appends a marker saying how
   much was dropped and how to narrow the call.
6. **`ActuatorMcpAutoConfiguration`** — main-context auto-configuration.
   - Active only for servlet web apps with actuator and the Spring AI MCP classes present, and
     `management.endpoints.mcp.enabled=true`.
   - Creates the discoverer, the tool specifications, a dedicated
     `WebMvcStreamableServerTransportProvider` and a dedicated `McpSyncServer`.
   - These beans have distinct names and must not replace or collide with Spring AI's own
     auto-configured server beans.
7. **`ActuatorMcpManagementContextConfiguration`** — `@ManagementContextConfiguration`.
   - Publishes the dedicated transport's `RouterFunction` at `<base-path>/mcp`.
   - With a separate management port it lives in the management child context only.
   - With no separate management port, Boot applies it to the main context, so the endpoint is on
     the single shared port. That is Boot's normal meaning of "management port".

### Data flow

MCP client → `POST /actuator/mcp` on the management port → dedicated transport → dedicated MCP
server → tool call handler → actuator operation (in-process) → JSON → size limit → MCP result.

The actuator web endpoints do not need to be exposed for this to work.

## Safety

- **Nothing exposed by default.** With an empty include list no tools are registered.
- **Per-endpoint access is respected.** `management.endpoint.<id>.access`,
  `management.endpoints.access.default` and `management.endpoints.access.max-permitted` decide
  which operations exist:
  - `none` → no tools for that endpoint;
  - `read-only` → read tools only;
  - `unrestricted` → read, write and delete tools.

  Consequence to document clearly: including `loggers` with Boot's default access exposes the
  "set log level" tool. Users who want read-only MCP set `access=read-only` on that endpoint or
  `max-permitted=read-only` globally. `shutdown` has access `none` by default, so it is not
  exposed unless the user enables it in actuator itself.
- **Sanitizing.** Operations are invoked in-process, so Boot's own value masking applies
  (`show-values` settings and all `SanitizingFunction` beans). The sanitizer starter from this
  repo applies automatically when present. This starter does not depend on it.
- **Operations are invoked with no security principal.** With `show-values=when-authorized` or
  `show-details=when-authorized`, values and details stay hidden. This is the safe direction and
  will be documented.
- **Authentication** is the application's job. The docs show how to protect `/actuator/mcp` with
  Spring Security. The starter adds no authentication of its own.

## Error handling

- Unknown or missing tool arguments → MCP error result naming the parameter.
- Operation throws → MCP error result with the exception message; no stack trace.
- Result cannot be serialised → MCP error result; logged at `WARN`.
- Null result → empty text result.

## Build and dependencies

- New module added to the always-on `<modules>` list in the parent POM.
- `spring-ai-bom` imported in the parent `dependencyManagement`, version in a property.
- Module dependencies: `spring-boot-actuator-autoconfigure`, `spring-webmvc`,
  `org.springframework.ai:mcp-spring-webmvc`, `io.modelcontextprotocol.sdk:mcp-json-jackson3`.
  `spring-ai-starter-mcp-server-webmvc` is a **test** dependency only (coexistence test).
- Java 17, Lombok, 4-space indentation, no formatter plugins — as the other starters.
- Auto-configuration registered in `AutoConfiguration.imports`; the management configuration in
  `ManagementContextConfiguration.imports`.

## Testing

Unit tests:

- include/exclude filtering;
- access filtering (`none`, `read-only`, `unrestricted`);
- tool naming, input-schema generation, description lookup;
- response truncation;
- each error-handling case.

Full-context tests (`@SpringBootTest`, random ports):

- separate management port: MCP answers on the management port, main port returns 404;
- shared port: MCP answers at `/actuator/mcp`;
- `tools/list` and `tools/call` through an MCP client for health, a metric and loggers;
- a masked value in `env` stays masked;
- an app that also defines its own Spring AI MCP tool keeps that tool on its own server, and the
  actuator server does not list it.

The module must pass the 80% JaCoCo line-coverage gate. The sample app gets the starter and a
manual check from Claude Code before release.

## Documentation and release

- New Antora page `docs/modules/ROOT/pages/actuator-mcp.adoc`; entries in `index.adoc`, the
  README module list and both changelog copies.
- Released as part of the normal release for each line: `4.1.1.2` from `main`, then `4.0.8.2`
  from `4.0` after the port. The docs hub tags are swapped after each release.

## Not in v1

- stdio transport, legacy SSE transport.
- MCP resources and prompts.
- WebFlux applications.
- Audit logging of tool calls.
- A bridge that also registers the actuator tools in the app's own Spring AI MCP server.

## Open risks

1. **Dedicated server wiring.** Resolved by the refinement above; still pinned by a full-context
   test that runs with Spring AI's own server on the classpath.
2. **Spring AI and future Boot bumps.** Each Boot patch or minor bump now also needs a compatible
   Spring AI release. The boot-upgrade survey must check this for both lines.
3. **Path with a custom base path.** `/actuator/mcp` must follow
   `management.endpoints.web.base-path` and `management.server.base-path`; covered by a test.

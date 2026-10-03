# Actuator MCP Starter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `spring-boot-actuator-mcp-starter`, which exposes included Spring Boot Actuator endpoints as MCP tools on a dedicated MCP server served at `/actuator/mcp` on the management port.

**Architecture:** A third actuator "exposure technology": `McpEndpointDiscoverer` (a subclass of Boot's `EndpointDiscoverer`) finds `@Endpoint` operations filtered by `management.endpoints.mcp.exposure.*` and Boot's per-endpoint access settings; `ActuatorToolFactory` turns each operation into an MCP tool; `ActuatorMcpServer` holds a dedicated MCP server + Spring AI WebMVC transport; a `@ManagementContextConfiguration` publishes the transport's `RouterFunction` in the management context.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring AI `mcp-spring-webmvc` 2.0.1, MCP Java SDK 2.0.0 (`mcp-core`, `mcp-json-jackson3`), Jackson 3, Lombok, JUnit 5, AssertJ, Maven, JaCoCo.

**Spec:** `docs/superpowers/specs/2026-10-02-actuator-mcp-starter-design.md` — read it first.

## Global Constraints

- Branch `actuator-mcp-starter` (from `main`). Project version `4.1.1.2-SNAPSHOT`; Boot parent `4.1.1`.
- Boot lines: 4.1 (`main`) and 4.0 (`4.0`). Never `3.5`.
- Package `org.alexmond.actuator.mcp`; module `spring-boot-actuator-mcp-starter`.
- Java 17. 4-space indentation, no tabs. No spring-javaformat/Checkstyle/PMD. Lombok allowed.
- MCP endpoint path: `<management.endpoints.web.base-path>/mcp` (default `/actuator/mcp`), on the management port.
- `management.endpoints.mcp.exposure.include` is empty by default → no tools.
- Write/delete tools follow Boot's `management.endpoint.<id>.access`, `management.endpoints.access.default`, `management.endpoints.access.max-permitted`. No separate MCP switch.
- Never register `McpSyncServer`, `McpStreamableServerTransportProvider` or `SyncToolSpecification` as Spring beans (Spring AI's own server backs off / collects them).
- Do **not** depend on `spring-ai-starter-mcp-server-webmvc` at compile scope (test scope only).
- Skip endpoints `heapdump` and `logfile`.
- Default `management.endpoints.mcp.max-response-chars` = `20000`.
- 80% JaCoCo line coverage per module (BUNDLE rule in the parent POM).
- No Maven wrapper. Build env:
  `export JAVA_HOME=/home/alexm/.jdks/temurin-17; export PATH="$JAVA_HOME/bin:/home/alexm/.m2/wrapper/dists/apache-maven-3.9.10/e5402a93/bin:$PATH"`
- Run Maven with `-f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml` (no `cd`).
- Commit trailers:
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R
  ```
- Commit with explicit paths (`git add <paths>`); leave `.claude/` and `.github/workflows/lab-scan.yml` untracked.

## Review Focus

1. **A required argument is missing** (e.g. `actuator_loggers_loggerLevels` with no `name`) → an MCP error result naming the argument, not a stack trace. Pinned in Task 3.
2. **Endpoint ids with a dash** (`health-manual`) → valid, stable tool names (`actuator_health-manual`). Pinned in Task 3.
3. **A custom `management.endpoints.web.base-path`** (e.g. `/manage`) → MCP served at `/manage/mcp`. Pinned in Task 4.
4. **The app already runs Spring AI's own MCP server** → it keeps its own tools on the main port; the actuator server lists only actuator tools. Pinned in Task 4.
5. **`include=*`** → `shutdown` (access `none` by default), `heapdump` and `logfile` never become tools. Pinned in Task 2.

---

## File map

```
pom.xml                                                     (modify: module + BOM imports)
spring-boot-actuator-mcp-starter/
  pom.xml
  src/main/java/org/alexmond/actuator/mcp/
    McpEndpointProperties.java        config properties
    McpOperation.java                 one discovered operation
    McpEndpoint.java                  one discovered endpoint
    McpEndpointDiscoverer.java        discovery + filters
    ResponseLimiter.java              size cap
    ToolDescriptions.java             hand-written descriptions
    ActuatorToolFactory.java          operation -> MCP tool
    ActuatorMcpServer.java            holder: dedicated server + transport
    ActuatorMcpAutoConfiguration.java main-context wiring
    ActuatorMcpManagementContextConfiguration.java  route on management port
  src/main/resources/META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
    org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports
  src/test/java/org/alexmond/actuator/mcp/
    TestEndpoints.java                test @Endpoint beans + discovery helper
    McpEndpointDiscovererTest.java
    ResponseLimiterTest.java
    ToolDescriptionsTest.java
    ActuatorToolFactoryTest.java
    ActuatorMcpAutoConfigurationTest.java
    it/McpTestApplication.java        test app (separate package: no scan of main package)
    it/McpTestClient.java             MCP client helper
    it/SeparateManagementPortTest.java
    it/SharedPortTest.java
    it/CustomBasePathTest.java
    it/SpringAiCoexistenceTest.java
spring-boot-test-app/pom.xml, src/main/resources/application.yaml   (modify)
docs/modules/ROOT/pages/actuator-mcp.adoc                          (create)
docs/modules/ROOT/nav.adoc, pages/index.adoc, README.adoc, CHANGELOG.adoc, docs/.../CHANGELOG.adoc (modify)
```

---

### Task 1: Module scaffold, properties, inert auto-configuration

**Files:**
- Modify: `pom.xml` (root)
- Create: `spring-boot-actuator-mcp-starter/pom.xml`
- Create: `spring-boot-actuator-mcp-starter/src/main/java/org/alexmond/actuator/mcp/McpEndpointProperties.java`
- Create: `spring-boot-actuator-mcp-starter/src/main/java/org/alexmond/actuator/mcp/ActuatorMcpAutoConfiguration.java` (skeleton; completed in Task 4)
- Create: `spring-boot-actuator-mcp-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `spring-boot-actuator-mcp-starter/src/test/java/org/alexmond/actuator/mcp/ActuatorMcpAutoConfigurationTest.java`

**Interfaces:**
- Produces: `McpEndpointProperties` with `isEnabled()`, `getExposure().getInclude()/getExclude()` (`Set<String>`), `getMaxResponseChars()` (`int`).
- Produces: `ActuatorMcpAutoConfiguration` registered as auto-configuration.

- [ ] **Step 1: Root POM — add module and BOM imports**

In `pom.xml`, add to `<properties>`:

```xml
        <spring-ai.version>2.0.1</spring-ai.version>
        <mcp-sdk.version>2.0.0</mcp-sdk.version>
```

Add `<module>spring-boot-actuator-mcp-starter</module>` after `spring-boot-manual-health-starter` in the always-on `<modules>` list.

Add (or extend, if present) a `<dependencyManagement>` block directly before `<build>`:

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.modelcontextprotocol.sdk</groupId>
                <artifactId>mcp-bom</artifactId>
                <version>${mcp-sdk.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

- [ ] **Step 2: Module POM**

Create `spring-boot-actuator-mcp-starter/pom.xml` (metadata copied from the sanitizer starter):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.alexmond</groupId>
        <artifactId>spring-boot-actuator-extensions-parent</artifactId>
        <version>4.1.1.2-SNAPSHOT</version>
    </parent>
    <artifactId>spring-boot-actuator-mcp-starter</artifactId>
    <version>4.1.1.2-SNAPSHOT</version>
    <name>spring-boot-actuator-mcp</name>
    <properties>
        <java.version>17</java.version>
    </properties>
    <url>https://www.alexmond.org/</url>
    <description>Spring Boot starter exposing Spring Boot Actuator endpoints as MCP (Model Context Protocol) tools
        on the management port, for AI assistants.
    </description>
    <licenses>
        <license>
            <name>The Apache Software License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>
    <developers>
        <developer>
            <name>Alex Mondshain</name>
            <email>alex.mondshain@gmail.com</email>
        </developer>
    </developers>
    <scm>
        <connection>scm:git:git@github.com:alexmond/spring-boot-actuator-extensions.git</connection>
        <developerConnection>scm:git:git@github.com:alexmond/spring-boot-actuator-extensions.git</developerConnection>
        <tag>HEAD</tag>
        <url>https://github.com/alexmond/spring-boot-actuator-extensions</url>
    </scm>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-actuator-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>mcp-spring-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.modelcontextprotocol.sdk</groupId>
            <artifactId>mcp-json-jackson3</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-source-plugin</artifactId>
                    </plugin>
                    <plugin>
                        <groupId>io.github.git-commit-id</groupId>
                        <artifactId>git-commit-id-maven-plugin</artifactId>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-gpg-plugin</artifactId>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-javadoc-plugin</artifactId>
                    </plugin>
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

- [ ] **Step 3: Verify dependency resolution**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter dependency:tree -Dincludes=io.modelcontextprotocol.sdk,org.springframework.ai --no-transfer-progress`
Expected: `mcp-spring-webmvc:2.0.1`, `mcp-core:2.0.0`, `mcp-json-jackson3:2.0.0` at compile scope; `spring-ai-starter-mcp-server-webmvc:2.0.1` at test scope. If a version is unresolved, pin it explicitly with `${mcp-sdk.version}` / `${spring-ai.version}` in the module POM.

- [ ] **Step 4: Write the failing test**

`ActuatorMcpAutoConfigurationTest.java`:

```java
package org.alexmond.actuator.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorMcpAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ActuatorMcpAutoConfiguration.class));

    @Test
    void bindsDefaults() {
        runner.run(context -> {
            McpEndpointProperties properties = context.getBean(McpEndpointProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getExposure().getInclude()).isEmpty();
            assertThat(properties.getExposure().getExclude()).isEmpty();
            assertThat(properties.getMaxResponseChars()).isEqualTo(20000);
        });
    }

    @Test
    void bindsCustomValues() {
        runner.withPropertyValues(
                        "management.endpoints.mcp.exposure.include=health,info",
                        "management.endpoints.mcp.exposure.exclude=env",
                        "management.endpoints.mcp.max-response-chars=500")
                .run(context -> {
                    McpEndpointProperties properties = context.getBean(McpEndpointProperties.class);
                    assertThat(properties.getExposure().getInclude()).containsExactly("health", "info");
                    assertThat(properties.getExposure().getExclude()).containsExactly("env");
                    assertThat(properties.getMaxResponseChars()).isEqualTo(500);
                });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("management.endpoints.mcp.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(McpEndpointProperties.class));
    }

    @Test
    void backsOffOutsideServletWebApps() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ActuatorMcpAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(McpEndpointProperties.class));
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Dtest=ActuatorMcpAutoConfigurationTest -Djacoco.skip=true --no-transfer-progress`
Expected: compilation FAIL — `McpEndpointProperties` / `ActuatorMcpAutoConfiguration` not found.

- [ ] **Step 6: Implement properties and skeleton auto-configuration**

`McpEndpointProperties.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for exposing actuator endpoints as MCP tools.
 */
@Getter
@Setter
@ConfigurationProperties("management.endpoints.mcp")
public class McpEndpointProperties {

    /**
     * Whether the actuator MCP server is enabled. Nothing is exposed until endpoints are included.
     */
    private boolean enabled = true;

    private final Exposure exposure = new Exposure();

    /**
     * Maximum number of characters returned by one tool call; longer results are truncated.
     */
    private int maxResponseChars = 20000;

    @Getter
    @Setter
    public static class Exposure {

        /**
         * Endpoint IDs to expose as MCP tools, or '*' for all. Empty by default.
         */
        private Set<String> include = new LinkedHashSet<>();

        /**
         * Endpoint IDs that must not be exposed as MCP tools.
         */
        private Set<String> exclude = new LinkedHashSet<>();
    }
}
```

`ActuatorMcpAutoConfiguration.java` (skeleton — beans are added in Task 4):

```java
package org.alexmond.actuator.mcp;

import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Exposes actuator endpoints as MCP tools on a dedicated MCP server.
 */
@AutoConfiguration(after = EndpointAutoConfiguration.class)
@ConditionalOnClass({Endpoint.class, McpSyncServer.class, WebMvcStreamableServerTransportProvider.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(name = "management.endpoints.mcp.enabled", matchIfMissing = true)
@EnableConfigurationProperties(McpEndpointProperties.class)
public class ActuatorMcpAutoConfiguration {
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
org.alexmond.actuator.mcp.ActuatorMcpAutoConfiguration
```

- [ ] **Step 7: Run test to verify it passes**

Run the Step 5 command. Expected: `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 8: Commit**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R add pom.xml spring-boot-actuator-mcp-starter
git -C $R commit -m "feat(mcp): scaffold actuator MCP starter module and properties" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R"
```

---

### Task 2: Endpoint discovery with exposure and access filtering

**Files:**
- Create: `src/main/java/org/alexmond/actuator/mcp/McpOperation.java`
- Create: `src/main/java/org/alexmond/actuator/mcp/McpEndpoint.java`
- Create: `src/main/java/org/alexmond/actuator/mcp/McpEndpointDiscoverer.java`
- Create: `src/test/java/org/alexmond/actuator/mcp/TestEndpoints.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/McpEndpointDiscovererTest.java`

(All paths relative to `spring-boot-actuator-mcp-starter/`.)

**Interfaces:**
- Consumes: `McpEndpointProperties.Exposure` (Task 1).
- Produces:
  - `McpOperation extends AbstractDiscoveredOperation`: `EndpointId getEndpointId()`, `String getName()` (Java method name), `List<OperationParameter> getParameters()`, plus inherited `OperationType getType()`, `Object invoke(InvocationContext)`.
  - `McpEndpoint extends AbstractDiscoveredEndpoint<McpOperation>`: inherited `EndpointId getEndpointId()`, `Collection<McpOperation> getOperations()`.
  - `McpEndpointDiscoverer(ApplicationContext, ParameterValueMapper, Collection<OperationInvokerAdvisor>, Collection<EndpointFilter<McpEndpoint>>, Collection<OperationFilter<McpOperation>>)` with inherited `Collection<McpEndpoint> getEndpoints()`; constant `Set<String> UNSUPPORTED_ENDPOINTS`.
  - Test helper `TestEndpoints.discover(ApplicationContext context, Collection<String> include, Collection<String> exclude)` → `McpEndpointDiscoverer`.

- [ ] **Step 1: Write the test endpoints and helper**

`TestEndpoints.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.endpoint.PropertiesEndpointAccessResolver;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test endpoints covering read/write/delete operations, selectors, dashes and failures.
 */
final class TestEndpoints {

    private TestEndpoints() {
    }

    static McpEndpointDiscoverer discover(ApplicationContext context, Collection<String> include,
                                          Collection<String> exclude) {
        return new McpEndpointDiscoverer(context, new ConversionServiceParameterValueMapper(), List.of(),
                List.of(new IncludeExcludeEndpointFilter<>(McpEndpoint.class, include, exclude)),
                List.of(OperationFilter.byAccess(new PropertiesEndpointAccessResolver(context.getEnvironment()))));
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean
        AlphaEndpoint alphaEndpoint() {
            return new AlphaEndpoint();
        }

        @Bean
        DashEndpoint dashEndpoint() {
            return new DashEndpoint();
        }

        @Bean
        BoomEndpoint boomEndpoint() {
            return new BoomEndpoint();
        }

        @Bean
        FakeHeapdumpEndpoint fakeHeapdumpEndpoint() {
            return new FakeHeapdumpEndpoint();
        }

        @Bean
        FakeShutdownEndpoint fakeShutdownEndpoint() {
            return new FakeShutdownEndpoint();
        }
    }

    @Endpoint(id = "alpha")
    static class AlphaEndpoint {

        private String level = "INFO";

        @ReadOperation
        public Map<String, String> read() {
            return Map.of("level", level);
        }

        @ReadOperation
        public String readOne(@Selector String name) {
            return "value-of-" + name;
        }

        @WriteOperation
        public void write(String level) {
            this.level = level;
        }

        @DeleteOperation
        public void reset() {
            this.level = "INFO";
        }
    }

    @Endpoint(id = "dash-id")
    static class DashEndpoint {

        @ReadOperation
        public Map<String, Object> status(Integer limit, Boolean verbose) {
            return Map.of("limit", limit == null ? 0 : limit, "verbose", verbose != null && verbose);
        }
    }

    @Endpoint(id = "boom")
    static class BoomEndpoint {

        @ReadOperation
        public String fail() {
            throw new IllegalStateException("boom went the endpoint");
        }

        @ReadOperation
        public Object empty() {
            return new Object();
        }

        @ReadOperation
        public String nothing(@Selector String key) {
            return null;
        }

        @ReadOperation
        public String big(@Selector int size, @Selector String fill) {
            return fill.repeat(size);
        }
    }

    @Endpoint(id = "heapdump")
    static class FakeHeapdumpEndpoint {

        @ReadOperation
        public String dump() {
            return "binary";
        }
    }

    @Endpoint(id = "shutdown", defaultAccess = Access.NONE)
    static class FakeShutdownEndpoint {

        @WriteOperation
        public String shutdown() {
            return "bye";
        }
    }
}
```

Note: `@Selector` on two parameters of `big` is allowed by Boot (path segments). The `empty()` result has no properties, so Jackson fails to serialise it — used for the serialisation-error case.

- [ ] **Step 2: Write the failing test**

`McpEndpointDiscovererTest.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class McpEndpointDiscovererTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestEndpoints.Config.class);

    private static Map<String, List<String>> operationsById(Collection<McpEndpoint> endpoints) {
        return endpoints.stream().collect(Collectors.toMap(e -> e.getEndpointId().toLowerCaseString(),
                e -> e.getOperations().stream().map(McpOperation::getName).sorted().toList()));
    }

    @Test
    void emptyIncludeExposesNothing() {
        runner.run(context -> assertThat(TestEndpoints.discover(context, List.of(), List.of()).getEndpoints())
                .isEmpty());
    }

    @Test
    void includeSelectsEndpoints() {
        runner.run(context -> {
            var endpoints = TestEndpoints.discover(context, List.of("alpha"), List.of()).getEndpoints();
            assertThat(operationsById(endpoints))
                    .containsOnlyKeys("alpha")
                    .containsEntry("alpha", List.of("read", "readOne", "reset", "write"));
        });
    }

    @Test
    void excludeWinsOverWildcardInclude() {
        runner.run(context -> {
            var endpoints = TestEndpoints.discover(context, List.of("*"), List.of("boom")).getEndpoints();
            assertThat(operationsById(endpoints)).containsOnlyKeys("alpha", "dash-id");
        });
    }

    @Test
    void wildcardNeverExposesHeapdumpLogfileOrShutdown() {
        runner.run(context -> {
            var endpoints = TestEndpoints.discover(context, List.of("*"), List.of()).getEndpoints();
            assertThat(operationsById(endpoints)).doesNotContainKeys("heapdump", "logfile", "shutdown");
        });
    }

    @Test
    void readOnlyAccessDropsWriteAndDeleteOperations() {
        runner.withPropertyValues("management.endpoint.alpha.access=read-only").run(context -> {
            var alpha = TestEndpoints.discover(context, List.of("alpha"), List.of()).getEndpoints()
                    .iterator().next();
            assertThat(alpha.getOperations()).extracting(McpOperation::getType)
                    .containsOnly(OperationType.READ);
        });
    }

    @Test
    void maxPermittedReadOnlyAppliesToAllEndpoints() {
        runner.withPropertyValues("management.endpoints.access.max-permitted=read-only").run(context -> {
            var endpoints = TestEndpoints.discover(context, List.of("*"), List.of()).getEndpoints();
            assertThat(endpoints).flatExtracting(McpEndpoint::getOperations).extracting(McpOperation::getType)
                    .containsOnly(OperationType.READ);
        });
    }

    @Test
    void accessNoneRemovesTheEndpoint() {
        runner.withPropertyValues("management.endpoint.alpha.access=none").run(context ->
                assertThat(TestEndpoints.discover(context, List.of("alpha"), List.of()).getEndpoints()).isEmpty());
    }

    @Test
    void shutdownAppearsOnlyWhenActuatorAccessAllowsIt() {
        runner.withPropertyValues("management.endpoint.shutdown.access=unrestricted").run(context -> {
            var endpoints = TestEndpoints.discover(context, List.of("shutdown"), List.of()).getEndpoints();
            assertThat(operationsById(endpoints)).containsEntry("shutdown", List.of("shutdown"));
        });
    }

    @Test
    void operationExposesEndpointIdAndParameters() {
        runner.run(context -> {
            var alpha = TestEndpoints.discover(context, List.of("alpha"), List.of()).getEndpoints()
                    .iterator().next();
            McpOperation readOne = alpha.getOperations().stream()
                    .filter(op -> op.getName().equals("readOne")).findFirst().orElseThrow();
            assertThat(readOne.getEndpointId().toLowerCaseString()).isEqualTo("alpha");
            assertThat(readOne.getParameters()).singleElement().satisfies(p -> {
                assertThat(p.getName()).isEqualTo("name");
                assertThat(p.isMandatory()).isTrue();
            });
        });
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Dtest=McpEndpointDiscovererTest -Djacoco.skip=true --no-transfer-progress`
Expected: compilation FAIL — `McpEndpointDiscoverer`, `McpEndpoint`, `McpOperation` not found.

- [ ] **Step 4: Implement**

`McpOperation.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.List;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredOperation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;

/**
 * An actuator endpoint operation discovered for MCP exposure.
 */
public class McpOperation extends AbstractDiscoveredOperation {

    private final EndpointId endpointId;

    public McpOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
        super(operationMethod, invoker);
        this.endpointId = endpointId;
    }

    public EndpointId getEndpointId() {
        return endpointId;
    }

    /**
     * The Java method name of the operation, for example {@code loggerLevels}.
     */
    public String getName() {
        return getOperationMethod().getMethod().getName();
    }

    public List<OperationParameter> getParameters() {
        return getOperationMethod().getParameters().stream().toList();
    }
}
```

`McpEndpoint.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;

/**
 * An actuator endpoint discovered for MCP exposure.
 */
public class McpEndpoint extends AbstractDiscoveredEndpoint<McpOperation> {

    McpEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, Access defaultAccess,
                Collection<McpOperation> operations) {
        super(discoverer, endpointBean, id, defaultAccess, operations);
    }
}
```

`McpEndpointDiscoverer.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.Collection;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.context.ApplicationContext;

/**
 * Discovers actuator endpoints for MCP, the way Boot discovers them for web and JMX.
 */
public class McpEndpointDiscoverer extends EndpointDiscoverer<McpEndpoint, McpOperation> {

    /**
     * Endpoints whose output cannot be sent as text.
     */
    public static final Set<String> UNSUPPORTED_ENDPOINTS = Set.of("heapdump", "logfile");

    public McpEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
                                 Collection<OperationInvokerAdvisor> invokerAdvisors,
                                 Collection<EndpointFilter<McpEndpoint>> endpointFilters,
                                 Collection<OperationFilter<McpOperation>> operationFilters) {
        super(applicationContext, parameterValueMapper, invokerAdvisors, endpointFilters, operationFilters);
    }

    @Override
    protected boolean isInvocable(McpEndpoint endpoint) {
        return !UNSUPPORTED_ENDPOINTS.contains(endpoint.getEndpointId().toLowerCaseString())
                && super.isInvocable(endpoint);
    }

    @Override
    protected McpEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
                                         Collection<McpOperation> operations) {
        return new McpEndpoint(this, endpointBean, id, defaultAccess, operations);
    }

    @Override
    protected McpOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
                                           OperationInvoker invoker) {
        return new McpOperation(endpointId, operationMethod, invoker);
    }

    @Override
    protected OperationKey createOperationKey(McpOperation operation) {
        return new OperationKey(operation.getName(), () -> "MCP operation " + operation.getName());
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run the Step 3 command. Expected: `Tests run: 9, Failures: 0, Errors: 0`.

If `wildcardNeverExposesHeapdumpLogfileOrShutdown` or `isInvocable` behaves differently (Boot only calls `isInvocable` in some code paths), filter in `getEndpoints()` callers instead: add a `public Collection<McpEndpoint> getExposableEndpoints()` that filters `getEndpoints()` by `UNSUPPORTED_ENDPOINTS`, use it everywhere downstream, and update the tests to call it.

- [ ] **Step 6: Commit**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R add spring-boot-actuator-mcp-starter/src
git -C $R commit -m "feat(mcp): discover actuator endpoints with exposure and access filters" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R"
```

---

### Task 3: Operation → MCP tool (naming, schema, descriptions, invocation, limits)

**Files:**
- Create: `src/main/java/org/alexmond/actuator/mcp/ResponseLimiter.java`
- Create: `src/main/java/org/alexmond/actuator/mcp/ToolDescriptions.java`
- Create: `src/main/java/org/alexmond/actuator/mcp/ActuatorToolFactory.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/ResponseLimiterTest.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/ToolDescriptionsTest.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/ActuatorToolFactoryTest.java`

**Interfaces:**
- Consumes: `McpEndpoint`, `McpOperation`, `TestEndpoints.discover(...)` (Task 2).
- Produces:
  - `ResponseLimiter(int maxChars)`; `String limit(String text)`.
  - `ToolDescriptions.find(String endpointId, String operationName)` → `Optional<String>`.
  - `ActuatorToolFactory(JsonMapper jsonMapper, ResponseLimiter limiter)`;
    `List<McpServerFeatures.SyncToolSpecification> createTools(Collection<McpEndpoint> endpoints)` (sorted by tool name);
    `static String toolName(McpEndpoint endpoint, McpOperation operation)`;
    `static McpSchema.JsonSchema inputSchema(McpOperation operation)`;
    `McpSchema.CallToolResult call(McpOperation operation, Map<String, Object> arguments)`.

- [ ] **Step 1: Write failing tests for the limiter and descriptions**

`ResponseLimiterTest.java`:

```java
package org.alexmond.actuator.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ResponseLimiterTest {

    @Test
    void keepsShortText() {
        assertThat(new ResponseLimiter(10).limit("short")).isEqualTo("short");
    }

    @Test
    void keepsTextAtExactLimit() {
        assertThat(new ResponseLimiter(5).limit("12345")).isEqualTo("12345");
    }

    @Test
    void truncatesLongTextWithMarker() {
        String result = new ResponseLimiter(5).limit("1234567890");
        assertThat(result).startsWith("12345\n");
        assertThat(result).contains("truncated: 5 of 10 characters dropped");
        assertThat(result).contains("management.endpoints.mcp.max-response-chars");
    }

    @Test
    void nullBecomesEmpty() {
        assertThat(new ResponseLimiter(5).limit(null)).isEmpty();
    }

    @Test
    void rejectsNonPositiveLimit() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ResponseLimiter(0));
    }
}
```

`ToolDescriptionsTest.java`:

```java
package org.alexmond.actuator.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolDescriptionsTest {

    @Test
    void knowsCommonOperations() {
        assertThat(ToolDescriptions.find("health", "health")).isPresent();
        assertThat(ToolDescriptions.find("metrics", "metric")).get().asString().contains("requiredMetricName");
        assertThat(ToolDescriptions.find("loggers", "configureLogLevel")).isPresent();
    }

    @Test
    void unknownOperationIsEmpty() {
        assertThat(ToolDescriptions.find("alpha", "read")).isEmpty();
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Dtest='ResponseLimiterTest,ToolDescriptionsTest' -Djacoco.skip=true --no-transfer-progress`
Expected: compilation FAIL.

- [ ] **Step 3: Implement `ResponseLimiter` and `ToolDescriptions`**

`ResponseLimiter.java`:

```java
package org.alexmond.actuator.mcp;

/**
 * Caps the size of a tool result so one call cannot flood an assistant's context.
 */
public final class ResponseLimiter {

    private final int maxChars;

    public ResponseLimiter(int maxChars) {
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars must be positive, was " + maxChars);
        }
        this.maxChars = maxChars;
    }

    public String limit(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        int dropped = text.length() - maxChars;
        return text.substring(0, maxChars) + "\n... [truncated: " + dropped + " of " + text.length()
                + " characters dropped. Call the tool with narrower arguments, or raise"
                + " management.endpoints.mcp.max-response-chars]";
    }
}
```

`ToolDescriptions.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

/**
 * Hand-written descriptions for common actuator operations, keyed by "endpointId.operationName".
 */
final class ToolDescriptions {

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            entry("health.health", "Overall application health: status (UP, DOWN, OUT_OF_SERVICE, UNKNOWN)"
                    + " and, if enabled, each health component's status and details."),
            entry("health.healthForPath", "Health of one component or group. 'path' is the component path,"
                    + " for example [\"db\"] or [\"diskSpace\"]."),
            entry("info.info", "Application info: build, git, java, os and custom info contributors."),
            entry("metrics.listNames", "Names of all available metrics. Use this first, then call the metric"
                    + " tool for one name instead of reading everything."),
            entry("metrics.metric", "Measurements of one metric. 'requiredMetricName' is a name from the"
                    + " metric list, e.g. jvm.memory.used. Optional 'tag' entries filter as KEY:VALUE,"
                    + " e.g. [\"area:heap\"]."),
            entry("loggers.loggers", "All loggers and their levels. Large; prefer loggerLevels for one logger."),
            entry("loggers.loggerLevels", "Configured and effective level of one logger. 'name' is the logger"
                    + " name, e.g. com.example.service or ROOT."),
            entry("loggers.configureLogLevel", "Change the level of one logger at runtime. 'name' is the logger;"
                    + " 'configuredLevel' is TRACE, DEBUG, INFO, WARN, ERROR, FATAL or OFF, or empty to reset."),
            entry("env.environment", "Environment property sources. Values may be masked. Use 'pattern'"
                    + " (a regular expression on property names) to narrow the result."),
            entry("env.environmentEntry", "One environment property across all property sources."
                    + " 'toMatch' is the exact property name."),
            entry("configprops.configurationProperties", "All @ConfigurationProperties beans. Large; prefer"
                    + " the prefix variant."),
            entry("configprops.configurationPropertiesWithPrefix", "@ConfigurationProperties beans whose"
                    + " prefix starts with 'prefix', e.g. spring.datasource."),
            entry("beans.beans", "All Spring beans in the application context. Very large."),
            entry("mappings.mappings", "All request mappings (URL to handler). Large."));

    private ToolDescriptions() {
    }

    static Optional<String> find(String endpointId, String operationName) {
        return Optional.ofNullable(DESCRIPTIONS.get(endpointId + "." + operationName));
    }
}
```

- [ ] **Step 4: Run limiter/description tests to verify they pass**

Run the Step 2 command. Expected: `Tests run: 7, Failures: 0, Errors: 0`.

- [ ] **Step 5: Write the failing factory test**

`ActuatorToolFactoryTest.java`:

```java
package org.alexmond.actuator.mcp;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorToolFactoryTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestEndpoints.Config.class);

    private final ActuatorToolFactory factory =
            new ActuatorToolFactory(JsonMapper.builder().build(), new ResponseLimiter(50));

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }

    private static SyncToolSpecification tool(List<SyncToolSpecification> tools, String name) {
        return tools.stream().filter(t -> t.tool().name().equals(name)).findFirst().orElseThrow();
    }

    private static CallToolResult call(SyncToolSpecification tool, Map<String, Object> args) {
        return tool.callHandler().apply(null, new McpSchema.CallToolRequest(tool.tool().name(), args));
    }

    @Test
    void namesToolsAndSortsThem() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha", "dash-id"), List.of())
                    .getEndpoints());
            assertThat(tools).extracting(t -> t.tool().name()).containsExactly(
                    "actuator_alpha_read", "actuator_alpha_readOne", "actuator_alpha_reset",
                    "actuator_alpha_write", "actuator_dash-id");
        });
    }

    @Test
    void buildsInputSchemaFromParameters() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha", "dash-id"), List.of())
                    .getEndpoints());
            McpSchema.JsonSchema readOne = tool(tools, "actuator_alpha_readOne").tool().inputSchema();
            assertThat(readOne.type()).isEqualTo("object");
            assertThat(readOne.properties()).containsOnlyKeys("name");
            assertThat(readOne.required()).containsExactly("name");

            McpSchema.JsonSchema dash = tool(tools, "actuator_dash-id").tool().inputSchema();
            assertThat(dash.properties()).containsEntry("limit", Map.of("type", "integer"))
                    .containsEntry("verbose", Map.of("type", "boolean"));
            assertThat(dash.required()).isEmpty();
        });
    }

    @Test
    void generatesDescriptionWhenNoneIsKnown() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha"), List.of())
                    .getEndpoints());
            assertThat(tool(tools, "actuator_alpha_write").tool().description())
                    .contains("'alpha'").contains("write operation").contains("level");
        });
    }

    @Test
    void invokesReadOperationAsJson() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_alpha_read"), Map.of());
            assertThat(result.isError()).isFalse();
            assertThat(text(result)).isEqualTo("{\"level\":\"INFO\"}");
        });
    }

    @Test
    void writeOperationChangesState() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha"), List.of())
                    .getEndpoints());
            assertThat(call(tool(tools, "actuator_alpha_write"), Map.of("level", "DEBUG")).isError()).isFalse();
            assertThat(text(call(tool(tools, "actuator_alpha_read"), Map.of()))).contains("DEBUG");
        });
    }

    @Test
    void convertsArgumentTypes() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("dash-id"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_dash-id"), Map.of("limit", 7, "verbose", true));
            assertThat(text(result)).contains("\"limit\":7").contains("\"verbose\":true");
        });
    }

    @Test
    void missingRequiredArgumentIsAnErrorNamingIt() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_alpha_readOne"), Map.of());
            assertThat(result.isError()).isTrue();
            assertThat(text(result)).isEqualTo("Missing required argument(s): name");
        });
    }

    @Test
    void nullArgumentsMapIsTreatedAsEmpty() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha"), List.of())
                    .getEndpoints());
            assertThat(call(tool(tools, "actuator_alpha_read"), null).isError()).isFalse();
        });
    }

    @Test
    void operationExceptionIsAnErrorWithoutStackTrace() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("boom"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_boom_fail"), Map.of());
            assertThat(result.isError()).isTrue();
            assertThat(text(result)).isEqualTo("boom went the endpoint").doesNotContain("at org.");
        });
    }

    @Test
    void unserialisableResultIsAnError() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("boom"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_boom_empty"), Map.of());
            assertThat(result.isError()).isTrue();
            assertThat(text(result)).startsWith("Result could not be serialised");
        });
    }

    @Test
    void nullResultIsEmptyText() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("boom"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_boom_nothing"), Map.of("key", "k"));
            assertThat(result.isError()).isFalse();
            assertThat(text(result)).isEmpty();
        });
    }

    @Test
    void longResultIsTruncated() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("boom"), List.of())
                    .getEndpoints());
            CallToolResult result = call(tool(tools, "actuator_boom_big"), Map.of("size", 200, "fill", "x"));
            assertThat(text(result)).startsWith("x".repeat(50) + "\n").contains("truncated");
        });
    }
}
```

- [ ] **Step 6: Run to verify failure**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Dtest=ActuatorToolFactoryTest -Djacoco.skip=true --no-transfer-progress`
Expected: compilation FAIL — `ActuatorToolFactory` not found.

- [ ] **Step 7: Implement `ActuatorToolFactory`**

```java
package org.alexmond.actuator.mcp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns actuator operations into MCP tool specifications.
 */
@Slf4j
public class ActuatorToolFactory {

    private final JsonMapper jsonMapper;

    private final ResponseLimiter limiter;

    public ActuatorToolFactory(JsonMapper jsonMapper, ResponseLimiter limiter) {
        this.jsonMapper = jsonMapper;
        this.limiter = limiter;
    }

    public List<SyncToolSpecification> createTools(Collection<McpEndpoint> endpoints) {
        List<SyncToolSpecification> tools = new ArrayList<>();
        for (McpEndpoint endpoint : endpoints) {
            for (McpOperation operation : endpoint.getOperations()) {
                tools.add(createTool(endpoint, operation));
            }
        }
        tools.sort(Comparator.comparing(tool -> tool.tool().name()));
        return tools;
    }

    SyncToolSpecification createTool(McpEndpoint endpoint, McpOperation operation) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(toolName(endpoint, operation))
                .description(description(operation))
                .inputSchema(inputSchema(operation))
                .build();
        return SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> call(operation, request.arguments()))
                .build();
    }

    /**
     * {@code actuator_<id>} for an endpoint with one operation, else {@code actuator_<id>_<operation>}.
     */
    static String toolName(McpEndpoint endpoint, McpOperation operation) {
        String base = "actuator_" + endpoint.getEndpointId().toLowerCaseString();
        return endpoint.getOperations().size() == 1 ? base : base + "_" + operation.getName();
    }

    static McpSchema.JsonSchema inputSchema(McpOperation operation) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (OperationParameter parameter : operation.getParameters()) {
            properties.put(parameter.getName(), typeSchema(parameter.getType()));
            if (parameter.isMandatory()) {
                required.add(parameter.getName());
            }
        }
        return new McpSchema.JsonSchema("object", properties, required, false, null, null);
    }

    private static Map<String, Object> typeSchema(Class<?> type) {
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return Map.of("type", "array", "items", Map.of("type", "string"));
        }
        if (type.isEnum()) {
            List<String> values = new ArrayList<>();
            for (Object constant : type.getEnumConstants()) {
                values.add(((Enum<?>) constant).name());
            }
            return Map.of("type", "string", "enum", values);
        }
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class
                || type == Short.class || type == short.class) {
            return Map.of("type", "integer");
        }
        if (type == Double.class || type == double.class || type == Float.class || type == float.class
                || type == BigDecimal.class) {
            return Map.of("type", "number");
        }
        if (type == Boolean.class || type == boolean.class) {
            return Map.of("type", "boolean");
        }
        return Map.of("type", "string");
    }

    private static String description(McpOperation operation) {
        String endpointId = operation.getEndpointId().toLowerCaseString();
        return ToolDescriptions.find(endpointId, operation.getName()).orElseGet(() -> {
            String parameters = operation.getParameters().stream()
                    .map(p -> p.getName() + (p.isMandatory() ? "" : " (optional)"))
                    .collect(Collectors.joining(", "));
            return "Spring Boot Actuator '" + endpointId + "' endpoint, "
                    + operation.getType().name().toLowerCase() + " operation '" + operation.getName() + "'."
                    + (parameters.isEmpty() ? "" : " Arguments: " + parameters + ".");
        });
    }

    CallToolResult call(McpOperation operation, Map<String, Object> arguments) {
        Map<String, Object> args = (arguments != null) ? arguments : Map.of();
        List<String> missing = operation.getParameters().stream()
                .filter(OperationParameter::isMandatory)
                .map(OperationParameter::getName)
                .filter(name -> args.get(name) == null)
                .toList();
        if (!missing.isEmpty()) {
            return error("Missing required argument(s): " + String.join(", ", missing));
        }
        try {
            Object result = operation.invoke(new InvocationContext(SecurityContext.NONE, args));
            return CallToolResult.builder().addTextContent(limiter.limit(toText(result))).isError(false).build();
        }
        catch (JacksonException ex) {
            log.warn("Could not serialise result of actuator operation {}.{}",
                    operation.getEndpointId(), operation.getName(), ex);
            return error("Result could not be serialised: " + ex.getOriginalMessage());
        }
        catch (RuntimeException ex) {
            return error(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }

    private String toText(Object result) {
        if (result == null) {
            return "";
        }
        if (result instanceof CharSequence text) {
            return text.toString();
        }
        return jsonMapper.writeValueAsString(result);
    }

    private static CallToolResult error(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }
}
```

- [ ] **Step 8: Run all module tests**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Djacoco.skip=true --no-transfer-progress`
Expected: all tests pass (Tasks 1–3).

Known risk: `dash-id` tool with two `null` optional args — Boot's `ReflectiveOperationInvoker` passes `null` for absent optional parameters; if it throws `MissingParametersException` instead, mark `limit`/`verbose` with `@org.springframework.lang.Nullable` in `TestEndpoints` (Boot treats `@Nullable` parameters as optional).

- [ ] **Step 9: Commit**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R add spring-boot-actuator-mcp-starter/src
git -C $R commit -m "feat(mcp): build MCP tools from actuator operations with size limits" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R"
```

---

### Task 4: Dedicated server, auto-configuration and the management-port route

**Files:**
- Create: `src/main/java/org/alexmond/actuator/mcp/ActuatorMcpServer.java`
- Modify: `src/main/java/org/alexmond/actuator/mcp/ActuatorMcpAutoConfiguration.java`
- Create: `src/main/java/org/alexmond/actuator/mcp/ActuatorMcpManagementContextConfiguration.java`
- Create: `src/main/resources/META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports`
- Modify: `src/test/java/org/alexmond/actuator/mcp/ActuatorMcpAutoConfigurationTest.java`
- Create: `src/test/java/org/alexmond/actuator/mcp/it/McpTestApplication.java`
- Create: `src/test/java/org/alexmond/actuator/mcp/it/McpTestClient.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/it/SeparateManagementPortTest.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/it/SharedPortTest.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/it/CustomBasePathTest.java`
- Test: `src/test/java/org/alexmond/actuator/mcp/it/SpringAiCoexistenceTest.java`

**Interfaces:**
- Consumes: `McpEndpointProperties` (Task 1), `McpEndpointDiscoverer` (Task 2), `ActuatorToolFactory`, `ResponseLimiter` (Task 3).
- Produces:
  - `ActuatorMcpServer(String endpointPath, List<SyncToolSpecification> tools)`; `String getEndpointPath()`; `List<String> getToolNames()`; `RouterFunction<ServerResponse> getRouterFunction()`; `destroy()`.
  - `static String ActuatorMcpAutoConfiguration.mcpPath(String basePath)`.
  - Beans: `McpEndpointDiscoverer mcpEndpointDiscoverer`, `ActuatorMcpServer actuatorMcpServer` (main context); `RouterFunction<ServerResponse> actuatorMcpRouterFunction` (management context).

- [ ] **Step 1: Write the test app and MCP client helper**

`it/McpTestApplication.java` (its own package so the scan never picks up the main package's `@ManagementContextConfiguration`):

```java
package org.alexmond.actuator.mcp.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpTestApplication.class, args);
    }
}
```

`it/McpTestClient.java`:

```java
package org.alexmond.actuator.mcp.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Small helper around the MCP SDK client for the full-context tests.
 */
final class McpTestClient implements AutoCloseable {

    private final McpSyncClient client;

    McpTestClient(int port, String path) {
        this.client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint(path).build()).build();
        this.client.initialize();
    }

    List<String> toolNames() {
        return client.listTools().tools().stream().map(McpSchema.Tool::name).toList();
    }

    McpSchema.CallToolResult call(String tool, Map<String, Object> arguments) {
        return client.callTool(new McpSchema.CallToolRequest(tool, arguments));
    }

    static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    /** Raw initialize POST, to check that a port does or does not serve MCP. */
    static int initializeStatus(int port, String path) throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":"
                + "\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"0\"}}}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Override
    public void close() {
        client.close();
    }
}
```

- [ ] **Step 2: Write the failing full-context tests**

`it/SeparateManagementPortTest.java`:

```java
package org.alexmond.actuator.mcp.it;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {McpTestApplication.class, SeparateManagementPortTest.MaskSecret.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.mcp.exposure.include=health,metrics,loggers,env",
                "management.endpoint.env.show-values=always",
                "app.secret=hunter2",
                "app.visible=hello"
        })
@DirtiesContext
class SeparateManagementPortTest {

    @LocalServerPort
    int serverPort;

    @LocalManagementPort
    int managementPort;

    @Test
    void servesMcpOnlyOnTheManagementPort() throws Exception {
        assertThat(managementPort).isNotEqualTo(serverPort);
        assertThat(McpTestClient.initializeStatus(managementPort, "/actuator/mcp")).isEqualTo(200);
        assertThat(McpTestClient.initializeStatus(serverPort, "/actuator/mcp")).isEqualTo(404);
    }

    @Test
    void listsAndCallsActuatorTools() {
        try (McpTestClient client = new McpTestClient(managementPort, "/actuator/mcp")) {
            assertThat(client.toolNames()).contains("actuator_health_health", "actuator_metrics_metric",
                    "actuator_loggers_loggerLevels", "actuator_loggers_configureLogLevel",
                    "actuator_env_environmentEntry");
            assertThat(McpTestClient.text(client.call("actuator_health_health", Map.of()))).contains("\"UP\"");
            assertThat(McpTestClient.text(client.call("actuator_metrics_metric",
                    Map.of("requiredMetricName", "jvm.memory.used")))).contains("jvm.memory.used");
            assertThat(McpTestClient.text(client.call("actuator_loggers_loggerLevels", Map.of("name", "ROOT"))))
                    .contains("effectiveLevel");
        }
    }

    @Test
    void writeToolChangesLogLevel() {
        try (McpTestClient client = new McpTestClient(managementPort, "/actuator/mcp")) {
            client.call("actuator_loggers_configureLogLevel", Map.of("name", "org.example.mcp",
                    "configuredLevel", "DEBUG"));
            assertThat(McpTestClient.text(client.call("actuator_loggers_loggerLevels",
                    Map.of("name", "org.example.mcp")))).contains("DEBUG");
        }
    }

    @Test
    void sanitizingFunctionsStillApply() {
        try (McpTestClient client = new McpTestClient(managementPort, "/actuator/mcp")) {
            assertThat(McpTestClient.text(client.call("actuator_env_environmentEntry",
                    Map.of("toMatch", "app.secret")))).contains("******").doesNotContain("hunter2");
            assertThat(McpTestClient.text(client.call("actuator_env_environmentEntry",
                    Map.of("toMatch", "app.visible")))).contains("hello");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MaskSecret {

        @Bean
        SanitizingFunction maskSecret() {
            return (SanitizableData data) -> data.getKey().equals("app.secret") ? data.withValue("******") : data;
        }
    }
}
```

`it/SharedPortTest.java`:

```java
package org.alexmond.actuator.mcp.it;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoints.mcp.exposure.include=health",
                "management.endpoint.health.access=read-only"
        })
@DirtiesContext
class SharedPortTest {

    @LocalServerPort
    int port;

    @Test
    void servesMcpUnderActuatorPathOnSharedPort() {
        try (McpTestClient client = new McpTestClient(port, "/actuator/mcp")) {
            assertThat(client.toolNames()).containsExactly("actuator_health_health", "actuator_health_healthForPath");
            assertThat(McpTestClient.text(client.call("actuator_health_health", Map.of()))).contains("\"UP\"");
        }
    }
}
```

`it/CustomBasePathTest.java`:

```java
package org.alexmond.actuator.mcp.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.base-path=/manage",
                "management.endpoints.mcp.exposure.include=health"
        })
@DirtiesContext
class CustomBasePathTest {

    @LocalManagementPort
    int managementPort;

    @Test
    void followsTheActuatorBasePath() throws Exception {
        assertThat(McpTestClient.initializeStatus(managementPort, "/manage/mcp")).isEqualTo(200);
        assertThat(McpTestClient.initializeStatus(managementPort, "/actuator/mcp")).isEqualTo(404);
    }
}
```

`it/SpringAiCoexistenceTest.java`:

```java
package org.alexmond.actuator.mcp.it;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring AI's own MCP server starter is on the test classpath: it must keep its own tools on the main
 * port, and the actuator server must not see them (nor they the actuator tools).
 */
@SpringBootTest(classes = {McpTestApplication.class, SpringAiCoexistenceTest.AppTool.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.mcp.exposure.include=health",
                "spring.ai.mcp.server.protocol=STREAMABLE"
        })
@DirtiesContext
class SpringAiCoexistenceTest {

    @LocalServerPort
    int serverPort;

    @LocalManagementPort
    int managementPort;

    @Test
    void appServerAndActuatorServerStaySeparate() {
        try (McpTestClient app = new McpTestClient(serverPort, "/mcp");
             McpTestClient actuator = new McpTestClient(managementPort, "/actuator/mcp")) {
            assertThat(app.toolNames()).containsExactly("app_echo");
            assertThat(actuator.toolNames()).allMatch(name -> name.startsWith("actuator_"))
                    .doesNotContain("app_echo");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AppTool {

        @Bean
        SyncToolSpecification appEcho() {
            return SyncToolSpecification.builder()
                    .tool(McpSchema.Tool.builder().name("app_echo").description("echo")
                            .inputSchema(new McpSchema.JsonSchema("object", java.util.Map.of(),
                                    java.util.List.of(), false, null, null)).build())
                    .callHandler((exchange, request) -> McpSchema.CallToolResult.builder()
                            .addTextContent("echo").build())
                    .build();
        }
    }
}
```

Add to `ActuatorMcpAutoConfigurationTest`:

```java
    @Test
    void computesMcpPath() {
        assertThat(ActuatorMcpAutoConfiguration.mcpPath("/actuator")).isEqualTo("/actuator/mcp");
        assertThat(ActuatorMcpAutoConfiguration.mcpPath("/manage/")).isEqualTo("/manage/mcp");
        assertThat(ActuatorMcpAutoConfiguration.mcpPath("/")).isEqualTo("/mcp");
        assertThat(ActuatorMcpAutoConfiguration.mcpPath("")).isEqualTo("/mcp");
    }

    @Test
    void createsServerWithNoToolsByDefault() {
        runner.withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration.class))
                .run(context -> assertThat(context.getBean(ActuatorMcpServer.class).getToolNames()).isEmpty());
    }
```

- [ ] **Step 3: Run to verify failure**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Djacoco.skip=true --no-transfer-progress`
Expected: compilation FAIL — `ActuatorMcpServer`, `mcpPath` not found.

- [ ] **Step 4: Implement `ActuatorMcpServer`**

```java
package org.alexmond.actuator.mcp;

import java.util.List;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Holds the dedicated actuator MCP server and its transport.
 * <p>
 * The server, transport and tools are deliberately not Spring beans: Spring AI's own MCP server
 * auto-configuration backs off for any {@code McpSyncServer} bean and collects every tool bean.
 */
public class ActuatorMcpServer implements DisposableBean {

    static final String SERVER_NAME = "spring-boot-actuator";

    private final String endpointPath;

    private final WebMvcStreamableServerTransportProvider transport;

    private final McpSyncServer server;

    public ActuatorMcpServer(String endpointPath, List<SyncToolSpecification> tools) {
        this.endpointPath = endpointPath;
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        this.transport = WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .mcpEndpoint(endpointPath)
                .build();
        String version = ActuatorMcpServer.class.getPackage().getImplementationVersion();
        this.server = McpServer.sync(transport)
                .serverInfo(SERVER_NAME, version != null ? version : "dev")
                .jsonMapper(jsonMapper)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .tools(tools)
                .build();
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public List<String> getToolNames() {
        return server.listTools().stream().map(McpSchema.Tool::name).toList();
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return transport.getRouterFunction();
    }

    @Override
    public void destroy() {
        server.closeGracefully();
    }
}
```

- [ ] **Step 5: Complete `ActuatorMcpAutoConfiguration`**

Replace the class body (keep the class annotations from Task 1):

```java
package org.alexmond.actuator.mcp;

import java.util.List;

import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.PropertiesEndpointAccessResolver;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exposes actuator endpoints as MCP tools on a dedicated MCP server.
 */
@AutoConfiguration(after = EndpointAutoConfiguration.class)
@ConditionalOnClass({Endpoint.class, McpSyncServer.class, WebMvcStreamableServerTransportProvider.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(name = "management.endpoints.mcp.enabled", matchIfMissing = true)
@EnableConfigurationProperties(McpEndpointProperties.class)
public class ActuatorMcpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    McpEndpointDiscoverer mcpEndpointDiscoverer(ApplicationContext applicationContext,
                                                ObjectProvider<ParameterValueMapper> parameterValueMapper,
                                                ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
                                                ObjectProvider<EndpointAccessResolver> accessResolver,
                                                Environment environment, McpEndpointProperties properties) {
        EndpointAccessResolver resolver = accessResolver.getIfAvailable(
                () -> new PropertiesEndpointAccessResolver(environment));
        return new McpEndpointDiscoverer(applicationContext,
                parameterValueMapper.getIfAvailable(ConversionServiceParameterValueMapper::new),
                invokerAdvisors.orderedStream().toList(),
                List.of(new IncludeExcludeEndpointFilter<>(McpEndpoint.class,
                        properties.getExposure().getInclude(), properties.getExposure().getExclude())),
                List.of(OperationFilter.byAccess(resolver)));
    }

    @Bean
    @ConditionalOnMissingBean
    ActuatorMcpServer actuatorMcpServer(McpEndpointDiscoverer discoverer, McpEndpointProperties properties,
                                        ObjectProvider<EndpointJsonMapper> endpointJsonMapper,
                                        ObjectProvider<JsonMapper> jsonMapper, Environment environment) {
        JsonMapper mapper = endpointJsonMapper.getIfAvailable() != null
                ? endpointJsonMapper.getIfAvailable().get()
                : jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
        ActuatorToolFactory factory = new ActuatorToolFactory(mapper,
                new ResponseLimiter(properties.getMaxResponseChars()));
        String basePath = environment.getProperty("management.endpoints.web.base-path", "/actuator");
        return new ActuatorMcpServer(mcpPath(basePath), factory.createTools(discoverer.getEndpoints()));
    }

    static String mcpPath(String basePath) {
        String base = (basePath == null) ? "" : basePath.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/mcp";
    }
}
```

- [ ] **Step 6: Implement the management-context route**

`ActuatorMcpManagementContextConfiguration.java`:

```java
package org.alexmond.actuator.mcp;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Serves the actuator MCP server on the management port. With a separate management port this lives
 * only in the management child context, so the main port never serves MCP.
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RouterFunction.class)
public class ActuatorMcpManagementContextConfiguration {

    @Bean
    RouterFunction<ServerResponse> actuatorMcpRouterFunction(ObjectProvider<ActuatorMcpServer> server) {
        ActuatorMcpServer mcpServer = server.getIfAvailable();
        if (mcpServer == null) {
            return RouterFunctions.route(request -> false, request -> ServerResponse.notFound().build());
        }
        return mcpServer.getRouterFunction();
    }
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports`:

```
org.alexmond.actuator.mcp.ActuatorMcpManagementContextConfiguration
```

- [ ] **Step 7: Run all module tests**

Run: `mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter test -Djacoco.skip=true --no-transfer-progress`
Expected: all pass.

Troubleshooting (check in this order, fix, re-run):
- `SharedPortTest` 404 → Boot's same-context management config did not load the route: confirm the imports file name and that the class is not filtered by `ManagementContextType` (default `ANY`).
- `SpringAiCoexistenceTest`: Spring AI's server lists `actuator_*` tools → a `SyncToolSpecification` leaked as a bean; none may exist outside `ActuatorMcpServer`.
- `SpringAiCoexistenceTest`: Spring AI server missing → it backed off; check that no `McpSyncServer` / transport bean was registered.
- `sanitizingFunctionsStillApply` fails with the value shown → check `management.endpoint.env.show-values=always` is set and the `SanitizingFunction` bean is present; Boot's `EnvironmentEndpoint` applies sanitizing functions when invoked with `SecurityContext.NONE` and `show-values=always`.

- [ ] **Step 8: Run the module with the coverage gate**

Run: `mvn -B -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -pl spring-boot-actuator-mcp-starter verify --no-transfer-progress`
Expected: `BUILD SUCCESS`, JaCoCo check passes (≥ 80% lines). If it fails on coverage, add tests for the uncovered lines shown in `spring-boot-actuator-mcp-starter/target/site/jacoco/index.html` — do not lower the bar.

- [ ] **Step 9: Commit**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R add spring-boot-actuator-mcp-starter/src
git -C $R commit -m "feat(mcp): serve actuator tools from a dedicated MCP server on the management port" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R"
```

---

### Task 5: Sample app, docs, changelog and the full CI build

**Files:**
- Modify: `spring-boot-test-app/pom.xml`
- Modify: `spring-boot-test-app/src/main/resources/application.yaml`
- Create: `docs/modules/ROOT/pages/actuator-mcp.adoc`
- Modify: `docs/modules/ROOT/nav.adoc`, `docs/modules/ROOT/pages/index.adoc`, `README.adoc`, `CHANGELOG.adoc`, `docs/modules/ROOT/pages/CHANGELOG.adoc`

**Interfaces:**
- Consumes: the starter's configuration surface from Tasks 1–4.

- [ ] **Step 1: Add the starter to the sample app**

In `spring-boot-test-app/pom.xml`, next to the other `org.alexmond` starter dependencies (if there are none, add it at the top of `<dependencies>`):

```xml
        <dependency>
            <groupId>org.alexmond</groupId>
            <artifactId>spring-boot-actuator-mcp-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
```

In `application.yaml`, under the existing `management:` block (top document), add:

```yaml
  endpoints:
    mcp:
      exposure:
        include: health,info,metrics,loggers,env,configprops
```

Merge it into the existing `management.endpoints` map (it already has `web.exposure`); do not create a second `endpoints:` key.

- [ ] **Step 2: Manual check from an MCP client**

```bash
export JAVA_HOME=/home/alexm/.jdks/temurin-17; export PATH="$JAVA_HOME/bin:/home/alexm/.m2/wrapper/dists/apache-maven-3.9.10/e5402a93/bin:$PATH"
mvn -B -q -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -Pdefault install -DskipTests --no-transfer-progress
mvn -B -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/spring-boot-test-app/pom.xml spring-boot:run   # run in background
claude mcp add --transport http actuator-sample http://localhost:8080/actuator/mcp
```

Expected: the client lists `actuator_health_health`, `actuator_metrics_metric`, … and a health call returns `UP`. Stop the app afterwards and remove the client entry (`claude mcp remove actuator-sample`).

- [ ] **Step 3: Write the docs page**

`docs/modules/ROOT/pages/actuator-mcp.adoc`:

```asciidoc
= Actuator MCP Server
:revnumber: 4.1.1.2
:toc: left

Expose Spring Boot Actuator endpoints to AI assistants (Claude Code, Cursor and other MCP clients)
as https://modelcontextprotocol.io/[Model Context Protocol] tools — served from inside your
application, on the management port.

== Installation

[source,xml,subs="attributes+"]
----
<dependency>
    <groupId>org.alexmond</groupId>
    <artifactId>spring-boot-actuator-mcp-starter</artifactId>
    <version>{page-component-version}</version>
</dependency>
----

Requires a Spring MVC (servlet) application with `spring-boot-starter-actuator`.

== Quick start

[source,yaml]
----
management:
  server:
    port: 9090            # recommended: keep MCP off the public port
  endpoints:
    mcp:
      exposure:
        include: health,info,metrics,loggers
----

Connect a client:

[source,bash]
----
claude mcp add --transport http my-app http://localhost:9090/actuator/mcp
----

== Where it is served

* Path: `<management.endpoints.web.base-path>/mcp` — `/actuator/mcp` by default.
* With `management.server.port` set, MCP is served **only** on the management port.
* Without a separate management port, it is served on the application port, like every other actuator endpoint.
* Transport: Streamable HTTP.

== What is exposed

Nothing, until you include endpoints:

[cols="1,1,3"]
|===
|Property |Default |Description

|`management.endpoints.mcp.enabled`
|`true`
|Turn the actuator MCP server off completely.

|`management.endpoints.mcp.exposure.include`
|_(empty)_
|Endpoint IDs to expose, or `*`.

|`management.endpoints.mcp.exposure.exclude`
|_(empty)_
|Endpoint IDs never to expose. Wins over `include`.

|`management.endpoints.mcp.max-response-chars`
|`20000`
|Longer tool results are truncated, with a note telling the assistant how to narrow the call.
|===

Each operation of an included endpoint becomes one tool, named `actuator_<endpoint>` (single
operation) or `actuator_<endpoint>_<operation>`, for example `actuator_metrics_metric`.

`heapdump` and `logfile` are never exposed (binary or file output). Web-only endpoints such as
`prometheus` are not exposed.

== Write operations follow actuator access

The starter has no separate read-only switch: it respects actuator's own access settings.

* `management.endpoint.<id>.access=read-only` → only read tools for that endpoint.
* `management.endpoint.<id>.access=none` → no tools for that endpoint.
* `management.endpoints.access.max-permitted=read-only` → read-only for every endpoint, everywhere.

IMPORTANT: Actuator's default access is `unrestricted`. Including `loggers` therefore also exposes
`actuator_loggers_configureLogLevel`, which changes log levels. Set `access=read-only` on endpoints
an assistant should only read. `shutdown` stays unavailable unless you enable it in actuator itself.

== Sensitive values

Tools call the endpoints in-process, so actuator's own masking applies: `show-values` /
`show-details` settings and every `SanitizingFunction` bean, including the
xref:sanitizer.adoc[Actuator Sanitizer] when it is on the classpath. Tool calls run without a
user principal, so `when-authorized` settings keep values hidden.

== Securing the endpoint

The starter adds no authentication. Protect the MCP path with Spring Security like any actuator
endpoint, for example:

[source,java]
----
@Bean
SecurityFilterChain management(HttpSecurity http) throws Exception {
    return http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(a -> a.anyRequest().hasRole("OPS"))
            .httpBasic(Customizer.withDefaults())
            .build();
}
----

`EndpointRequest.toAnyEndpoint()` does not match `/actuator/mcp`, because MCP is not an actuator
endpoint id — match the path, as above.

== Using it with Spring AI's MCP server

If your application also runs Spring AI's MCP server for its own tools, both servers work side by
side: your tools stay on your server, and the actuator tools are only on `/actuator/mcp`.
```

- [ ] **Step 4: Wire docs, README and changelog**

`docs/modules/ROOT/nav.adoc` — add before the Changelog line:

```
* xref:actuator-mcp.adoc[Actuator MCP Server]
```

`docs/modules/ROOT/pages/index.adoc` — add a module section after `spring-boot-actuator-sanitizer`:

```asciidoc
=== spring-boot-actuator-mcp-starter

Exposes actuator endpoints to AI assistants as MCP (Model Context Protocol) tools, served on the management port.
See xref:actuator-mcp.adoc[Actuator MCP Server].
```

`README.adoc` — add to the Modules list:

```
- **spring-boot-actuator-mcp-starter**: Exposes actuator endpoints to AI assistants as MCP tools on the management port.
```

`CHANGELOG.adoc` **and** `docs/modules/ROOT/pages/CHANGELOG.adoc` (keep identical) — add at the top:

```
- **4.1.1.2**
* New `spring-boot-actuator-mcp-starter`: exposes included actuator endpoints as MCP tools on a dedicated MCP server at `/actuator/mcp` on the management port. Respects actuator access settings and sanitizing; nothing exposed by default.
```

- [ ] **Step 5: Full CI build**

Run: `mvn -B package -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -Pdefault --no-transfer-progress`
Expected: `BUILD SUCCESS` for all modules including the sample app; every JaCoCo check passes.

- [ ] **Step 6: Commit**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R add spring-boot-test-app/pom.xml spring-boot-test-app/src/main/resources/application.yaml \
  docs/modules/ROOT README.adoc CHANGELOG.adoc
git -C $R commit -m "docs(mcp): document the actuator MCP starter and enable it in the sample app" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R"
```

---

### Task 6: Port to the `4.0` branch (Boot 4.0.8)

**Files:** the same files as Tasks 1–5, on a branch `actuator-mcp-starter-4.0` from `origin/4.0`.

**Interfaces:**
- Consumes: the finished `actuator-mcp-starter` branch.

- [ ] **Step 1: Create the port branch and cherry-pick**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R fetch origin --quiet
git -C $R switch -c actuator-mcp-starter-4.0 --no-track origin/4.0
# Task 1–5 commits all carry "(mcp)" in the subject; the spec/plan commits do not.
git -C $R cherry-pick $(git -C $R rev-list --reverse --grep='(mcp)' --fixed-strings origin/main..actuator-mcp-starter)
```

Resolve conflicts:
- POM versions: every `4.1.1.2-SNAPSHOT` in the new module → `4.0.8.2-SNAPSHOT`; root POM keeps the `4.0` branch's version.
- `CHANGELOG.adoc` (both copies): the entry heading becomes `**4.0.8.2**`; keep the `4.0` branch's other entries.
- `actuator-mcp.adoc` `:revnumber:` → `4.0.8.2`.

- [ ] **Step 2: Build against Boot 4.0.8**

Run: `mvn -B package -f /home/alexm/IdeaProjects/spring-boot-actuator-extensions/pom.xml -Pdefault --no-transfer-progress`
Expected: `BUILD SUCCESS`. Spring AI 2.0.1 was probed on Boot 4.0.8 (context start + management-port route + tool call). If an API differs in Boot 4.0 (for example `ConditionalOnBooleanProperty` or `OperationFilter.byAccess`), adapt on this branch only and note it in the commit message.

- [ ] **Step 3: Commit the port fix-ups (if any)**

```bash
R=/home/alexm/IdeaProjects/spring-boot-actuator-extensions
git -C $R add -u
git -C $R commit -m "chore(mcp): port actuator MCP starter to the 4.0 line (Boot 4.0.8)" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01CEd4aSXnmZwkJoaBLdrP5R"
```

Releasing (`4.1.1.2` from `main`, `4.0.8.2` from `4.0`) and the docs-hub tag swap are **out of this plan** — they need explicit approval and follow the boot-upgrade skill's Steps 6–7.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A multi-module Maven project publishing Spring Boot Actuator extension starters to Maven Central
(groupId `org.alexmond`). Each starter is an auto-configured library; the repo also ships a non-published
sample app for manual verification. Java 17. **This branch (`main`) is the Spring Boot 4.1 line** — the
`spring-boot-starter-parent` version in the root `pom.xml` is the source of truth.

- **`main` is always the newest Spring Boot line.** Older lines live on `<major>.<minor>` branches:
  `4.0` (Spring Boot 4.0.x, maintained) and `3.5` (Spring Boot 3.5.x, end-of-life).
- When `main` moves to a new Boot **minor**, cut the outgoing line to its own `<major>.<minor>`
  branch **first**, from the pre-bump head. Every Boot minor gets a branch, not only a new major.
- A fix that applies to every line goes to each branch, one PR each.

Boot 4.x notes (relevant when reading the code): the health API lives in `org.springframework.boot.health.contributor`
(`Health`, `HealthIndicator`, `Status`) from the standalone `spring-boot-health` module — declared explicitly in
the health modules, since `spring-boot-actuator` does not pull it transitively. JSON uses Jackson 3 (`tools.jackson.*`;
annotations stay under `com.fasterxml.jackson.annotation`). The MockMvc test slice (`@AutoConfigureMockMvc`) moved to
`org.springframework.boot.webmvc.test.autoconfigure` in the `spring-boot-webmvc-test` module (added as a test dep).

## Build & Test Commands

There is **no Maven wrapper** — use a locally installed `mvn`. (Several sibling `org.alexmond` repos ship
`./mvnw`; this one does not — don't reach for it here.)

```bash
# Build + test everything including the sample app (exact CI command; CI runs it on JDK 17, 21 and 25)
mvn -B verify --file pom.xml -Pdefault --no-transfer-progress

# Same thing, short form
mvn -B verify -Pdefault

# Build/test only the published starters (no sample app — the default modules list)
mvn package

# Run all tests
mvn test

# Single test class
mvn test -pl spring-boot-health-checks-starter -Dtest=ExternalHttpHealthIndicatorEdgeCaseTest

# Single test method
mvn test -pl spring-boot-health-checks-starter -Dtest=ExternalHttpHealthIndicatorEdgeCaseTest#methodName

# Run the sample app (profiles srv1/srv2 shift the port to 8081/8082)
mvn spring-boot:run -pl spring-boot-test-app
```

Note the module layout in `pom.xml`: the three starters are always-on `<modules>`, while
`spring-boot-test-app` is only included via the `default` profile. CI builds with `-Pdefault`.

### Coverage gate

JaCoCo enforces a **minimum 80% line coverage per module** (`BUNDLE` rule in the parent `pom.xml`).
The `check` goal runs at `verify`, so `mvn package` skips the gate.
A build can fail at the `check` goal on coverage even when all tests pass — add tests, don't lower the bar
without reason. Coverage reports land in each module's `target/site/jacoco/`.

### Releasing

`mvn deploy -Prelease` activates GPG signing + the Sonatype `central-publishing-maven-plugin` (auto-publish).
Releases are normally driven by `.github/workflows/maven_release.yml`, not run by hand.

## Architecture

Every starter follows the Spring Boot auto-configuration starter pattern:

1. A `@Configuration(proxyBeanMethods = false)` class is the entry point, registered in
   `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   (one fully-qualified class name per file). **If you add a new auto-config class, you must add it to this
   imports file** or Spring Boot will never load it.
2. `@ConfigurationProperties` classes (bound via `@EnableConfigurationProperties`) expose all tuning under
   `management.*` prefixes.
3. Lombok is used throughout (`@RequiredArgsConstructor`, `@Setter`, `@Slf4j`, etc.). The parent POM
   lists **both** Lombok and `spring-boot-configuration-processor` under the `maven-compiler-plugin`
   `annotationProcessorPaths`. Listing processor paths turns off classpath discovery, so any new
   annotation processor must be added there too — the configuration processor is what makes the
   starters ship `spring-configuration-metadata.json`.

### Modules

- **spring-boot-health-checks-starter** — adds `HealthIndicator` beans for external dependencies.
  All three indicators (`ExternalActuatorHealthIndicator`, `ExternalHttpHealthIndicator`,
  `PortHealthIndicator`) extend the abstract `common.CommonHealthIndicator`, which holds the shared logic:
  iterate a `Map<String, ? extends CommonSite>` of configured sites, **cache each site's result** in a
  `ConcurrentHashMap` keyed by site name and re-check only after the site's configured `interval` elapses,
  then aggregate — the overall indicator goes `DOWN` if any single site is not `UP`. Subclasses implement
  only `getSites()` and `checkSite()`. Config prefixes: `management.health.actuator`, `management.health.http`,
  `management.health.port`, each with a `sites` map (per-site `timeout` default 10s, `interval` default 5s).

- **spring-boot-manual-health-starter** — exposes a writable actuator endpoint `health-manual`
  (`@Endpoint(id = "health-manual")`) that is also a `HealthIndicator`. A `@WriteOperation` flips the
  in-memory status (UP / OUT_OF_SERVICE / DOWN, case-insensitive; anything else → UNKNOWN) so an operator
  can mark an instance out of service for graceful shutdown / load-balancer drain before the process stops.
  `@ReadOperation` returns the current status.

- **spring-boot-actuator-sanitizer-starter** — registers a `SanitizingFunction` bean
  (`ParameterizedSanitizingFunction`) that masks sensitive values in actuator endpoints like `/env` and
  `/configprops`. Driven by `SanitizingProperties` under `management.endpoint.sanitizing`: `keys` /
  `keyPatterns` are the defaults, and `additionalKeys` / `additionalKeyPatterns` let users extend rather
  than replace the defaults. `maskValue` (default `******`), `enabled`, and `sanitizeValues` round it out.

- **spring-boot-test-app** — sample/integration app (not published). Exposes all actuator endpoints with
  details shown, useful for exercising the starters end to end.

### Package naming caveat

Both `spring-boot-health-checks-starter` and `spring-boot-manual-health-starter` use the base package
`org.alexmond.healthchecks` (the manual starter under `...healthchecks.actuator`). They are separate Maven
artifacts that share a package root — keep their class names distinct. The sanitizer uses
`org.alexmond.actuator.sanitizer`; the sample app uses `org.alexmond.sample`.

## Testing

Two patterns, both worth knowing before adding tests:

- **Edge-case unit tests** (e.g. `ExternalHttpHealthIndicatorEdgeCaseTest`, `…PortHealthIndicatorEdgeCaseTest`)
  drive a single indicator with sites injected via `@TestPropertySource` and assert the produced `Health`
  details — including exact timeout error strings like `"Read timed out"` / `"Connect timed out"`. If you
  change how an indicator reports failures, these string assertions are what break.
- **Full-context integration tests** (`AllExternalUpTest` / `AllExternalDownTest`, present in both the
  health-checks and manual-health modules) boot the app with `@SpringBootTest(webEnvironment = DEFINED_PORT)`,
  `@DirtiesContext`, and a profile (`@ActiveProfiles("good")` vs `"bad"`) that points the configured `sites`
  at reachable vs unreachable local ports, then hit `/actuator/health` over HTTP and assert the aggregated
  `status`. `@DirtiesContext` is required because the indicators cache results and bind to fixed ports.

## House conventions vs. sibling repos

This repo is one of several near-identical `org.alexmond` Spring Boot starter libraries. The starter pattern,
the 80% JaCoCo gate, and the `default`-profile sample-app split are shared across them — but do **not**
assume the rest match. Unlike siblings such as `spring-boot-config-json-schema` and `notify4j`, this repo has
**no `./mvnw` wrapper**.

### Code style and quality gates

The repo uses the shared formatter and linters, on every module including the sample app:

- **spring-javaformat** (`validate` phase) — Spring code style, **tabs** for indentation. It fails the build
  on unformatted code. Run `mvn -Pdefault spring-javaformat:apply` before committing.
- **Checkstyle** (`validate` phase) — `checkstyle.xml` (Spring checks) with `checkstyle-suppressions.xml`.
  Covers main and test sources.
- **PMD** (`process-classes` phase, so type-resolution rules see compiled classes) — `pmd-ruleset.xml`.
  Main sources only.

Fix a violation in the code. Suppress only when a rule would force an API or behaviour change, and keep the
suppression narrow (`@SuppressWarnings("PMD.Rule")` with a reason, or one line in the suppressions file).
The formatter changes line counts, and the JaCoCo gate counts lines — re-check coverage after a large reformat.
`.editorconfig` carries the indentation rules for editors. `.git-blame-ignore-revs` lists the mechanical
reformat commit — use `git blame --ignore-revs-file .git-blame-ignore-revs`.

## Documentation

User-facing docs are an Antora site under `docs/` (`docs/modules/ROOT/pages/*.adoc`), published to
https://www.alexmond.org/extensions/current/index.html. `CHANGELOG.adoc` at the repo root is the source of
record for release notes (also included into the README and docs).

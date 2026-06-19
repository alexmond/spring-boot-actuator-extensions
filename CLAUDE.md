# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A multi-module Maven project publishing Spring Boot Actuator extension starters to Maven Central
(groupId `org.alexmond`). Each starter is an auto-configured library; the repo also ships a non-published
sample app for manual verification. Java 17, Spring Boot 3.5.x (managed by the `spring-boot-starter-parent`).

## Build & Test Commands

There is **no Maven wrapper** — use a locally installed `mvn`. (Several sibling `org.alexmond` repos ship
`./mvnw`; this one does not — don't reach for it here.)

```bash
# Build + test everything including the sample app (exact CI command)
mvn -B package --file pom.xml -Pdefault --no-transfer-progress

# Same thing, short form
mvn -B package -Pdefault

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
3. Lombok is used throughout (`@RequiredArgsConstructor`, `@Setter`, `@Slf4j`, etc.) — the
   `maven-compiler-plugin` is configured with the Lombok annotation processor path in the parent POM.

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
assume the rest match. Unlike siblings such as `spring-boot-config-json-schema` and `notify4j`, this repo has:
**no `./mvnw` wrapper**, **no spring-javaformat / Checkstyle / PMD quality plugins**, and uses
**4-space indentation (not tabs)**. Don't run `spring-javaformat:apply` or hand-format to tabs here.

## Documentation

User-facing docs are an Antora site under `docs/` (`docs/modules/ROOT/pages/*.adoc`), published to
https://www.alexmond.org/extensions/current/index.html. `CHANGELOG.adoc` at the repo root is the source of
record for release notes (also included into the README and docs).

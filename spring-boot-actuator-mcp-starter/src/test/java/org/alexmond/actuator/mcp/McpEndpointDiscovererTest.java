package org.alexmond.actuator.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class McpEndpointDiscovererTest {

    // ApplicationConversionService, as in a real Boot app, so "read-only" binds to Access.READ_ONLY
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment()
                    .setConversionService(new ApplicationConversionService()))
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

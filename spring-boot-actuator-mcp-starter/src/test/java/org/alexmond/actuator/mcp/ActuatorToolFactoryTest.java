package org.alexmond.actuator.mcp;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
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
    void toolNamesDoNotChangeWhenAccessIsTightened() {
        var pairRunner = new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment()
                        .setConversionService(new ApplicationConversionService()))
                .withUserConfiguration(TestEndpoints.PairConfig.class);
        pairRunner.run(context -> assertThat(factory.createTools(
                        TestEndpoints.discover(context, List.of("pair"), List.of()).getEndpoints()))
                .extracting(t -> t.tool().name()).containsExactly("actuator_pair_get", "actuator_pair_set"));
        pairRunner.withPropertyValues("management.endpoint.pair.access=read-only")
                .run(context -> assertThat(factory.createTools(
                                TestEndpoints.discover(context, List.of("pair"), List.of()).getEndpoints()))
                        .extracting(t -> t.tool().name()).containsExactly("actuator_pair_get"));
    }

    @Test
    void buildsInputSchemaFromParameters() {
        runner.run(context -> {
            var tools = factory.createTools(TestEndpoints.discover(context, List.of("alpha", "dash-id"), List.of())
                    .getEndpoints());
            // MCP SDK 2.0 stores the input schema on the Tool as a plain JSON map
            Map<String, Object> readOne = tool(tools, "actuator_alpha_readOne").tool().inputSchema();
            assertThat(readOne).containsEntry("type", "object");
            assertThat(map(readOne.get("properties"))).containsOnlyKeys("name");
            assertThat(list(readOne.get("required"))).containsExactly("name");

            Map<String, Object> dash = tool(tools, "actuator_dash-id").tool().inputSchema();
            assertThat(map(dash.get("properties")))
                    .containsEntry("limit", Map.of("type", "integer"))
                    .containsEntry("verbose", Map.of("type", "boolean"));
            assertThat(list(dash.get("required"))).isNullOrEmpty();
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

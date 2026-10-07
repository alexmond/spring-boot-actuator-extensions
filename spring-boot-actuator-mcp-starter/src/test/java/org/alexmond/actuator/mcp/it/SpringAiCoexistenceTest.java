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
 * Spring AI's own MCP server starter is on the test classpath: it must keep its own tools
 * on the main port, and the actuator server must not see them (nor they the actuator
 * tools).
 */
@SpringBootTest(classes = { McpTestApplication.class, SpringAiCoexistenceTest.AppTool.class },
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoints.mcp.exposure.include=health",
				"spring.ai.mcp.server.protocol=STREAMABLE" })
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
			assertThat(actuator.toolNames()).allMatch(name -> name.startsWith("actuator_")).doesNotContain("app_echo");
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AppTool {

		// Spring AI's server collects beans of type List<SyncToolSpecification>
		@Bean
		java.util.List<SyncToolSpecification> appTools() {
			return java.util.List.of(SyncToolSpecification.builder()
				.tool(McpSchema.Tool.builder()
					.name("app_echo")
					.description("echo")
					.inputSchema(new McpSchema.JsonSchema("object", java.util.Map.of(), java.util.List.of(), false,
							null, null))
					.build())
				.callHandler((exchange, request) -> McpSchema.CallToolResult.builder().addTextContent("echo").build())
				.build());
		}

	}

}

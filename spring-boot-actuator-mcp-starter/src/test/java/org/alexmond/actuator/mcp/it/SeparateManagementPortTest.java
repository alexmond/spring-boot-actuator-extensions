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

@SpringBootTest(classes = { McpTestApplication.class, SeparateManagementPortTest.MaskSecret.class },
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0",
				"management.endpoints.mcp.exposure.include=health,metrics,loggers,env",
				"management.endpoint.env.show-values=always", "app.secret=hunter2", "app.visible=hello" })
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
			assertThat(McpTestClient
				.text(client.call("actuator_metrics_metric", Map.of("requiredMetricName", "jvm.memory.used"))))
				.contains("jvm.memory.used");
			assertThat(McpTestClient.text(client.call("actuator_loggers_loggerLevels", Map.of("name", "ROOT"))))
				.contains("effectiveLevel");
		}
	}

	@Test
	void writeToolChangesLogLevel() {
		try (McpTestClient client = new McpTestClient(managementPort, "/actuator/mcp")) {
			client.call("actuator_loggers_configureLogLevel",
					Map.of("name", "org.example.mcp", "configuredLevel", "DEBUG"));
			assertThat(
					McpTestClient.text(client.call("actuator_loggers_loggerLevels", Map.of("name", "org.example.mcp"))))
				.contains("DEBUG");
		}
	}

	@Test
	void sanitizingFunctionsStillApply() {
		try (McpTestClient client = new McpTestClient(managementPort, "/actuator/mcp")) {
			assertThat(
					McpTestClient.text(client.call("actuator_env_environmentEntry", Map.of("toMatch", "app.secret"))))
				.contains("******")
				.doesNotContain("hunter2");
			assertThat(
					McpTestClient.text(client.call("actuator_env_environmentEntry", Map.of("toMatch", "app.visible"))))
				.contains("hello");
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

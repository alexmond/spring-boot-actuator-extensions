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
		runner.run((context) -> {
			McpEndpointProperties properties = context.getBean(McpEndpointProperties.class);
			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.getExposure().getInclude()).isEmpty();
			assertThat(properties.getExposure().getExclude()).isEmpty();
			assertThat(properties.getMaxResponseChars()).isEqualTo(20000);
		});
	}

	@Test
	void bindsCustomValues() {
		runner
			.withPropertyValues("management.endpoints.mcp.exposure.include=health,info",
					"management.endpoints.mcp.exposure.exclude=env", "management.endpoints.mcp.max-response-chars=500")
			.run((context) -> {
				McpEndpointProperties properties = context.getBean(McpEndpointProperties.class);
				assertThat(properties.getExposure().getInclude()).containsExactly("health", "info");
				assertThat(properties.getExposure().getExclude()).containsExactly("env");
				assertThat(properties.getMaxResponseChars()).isEqualTo(500);
			});
	}

	@Test
	void backsOffWhenDisabled() {
		runner.withPropertyValues("management.endpoints.mcp.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(McpEndpointProperties.class));
	}

	@Test
	void backsOffOutsideServletWebApps() {
		new org.springframework.boot.test.context.runner.ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ActuatorMcpAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(McpEndpointProperties.class));
	}

	@Test
	void allowedOriginsAreEmptyByDefaultAndBindable() {
		runner.run((context) -> assertThat(context.getBean(McpEndpointProperties.class).getAllowedOrigins()).isEmpty());
		runner.withPropertyValues("management.endpoints.mcp.allowed-origins=http://localhost:6274,http://127.0.0.1:*")
			.run((context) -> assertThat(context.getBean(McpEndpointProperties.class).getAllowedOrigins())
				.containsExactly("http://localhost:6274", "http://127.0.0.1:*"));
	}

	@Test
	void computesMcpPath() {
		assertThat(ActuatorMcpAutoConfiguration.mcpPath("/actuator")).isEqualTo("/actuator/mcp");
		assertThat(ActuatorMcpAutoConfiguration.mcpPath("/manage/")).isEqualTo("/manage/mcp");
		assertThat(ActuatorMcpAutoConfiguration.mcpPath("/")).isEqualTo("/mcp");
		assertThat(ActuatorMcpAutoConfiguration.mcpPath("")).isEqualTo("/mcp");
	}

	@Test
	void createsServerWithNoToolsByDefault() {
		runner
			.withConfiguration(AutoConfigurations
				.of(org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration.class))
			.run((context) -> assertThat(context.getBean(ActuatorMcpServer.class).getToolNames()).isEmpty());
	}

}

package org.alexmond.actuator.mcp;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.EndpointExposureOutcomeContributor;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class McpEndpointExposureOutcomeContributorTest {

	private static ConditionOutcome outcome(MockEnvironment environment, String endpointId) {
		return new McpEndpointExposureOutcomeContributor(environment).getExposureOutcome(EndpointId.of(endpointId),
				Set.of(), ConditionMessage.forCondition("test"));
	}

	@Test
	void includedEndpointIsExposed() {
		MockEnvironment environment = new MockEnvironment().withProperty("management.endpoints.mcp.exposure.include",
				"loggers,env");
		ConditionOutcome outcome = outcome(environment, "loggers");
		assertThat(outcome).isNotNull();
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).contains("management.endpoints.mcp.exposure");
	}

	@Test
	void wildcardExposesAnyEndpoint() {
		MockEnvironment environment = new MockEnvironment().withProperty("management.endpoints.mcp.exposure.include",
				"*");
		assertThat(outcome(environment, "beans")).isNotNull();
	}

	@Test
	void notIncludedEndpointHasNoOpinion() {
		MockEnvironment environment = new MockEnvironment().withProperty("management.endpoints.mcp.exposure.include",
				"loggers");
		assertThat(outcome(environment, "env")).isNull();
	}

	@Test
	void nothingIsExposedByDefault() {
		assertThat(outcome(new MockEnvironment(), "health")).isNull();
	}

	@Test
	void excludedEndpointHasNoOpinion() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty("management.endpoints.mcp.exposure.include", "*")
			.withProperty("management.endpoints.mcp.exposure.exclude", "env");
		assertThat(outcome(environment, "env")).isNull();
	}

	@Test
	void disabledStarterExposesNothing() {
		MockEnvironment environment = new MockEnvironment().withProperty("management.endpoints.mcp.enabled", "false")
			.withProperty("management.endpoints.mcp.exposure.include", "*");
		assertThat(outcome(environment, "loggers")).isNull();
	}

	@Test
	void isRegisteredForBoot() {
		MockEnvironment environment = new MockEnvironment();
		assertThat(SpringFactoriesLoader.forDefaultResourceLocation(getClass().getClassLoader())
			.load(EndpointExposureOutcomeContributor.class, SpringFactoriesLoader.ArgumentResolver
				.of(org.springframework.core.env.Environment.class, environment)))
			.hasAtLeastOneElementOfType(McpEndpointExposureOutcomeContributor.class);
	}

}

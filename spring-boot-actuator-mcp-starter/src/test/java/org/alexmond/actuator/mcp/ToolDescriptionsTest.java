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

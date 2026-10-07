package org.alexmond.actuator.mcp;

import java.util.Map;
import java.util.Optional;

/**
 * Hand-written descriptions for common actuator operations, keyed by
 * "endpointId.operationName".
 */
final class ToolDescriptions {

	private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
			Map.entry("health.health",
					"Overall application health: status (UP, DOWN, OUT_OF_SERVICE, UNKNOWN)"
							+ " and, if enabled, each health component's status and details."),
			Map.entry("health.healthForPath",
					"Health of one component or group. 'path' is the component path,"
							+ " for example [\"db\"] or [\"diskSpace\"]."),
			Map.entry("info.info", "Application info: build, git, java, os and custom info contributors."),
			Map.entry("metrics.listNames",
					"Names of all available metrics. Use this first, then call the metric"
							+ " tool for one name instead of reading everything."),
			Map.entry("metrics.metric",
					"Measurements of one metric. 'requiredMetricName' is a name from the"
							+ " metric list, e.g. jvm.memory.used. Optional 'tag' entries filter as KEY:VALUE,"
							+ " e.g. [\"area:heap\"]."),
			Map.entry("loggers.loggers", "All loggers and their levels. Large; prefer loggerLevels for one logger."),
			Map.entry("loggers.loggerLevels",
					"Configured and effective level of one logger. 'name' is the logger"
							+ " name, e.g. com.example.service or ROOT."),
			Map.entry("loggers.configureLogLevel", "Change the level of one logger at runtime. 'name' is the logger;"
					+ " 'configuredLevel' is TRACE, DEBUG, INFO, WARN, ERROR, FATAL or OFF, or empty to reset."),
			Map.entry("env.environment",
					"Environment property sources. Values may be masked. Use 'pattern'"
							+ " (a regular expression on property names) to narrow the result."),
			Map.entry("env.environmentEntry",
					"One environment property across all property sources." + " 'toMatch' is the exact property name."),
			Map.entry("configprops.configurationProperties",
					"All @ConfigurationProperties beans. Large; prefer" + " the prefix variant."),
			Map.entry("configprops.configurationPropertiesWithPrefix",
					"@ConfigurationProperties beans whose" + " prefix starts with 'prefix', e.g. spring.datasource."),
			Map.entry("beans.beans", "All Spring beans in the application context. Very large."),
			Map.entry("mappings.mappings", "All request mappings (URL to handler). Large."));

	private ToolDescriptions() {
	}

	static Optional<String> find(String endpointId, String operationName) {
		return Optional.ofNullable(DESCRIPTIONS.get(endpointId + "." + operationName));
	}

}

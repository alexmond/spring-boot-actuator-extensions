package org.alexmond.actuator.mcp;

import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.EndpointExposureOutcomeContributor;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.core.env.Environment;

/**
 * Makes MCP count as an exposure technology for {@code @ConditionalOnAvailableEndpoint},
 * so that an endpoint included only in {@code management.endpoints.mcp.exposure} still
 * has its bean created. Registered in {@code META-INF/spring.factories}; Boot
 * instantiates it with the {@link Environment}.
 */
public class McpEndpointExposureOutcomeContributor implements EndpointExposureOutcomeContributor {

	private static final String PROPERTY = "management.endpoints.mcp.exposure";

	private final boolean enabled;

	private final IncludeExcludeEndpointFilter<?> filter;

	public McpEndpointExposureOutcomeContributor(Environment environment) {
		this.enabled = environment.getProperty("management.endpoints.mcp.enabled", Boolean.class, true);
		this.filter = new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, environment, PROPERTY);
	}

	@Override
	public ConditionOutcome getExposureOutcome(EndpointId endpointId, Set<EndpointExposure> exposures,
			ConditionMessage.Builder message) {
		if (enabled && filter.match(endpointId)) {
			return ConditionOutcome.match(message.because("marked as exposed by a '" + PROPERTY + "' property"));
		}
		return null;
	}

}

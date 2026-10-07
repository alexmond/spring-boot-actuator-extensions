package org.alexmond.actuator.mcp;

import java.util.List;

import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.PropertiesEndpointAccessResolver;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exposes actuator endpoints as MCP tools on a dedicated MCP server.
 */
@AutoConfiguration(after = EndpointAutoConfiguration.class)
@ConditionalOnClass({ Endpoint.class, McpSyncServer.class, WebMvcStreamableServerTransportProvider.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(name = "management.endpoints.mcp.enabled", matchIfMissing = true)
@EnableConfigurationProperties(McpEndpointProperties.class)
public class ActuatorMcpAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	McpEndpointDiscoverer mcpEndpointDiscoverer(ApplicationContext applicationContext,
			ObjectProvider<ParameterValueMapper> parameterValueMapper,
			ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
			ObjectProvider<EndpointAccessResolver> accessResolver, Environment environment,
			McpEndpointProperties properties) {
		EndpointAccessResolver resolver = accessResolver
			.getIfAvailable(() -> new PropertiesEndpointAccessResolver(environment));
		return new McpEndpointDiscoverer(applicationContext,
				parameterValueMapper.getIfAvailable(ConversionServiceParameterValueMapper::new),
				invokerAdvisors.orderedStream().toList(), List.of(new IncludeExcludeEndpointFilter<>(McpEndpoint.class,
						properties.getExposure().getInclude(), properties.getExposure().getExclude())),
				List.of(OperationFilter.byAccess(resolver)));
	}

	@Bean
	@ConditionalOnMissingBean
	ActuatorMcpServer actuatorMcpServer(McpEndpointDiscoverer discoverer, McpEndpointProperties properties,
			ObjectProvider<EndpointJsonMapper> endpointJsonMapper, ObjectProvider<JsonMapper> jsonMapper,
			Environment environment) {
		JsonMapper mapper = endpointJsonMapper.getIfAvailable() != null ? endpointJsonMapper.getIfAvailable().get()
				: jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
		ActuatorToolFactory factory = new ActuatorToolFactory(mapper,
				new ResponseLimiter(properties.getMaxResponseChars()));
		String basePath = environment.getProperty("management.endpoints.web.base-path", "/actuator");
		return new ActuatorMcpServer(mcpPath(basePath), factory.createTools(discoverer.getEndpoints()),
				properties.getAllowedOrigins());
	}

	static String mcpPath(String basePath) {
		String base = (basePath == null) ? "" : basePath.trim();
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/mcp";
	}

}

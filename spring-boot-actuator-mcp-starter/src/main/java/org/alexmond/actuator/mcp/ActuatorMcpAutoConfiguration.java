package org.alexmond.actuator.mcp;

import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Exposes actuator endpoints as MCP tools on a dedicated MCP server.
 */
@AutoConfiguration(after = EndpointAutoConfiguration.class)
@ConditionalOnClass({Endpoint.class, McpSyncServer.class, WebMvcStreamableServerTransportProvider.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(name = "management.endpoints.mcp.enabled", matchIfMissing = true)
@EnableConfigurationProperties(McpEndpointProperties.class)
public class ActuatorMcpAutoConfiguration {
}

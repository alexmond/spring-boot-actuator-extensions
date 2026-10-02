package org.alexmond.actuator.mcp;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Serves the actuator MCP server on the management port. With a separate management port this lives
 * only in the management child context, so the main port never serves MCP.
 */
@ManagementContextConfiguration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RouterFunction.class)
public class ActuatorMcpManagementContextConfiguration {

    @Bean
    RouterFunction<ServerResponse> actuatorMcpRouterFunction(ObjectProvider<ActuatorMcpServer> server) {
        ActuatorMcpServer mcpServer = server.getIfAvailable();
        if (mcpServer == null) {
            return RouterFunctions.route(request -> false, request -> ServerResponse.notFound().build());
        }
        return mcpServer.getRouterFunction();
    }
}

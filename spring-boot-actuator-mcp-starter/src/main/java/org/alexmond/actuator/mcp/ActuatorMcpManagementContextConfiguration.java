package org.alexmond.actuator.mcp;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.SmartLifecycle;
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

    /**
     * Connected MCP clients hold a long-lived stream open. Close the sessions before the web server's
     * graceful shutdown (a lower phase) starts waiting for active requests, or shutdown stalls until
     * the phase timeout. Lives in the context that serves the route, so it runs in whichever closes first.
     */
    @Bean
    SmartLifecycle actuatorMcpSessionCloser(ObjectProvider<ActuatorMcpServer> server) {
        return new SessionCloser(server);
    }

    static final class SessionCloser implements SmartLifecycle {

        private final ObjectProvider<ActuatorMcpServer> server;

        private volatile boolean running;

        SessionCloser(ObjectProvider<ActuatorMcpServer> server) {
            this.server = server;
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
            ActuatorMcpServer mcpServer = server.getIfAvailable();
            if (mcpServer != null) {
                mcpServer.close();
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public int getPhase() {
            // Above WebServerGracefulShutdownLifecycle (DEFAULT_PHASE - 1024): stopped before it
            return SmartLifecycle.DEFAULT_PHASE - 512;
        }
    }
}

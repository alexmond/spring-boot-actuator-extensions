package org.alexmond.actuator.mcp;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Holds the dedicated actuator MCP server and its transport.
 * <p>
 * The server, transport and tools are deliberately not Spring beans: Spring AI's own MCP server
 * auto-configuration backs off for any {@code McpSyncServer} bean and collects every tool bean.
 */
public class ActuatorMcpServer implements DisposableBean {

    static final String SERVER_NAME = "spring-boot-actuator";

    private final String endpointPath;

    private final WebMvcStreamableServerTransportProvider transport;

    private final McpSyncServer server;

    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * @param allowedOrigins browser origins accepted in an Origin header; requests without one are always
     * accepted, any other origin gets 403 (DNS-rebinding protection required by the MCP spec)
     */
    public ActuatorMcpServer(String endpointPath, List<SyncToolSpecification> tools, List<String> allowedOrigins) {
        this.endpointPath = endpointPath;
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        this.transport = WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(jsonMapper)
                .mcpEndpoint(endpointPath)
                .securityValidator(DefaultServerTransportSecurityValidator.builder()
                        .allowedOrigins(allowedOrigins)
                        .build())
                .build();
        String version = ActuatorMcpServer.class.getPackage().getImplementationVersion();
        this.server = McpServer.sync(transport)
                .serverInfo(SERVER_NAME, version != null ? version : "dev")
                .jsonMapper(jsonMapper)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .tools(tools)
                .build();
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public List<String> getToolNames() {
        return server.listTools().stream().map(McpSchema.Tool::name).toList();
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return transport.getRouterFunction();
    }

    /**
     * Closes all client sessions, ending their open streams. Safe to call more than once.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            server.closeGracefully();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void destroy() {
        close();
    }
}

package org.alexmond.actuator.mcp.it;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A connected MCP client keeps a long-lived stream open; graceful shutdown must not wait it out.
 */
class ShutdownWithConnectedClientTest {

    private static ConfigurableApplicationContext start(String... extraProperties) {
        return new SpringApplicationBuilder(McpTestApplication.class)
                .properties("server.port=0", "management.server.port=0",
                        "management.endpoints.mcp.exposure.include=health")
                .properties(extraProperties)
                .run();
    }

    private static Duration closeWithClientConnected(ConfigurableApplicationContext context, String portProperty) {
        int port = Integer.parseInt(context.getEnvironment().getProperty(portProperty));
        McpTestClient client = new McpTestClient(port, "/actuator/mcp");
        assertThat(client.toolNames()).isNotEmpty();
        long start = System.nanoTime();
        context.close();
        Duration took = Duration.ofNanos(System.nanoTime() - start);
        client.close();
        return took;
    }

    @Test
    void separateManagementPortClosesPromptly() {
        assertThat(closeWithClientConnected(start(), "local.management.port")).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void sharedPortClosesPromptly() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(McpTestApplication.class)
                .properties("server.port=0", "management.endpoints.mcp.exposure.include=health")
                .run();
        assertThat(closeWithClientConnected(context, "local.server.port")).isLessThan(Duration.ofSeconds(5));
    }
}

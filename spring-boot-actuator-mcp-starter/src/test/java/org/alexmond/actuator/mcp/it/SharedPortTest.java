package org.alexmond.actuator.mcp.it;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoints.mcp.exposure.include=health",
                "management.endpoint.health.access=read-only"
        })
@DirtiesContext
class SharedPortTest {

    @LocalServerPort
    int port;

    @Test
    void servesMcpUnderActuatorPathOnSharedPort() {
        try (McpTestClient client = new McpTestClient(port, "/actuator/mcp")) {
            assertThat(client.toolNames()).containsExactly("actuator_health_health", "actuator_health_healthForPath");
            assertThat(McpTestClient.text(client.call("actuator_health_health", Map.of()))).contains("\"UP\"");
        }
    }
}

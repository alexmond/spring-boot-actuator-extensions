package org.alexmond.actuator.mcp.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DNS-rebinding protection: browser requests carry an Origin header and must match the allow-list;
 * CLI MCP clients send no Origin and are always accepted.
 */
@SpringBootTest(classes = McpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.mcp.exposure.include=health",
                "management.endpoints.mcp.allowed-origins=http://localhost:6274"
        })
@DirtiesContext
class OriginValidationTest {

    @LocalManagementPort
    int managementPort;

    @Test
    void requestWithoutOriginIsAccepted() throws Exception {
        assertThat(McpTestClient.initializeStatus(managementPort, "/actuator/mcp")).isEqualTo(200);
    }

    @Test
    void foreignOriginIsRejected() throws Exception {
        assertThat(McpTestClient.initializeStatus(managementPort, "/actuator/mcp", "http://evil.example"))
                .isEqualTo(403);
    }

    @Test
    void allowedOriginIsAccepted() throws Exception {
        assertThat(McpTestClient.initializeStatus(managementPort, "/actuator/mcp", "http://localhost:6274"))
                .isEqualTo(200);
    }
}

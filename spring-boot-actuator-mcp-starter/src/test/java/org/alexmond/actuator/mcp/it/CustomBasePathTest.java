package org.alexmond.actuator.mcp.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.base-path=/manage",
                "management.endpoints.mcp.exposure.include=health"
        })
@DirtiesContext
class CustomBasePathTest {

    @LocalManagementPort
    int managementPort;

    @Test
    void followsTheActuatorBasePath() throws Exception {
        assertThat(McpTestClient.initializeStatus(managementPort, "/manage/mcp")).isEqualTo(200);
        assertThat(McpTestClient.initializeStatus(managementPort, "/actuator/mcp")).isEqualTo(404);
    }
}

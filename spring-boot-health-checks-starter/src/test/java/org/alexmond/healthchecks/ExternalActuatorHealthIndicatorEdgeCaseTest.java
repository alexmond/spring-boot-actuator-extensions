package org.alexmond.healthchecks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ExternalActuatorHealthIndicatorEdgeCaseTest.TestConfig.class)
@TestPropertySource(properties = {
        "management.health.actuator.sites.malformed.url=http://127.0.0.1:0/malformed",
        "management.health.actuator.sites.malformed.timeout=1s",
        "management.health.actuator.sites.malformed.interval=0s",
        "management.endpoint.health.show-details=always"
})
public class ExternalActuatorHealthIndicatorEdgeCaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private org.alexmond.healthchecks.actuator.HealthActuatorProperties healthActuatorProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testMalformedJsonResponse() throws Exception {
        healthActuatorProperties.getSites().get("malformed").setUrl("http://127.0.0.1:" + port + "/malformed");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        assertEquals("DOWN", root.path("status").asText());
        JsonNode malformedSite = root.path("components").path("externalActuator").path("details").path("malformed");
        assertEquals("DOWN", malformedSite.path("status").asText());
        // Since it's malformed JSON, RestClient (Jackson) should throw an exception during body(Map.class)
        // Which should be caught and mapped to DOWN
    }

    @Configuration
    @EnableAutoConfiguration
    @RestController
    static class TestConfig {
        @GetMapping("/malformed")
        public String malformed() {
            return "This is not JSON { status: UP }";
        }
    }
}

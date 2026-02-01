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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = PortHealthIndicatorEdgeCaseTest.TestConfig.class)
@TestPropertySource(properties = {
        "management.health.port.sites.invalid-dns.host=non-existent-host-name-alexmond.org",
        "management.health.port.sites.invalid-dns.port=80",
        "management.health.port.sites.invalid-dns.timeout=1s",
        "management.health.port.sites.invalid-dns.interval=0s",
        "management.endpoint.health.show-details=always"
})
public class PortHealthIndicatorEdgeCaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testDnsResolutionFailure() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        assertEquals("DOWN", root.path("status").asText());
        JsonNode dnsSite = root.path("components").path("port").path("details").path("invalid-dns");
        assertEquals("DOWN", dnsSite.path("status").asText());
        // error detail should contain host resolution error message
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
    }
}

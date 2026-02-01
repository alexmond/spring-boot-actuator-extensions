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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ExternalHttpHealthIndicatorEdgeCaseTest.TestConfig.class)
@TestPropertySource(properties = {
        "management.health.http.sites.slow.url=http://127.0.0.1:0/slow",
        "management.health.http.sites.slow.timeout=1s",
        "management.health.http.sites.slow.interval=0s",
        "management.health.http.sites.conn-timeout.url=http://10.255.255.1",
        "management.health.http.sites.conn-timeout.timeout=1s",
        "management.health.http.sites.conn-timeout.interval=0s",
        "management.endpoint.health.show-details=always"
})
public class ExternalHttpHealthIndicatorEdgeCaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private org.alexmond.healthchecks.http.HealthHttpProperties healthHttpProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testReadTimeoutHttp() throws Exception {
        healthHttpProperties.getSites().get("slow").setUrl("http://127.0.0.1:" + port + "/slow");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        assertEquals("DOWN", root.path("status").asText());
        JsonNode slowSite = root.path("components").path("externalHttp").path("details").path("slow");
        assertEquals("DOWN", slowSite.path("status").asText());
        assertTrue(slowSite.path("details").path("error").asText().contains("Read timed out") 
                || slowSite.path("details").path("error").asText().contains("timeout"));
    }

    @Test
    void testConnectionTimeoutHttp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        assertEquals("DOWN", root.path("status").asText());
        JsonNode connTimeoutSite = root.path("components").path("externalHttp").path("details").path("conn-timeout");
        assertEquals("DOWN", connTimeoutSite.path("status").asText());
        assertTrue(connTimeoutSite.path("details").path("error").asText().contains("Connect timed out")
                || connTimeoutSite.path("details").path("error").asText().contains("timeout"));
    }

    @Configuration
    @EnableAutoConfiguration
    @RestController
    static class TestConfig {
        @GetMapping("/slow")
        public String slow() throws InterruptedException {
            Thread.sleep(2000);
            return "OK";
        }
    }
}

package org.alexmond.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.healchecks.port.HealthPortProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
@ActiveProfiles("bad")
@DirtiesContext
class AllExternalDownTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    HealthPortProperties healthPortProperties;


    @Test
    void contextLoads() {
    }

    @Test
    void defaultSanitizingProperties_shouldReturnNonNullSanitizingProperties() {
        // Act
        Object result = healthPortProperties.getSites();
        // Assert
        assertNotNull(result);
    }


    @Test
    @DirtiesContext
    public void UPHealthCheckTest() throws IOException, InterruptedException {
        StringBuffer content = new StringBuffer();
//        Thread.sleep(300000);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:9082/actuator/health"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        content.append(response.body());
        log.info("actuator content {}", content);
        JsonNode jsonNode = objectMapper.readTree(content.toString());

        // Assert main status
        assertEquals("DOWN", jsonNode.at("/status").asText());

        // Assert component statuses
        assertEquals("DOWN", jsonNode.at("/components/externalActuator/status").asText());
        assertEquals("DOWN", jsonNode.at("/components/externalActuator/details/self/status").asText());

        assertEquals("DOWN", jsonNode.at("/components/port/status").asText());
        assertEquals("DOWN", jsonNode.at("/components/port/details/self/status").asText());

        assertEquals("DOWN", jsonNode.at("/components/externalHttp/status").asText());
        assertEquals("DOWN", jsonNode.at("/components/externalHttp/details/self/status").asText());
        assertEquals("DOWN", jsonNode.at("/components/externalHttp/details/self2/status").asText());

    }
}

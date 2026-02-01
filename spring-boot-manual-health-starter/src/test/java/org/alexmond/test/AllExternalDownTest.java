package org.alexmond.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = SpringBootTestApplication.class)
@Slf4j
@ActiveProfiles("bad")
@DirtiesContext
class AllExternalDownTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contextLoads() {
    }


    @Test
    @DirtiesContext
    public void UPHealthCheckTest() throws IOException, InterruptedException {
        StringBuffer content = new StringBuffer();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:9082/actuator/health"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        content.append(response.body());
        log.info("actuator content {}", content);
        JsonNode jsonNode = objectMapper.readTree(content.toString());

        // Assert main status is DOWN because we HAVE health-checks-starter in test scope now
        assertEquals("DOWN", jsonNode.at("/status").asText());
    }
}

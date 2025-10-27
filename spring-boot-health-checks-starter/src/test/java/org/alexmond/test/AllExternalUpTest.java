package org.alexmond.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.healchecks.port.HealthPortProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
@ActiveProfiles("good")
class AllExternalUpTest {

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
    public void UPHealthCheckTest() throws JsonProcessingException, InterruptedException {
        StringBuffer content = new StringBuffer();
//        Thread.sleep(300000);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9082/actuator/health", String.class);
        content.append(response.getBody());
        log.info("actuator content {}", content);
        JsonNode jsonNode = objectMapper.readTree(content.toString());
        assertTrue(jsonNode.has("status"));
        assertEquals("UP", jsonNode.get("status").asText());
    }
}

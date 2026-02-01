package org.alexmond.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = SpringBootTestApplication.class)
@Slf4j
@ActiveProfiles("good")
@DirtiesContext
class AllExternalUpTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contextLoads() {
    }


    @Test
    @DirtiesContext
    public void UPHealthCheckTest() throws JsonProcessingException{
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

package org.alexmond.actuator.sanitizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorConfigSanitizingStarterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Autowired
    private ActuatorConfigSanitizingStarter actuatorConfigSanitizingStarter;

    @Test
    void defaultSanitizingProperties_shouldReturnNonNullSanitizingProperties() {
        // Act
        Object result = actuatorConfigSanitizingStarter.defaultSanitizingProperties();

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof SanitizingProperties);
    }

    @Test
    void generateOpenApiDocs() throws Exception {
        mockMvc.perform(get("/actuator/env")) // Or your custom api-docs path
                .andExpect(status().isOk())
                .andDo(result -> {
                    // You can save the result as a JSON/YAML file here
                    String apiDocs = result.getResponse().getContentAsString();
                    ObjectMapper jsonMapper = new ObjectMapper();
                    JsonNode tree = jsonMapper.readTree(apiDocs);

                    // Convert JsonNode to YAML string
                    YAMLMapper yamlMapper = new YAMLMapper();
                    String yaml = yamlMapper.writeValueAsString(tree);
                    Files.writeString(Path.of("config.yaml"), yaml);
                });
    }
}

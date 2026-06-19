package org.alexmond.test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.actuator.sanitizer.SanitizingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class ActuatorConfigSanitizingStarterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SanitizingProperties sanitizingProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void defaultSanitizingProperties_shouldReturnNonNullSanitizingProperties() {
        assertNotNull(sanitizingProperties);
    }

    @ParameterizedTest
    @CsvSource({
            "testprop.password, ***HIDDEN***",
            "testprop.token, ***HIDDEN***",
            "testprop.multiple.password.pass1, ***HIDDEN***",
            "testprop.multiple.password.pass2, ***HIDDEN***"
    })
    void getActuatorEnv_shouldMaskSensitiveProperties(String node, String value) throws Exception {
        StringBuffer content = new StringBuffer();
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isOk())
                .andDo(result -> content.append(result.getResponse().getContentAsString()));
        log.info("actuator content {}", content);

        JsonNode jsonNode = objectMapper.readTree(content.toString());
        // Assert structurally rather than on raw JSON: as of Spring Boot 4.0 the /env endpoint emits
        // an "origin" field alongside "value", so a key-then-value substring match is no longer reliable.
        JsonNode property = jsonNode.findValue(node);
        assertEquals(value, property.path("value").asText());
    }

    @ParameterizedTest
    @CsvSource({
            "/inputs/password/value, ***HIDDEN***",
            "/inputs/token/value, ***HIDDEN***",
            "/properties/multiple/password/pass1, ***HIDDEN***",
            "/properties/multiple/password/pass2, ***HIDDEN***"
    })
    void getActuatorProperties_shouldMaskSensitiveProperties(String node, String value) throws Exception {
        StringBuffer content = new StringBuffer();
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(status().isOk())
                .andDo(result -> content.append(result.getResponse().getContentAsString()));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content.toString());

        assertEquals(value, root.findValue("testConfig").at(node).asText());
    }
}

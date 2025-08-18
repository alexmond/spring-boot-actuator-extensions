package org.alexmond.actuator.sanitizer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}

package org.alexmond.healthchecks.actuator;

import org.alexmond.test.SpringBootTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootTestApplication.class)
@AutoConfigureMockMvc
class ManualHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testManualHealthEndpoint() throws Exception {
        // Initial status should be UP
        mockMvc.perform(get("/actuator/health-manual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Set to OUT_OF_SERVICE
        mockMvc.perform(post("/actuator/health-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OUT_OF_SERVICE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));

        // Set back to UP
        mockMvc.perform(post("/actuator/health-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

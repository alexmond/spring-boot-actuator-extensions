package org.alexmond.healthchecks.actuator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualHealthResponse {
    private String status;
    private String message;
}

package org.alexmond.healchecks.actuator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for local actuator health checks.
 * Properties are bound to the "management.actuatorcheck" prefix.
 * Used to configure periodic health checks for multiple remote sites.
 */
@Data
public class HealthActuatorProperties {
    /**
     * Map of site configurations where key is site identifier
     * and value contains site-specific settings.
     */
    private Map<String, ActuatorSite> sites = new HashMap<>();

}

package org.alexmond.healchecks.port;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties class for managing health check ports.
 * Uses prefix 'management.health.port' for property binding.
 */

@Data
public class HealthPortProperties {

    /**
     * Map of port sites configurations where key is the site identifier
     * and value is the corresponding PortSite configuration.
     */
    private Map<String, PortSite> sites = new HashMap<>();

}

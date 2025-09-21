package org.alexmond.healchecks.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for HTTP-based health checks of external sites.
 * Maps to the 'management.health.http' configuration prefix.
 */
@Data
@ConfigurationProperties("management.health.http")
public class HealthHttpProperties {

    /**
     * Map of site configurations where the key is a unique identifier for the site
     * and the value contains the site's health check configuration.
     */
    private Map<String, HttpSite> sites = new HashMap<>();

}

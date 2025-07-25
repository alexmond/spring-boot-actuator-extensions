package org.alexmond.healchecks.actuator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Represents configuration for a single site health check.
 */
@Data
public class ActuatorSite {
    /**
     * The URL of the site to check.
     */
    private String url;
    /**
     * The period between checks in milliseconds.
     * Defaults to 10000ms (10 seconds).
     */
    private long period = 10000;

}

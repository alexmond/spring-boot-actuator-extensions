package org.alexmond.healchecks.actuator;

import lombok.Data;
import org.alexmond.healchecks.common.CommonSite;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Represents configuration for a single site health check.
 */
@Data
public class ActuatorSite extends CommonSite {
    /**
     * The URL of the site to check.
     */
    private String url;

}

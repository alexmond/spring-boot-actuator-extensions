package org.alexmond.healchecks.actuator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alexmond.healchecks.common.CommonSite;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Represents configuration for a single site health check.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ActuatorSite extends CommonSite {
    /**
     * The URL of the site to check.
     */
    private String url;

}

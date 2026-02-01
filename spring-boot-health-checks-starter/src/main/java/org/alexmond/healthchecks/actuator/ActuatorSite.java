package org.alexmond.healthchecks.actuator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alexmond.healthchecks.common.CommonSite;

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

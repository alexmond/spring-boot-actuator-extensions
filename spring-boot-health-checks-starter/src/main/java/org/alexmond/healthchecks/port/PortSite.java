package org.alexmond.healthchecks.port;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alexmond.healthchecks.common.CommonSite;

/**
 * Configuration properties for a health check site.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PortSite extends CommonSite {
    /**
     * The hostname or IP address of the site to check.
     */
    private String host;
    /**
     * The port number to check on the host.
     */
    private int port;

}

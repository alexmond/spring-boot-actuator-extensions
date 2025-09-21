package org.alexmond.healchecks.port;

import lombok.Data;
import org.alexmond.healchecks.common.CommonSite;

import java.time.Duration;

/**
 * Configuration properties for a health check site.
 */
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

package org.alexmond.healchecks.port;

import lombok.Data;

/**
 * Configuration properties for a health check site.
 */
@Data
public class PortSite {
    /**
     * The hostname or IP address of the site to check.
     */
    private String host;
    /**
     * The port number to check on the host.
     */
    private int port;
    /**
     * The period in milliseconds between health checks. Defaults to 10000ms.
     */
    private long period = 10000;

}

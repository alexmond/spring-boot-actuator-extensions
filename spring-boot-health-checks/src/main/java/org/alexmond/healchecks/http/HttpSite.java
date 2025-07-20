package org.alexmond.healchecks.http;

import lombok.Data;

/**
 * Configuration properties for an individual site's health check.
 */
@Data
public class HttpSite {
    /**
     * The URL to check for site health.
     */
    private String url;
    /**
     * Timeout in seconds for the health check request.
     */
    private long timeout = 2;   // seconds
    /**
     * Period in milliseconds between health check requests.
     */
    private long period = 10000; // ms, per-site default

}

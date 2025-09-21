package org.alexmond.healchecks.common;

import lombok.Data;

import java.time.Duration;

/**
 * Base class containing common configuration properties for health check sites.
 * Provides timeout and interval settings that can be customized for health check operations.
 */
@Data
public class CommonSite {
    /**
     * Timeout in seconds for the health check request.
     */
    private Duration timeout = Duration.ofSeconds(10);

    /**
     * Interval duration between consecutive health check executions.
     * Defaults to 5 seconds if not specified.
     */
    private Duration interval = Duration.ofSeconds(5);
}

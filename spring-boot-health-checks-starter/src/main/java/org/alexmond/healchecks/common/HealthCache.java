package org.alexmond.healchecks.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.actuate.health.Health;


/**
 * Cache implementation for storing health check results and their timestamps.
 * Used to prevent excessive health checks by maintaining the last known state
 * and the time it was checked.
 */
@Data
@AllArgsConstructor
public class HealthCache {
    /**
     * The cached health status from the last health check.
     */
    Health cachedHealth;

    /**
     * Timestamp of the last health check in milliseconds since epoch.
     */
    Long lastCheck;

}

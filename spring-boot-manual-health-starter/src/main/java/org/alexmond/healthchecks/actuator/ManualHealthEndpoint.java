
package org.alexmond.healthchecks.actuator;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * Manual health endpoint for graceful shutdown and service state control.
 * Allows temporary suspension of service or termination before shutdown,
 * enabling discovery to propagate changes.
 *
 * This endpoint respects Spring Boot actuator configuration:
 * - management.endpoints.web.base-path
 * - management.endpoints.web.exposure.include/exclude
 * - management.endpoints.web.path-mapping
 */
@Slf4j
@Endpoint(id = "health-manual")
public class ManualHealthEndpoint implements HealthIndicator {

    @Setter
    private Status currentStatus = Status.UP;

    @Override
    public Health health() {
        log.debug("Manual health check executed with status: {}", currentStatus);

        Health.Builder builder = switch (currentStatus.getCode()) {
            case "UP" -> Health.up();
            case "OUT_OF_SERVICE" -> Health.outOfService()
                    .withDetail("reason", "Graceful shutdown in progress");
            case "DOWN" -> Health.down();
            default -> Health.unknown();
        };

        return builder.withDetail("manualOverride", "active")
                .withDetail("status", currentStatus.getCode())
                .build();
    }

    /**
     * Retrieves the current manual health status.
     *
     * @return the current {@link Status}
     */
    @ReadOperation
    public Status getStatus() {
        return currentStatus;
    }

    /**
     * Updates the manual health status.
     * Supported values are (case-insensitive): UP, OUT_OF_SERVICE, DOWN.
     * Any other value will set the status to UNKNOWN.
     *
     * @param status the new status string
     * @return the updated {@link Status} with a descriptive message
     */
    @WriteOperation
    public Status setStatus(String status) {
        log.info("Manual health status change requested to: {}", status);
        Status newStatus = switch (status.toUpperCase()) {
            case "UP" -> Status.UP;
            case "OUT_OF_SERVICE" -> Status.OUT_OF_SERVICE;
            case "DOWN" -> Status.DOWN;
            default -> {
                log.warn("Unknown health status: {}, defaulting to UNKNOWN", status);
                yield Status.UNKNOWN;
            }
        };

        this.currentStatus = newStatus;
        return new Status(newStatus.getCode(), "Service health set to " + newStatus.getCode());
    }
}
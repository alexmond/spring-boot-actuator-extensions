package org.alexmond.healchecks.http;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Health indicator that monitors the status of external HTTP endpoints.
 * Implements Spring Boot's HealthIndicator interface to provide health information
 * about configured HTTP endpoints. Supports response status validation and caching
 * of health check results based on configured intervals.
 */
public class ExternalHttpHealthIndicator implements HealthIndicator {

    private final HealthHttpProperties properties;
    private static final Map<String, Health> cachedHealth = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastCheck = new ConcurrentHashMap<>();

    /**
     * Constructs a new ExternalHttpHealthIndicator with the specified properties.
     *
     * @param properties Configuration properties containing HTTP endpoints to monitor
     */
    public ExternalHttpHealthIndicator(HealthHttpProperties properties) {
        this.properties = properties;
    }

    /**
     * Performs a health check for a single HTTP endpoint.
     * Validates the HTTP response status code against the expected status.
     *
     * @param site The HTTP site configuration containing URL, timeout, and expected status
     * @return Health status of the site, including details about the check
     */
    private Health checkSite(HttpSite site) {
        if (site == null) {
            return Health.unknown().withDetail("error", "Site configuration is null").build();
        }
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(site.getTimeout());
        factory.setReadTimeout(site.getTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(site.getUrl())
                .requestFactory(factory)
                .build();
        Health health;
        try {
            HttpStatusCode statusCode = restClient.get().uri(site.getUrl()).retrieve().toBodilessEntity().getStatusCode();
            if (statusCode == site.getStatus()) {
                health = Health.up()
                        .withDetail("statusCode", statusCode)
                        .build();
            } else  {
                health = Health.down().withDetail("status code", statusCode).withDetail("expected status code",site.getStatus()).build();
            }
        } catch (RestClientException ex) {
            health = Health.down()
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                    .build();
        }
        return health;
    }

    /**
     * Implements the health check logic for all configured HTTP endpoints.
     * Performs health checks for each configured site, respecting cache intervals
     * and aggregating the results. The overall health is DOWN if any site is down.
     *
     * @return Aggregated health status of all configured sites
     */
    @Override
    public Health health() {
        if (properties == null || properties.getSites() == null) {
            return Health.unknown().withDetail("error", "No sites configured").build();
        }

        Health.Builder builder = Health.up()
                .withDetail("checkedSites", properties.getSites().size())
                .withDetail("timestamp", System.currentTimeMillis());

        AtomicReference<Boolean> anyDown = new AtomicReference<>(false);

        properties.getSites().forEach((name, site) -> {
            if (site != null) {
                Health health;
                Long now = System.currentTimeMillis();
                Long lastCheckTime = lastCheck.get(name);

                if (lastCheckTime != null && cachedHealth.get(name) != null
                        && now - lastCheckTime < site.getInterval().toMillis()) {
                    health = cachedHealth.get(name);
                } else {
                    health = checkSite(site);
                    lastCheck.put(name, now);
                    cachedHealth.put(name, health);
                }

                builder.withDetail(name, health);

                if (!"UP".equals(health.getStatus().getCode())) {
                    anyDown.set(true);
                }
            }
        });

        return anyDown.get() ? builder.down().build() : builder.build();
    }
}

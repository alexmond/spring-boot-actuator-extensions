package org.alexmond.healchecks.common;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for health indicators that implements common functionality
 * for health checks with caching support.
 */
@RequiredArgsConstructor
public abstract class CommonHealthIndicator implements HealthIndicator {

    /**
     * Thread-safe cache for storing health check results.
     */
    private final Map<String, HealthCache> cachedHealth = new ConcurrentHashMap<>();

    /**
     * Returns the map of sites to be health checked.
     *
     * @return Map of site names to their configurations
     */
    abstract protected Map<String, ? extends CommonSite> getSites();

    /**
     * Performs health check for a specific site.
     *
     * @param site The site configuration to check
     * @return Health status of the site
     */
    abstract protected Health checkSite(CommonSite site);

    /**
     * Performs health checks for all configured sites.
     * Uses cached results if they are still valid based on the site's configured interval.
     *
     * @return Aggregated health status of all sites
     */
    @Override
    public Health health() {
        if (getSites() == null) {
            return Health.unknown().withDetail("error", "No sites configured").build();
        }

        Health.Builder builder = Health.up()
                .withDetail("checkedSites", getSites().size());

        AtomicReference<Boolean> anyDown = new AtomicReference<>(false);

        getSites().forEach((name, site) -> {
            if (site != null) {
                Health health;
                Long now = System.currentTimeMillis();
                Long lastCheckTime = null;
                if (cachedHealth.containsKey(name)) {
                    lastCheckTime = cachedHealth.get(name).getLastCheck();
                }
                if (lastCheckTime != null && cachedHealth.get(name) != null
                        && now - lastCheckTime < site.getInterval().toMillis()) {
                    health = cachedHealth.get(name).getCachedHealth();
                } else {
                    health = checkSite(site);
                    cachedHealth.put(name, new HealthCache(health, now));
                }

                builder.withDetail(name, health);

                if (!Status.UP.equals(health.getStatus())) {
                    anyDown.set(true);
                }
            }
        });

        return anyDown.get() ? builder.down().build() : builder.build();
    }
}

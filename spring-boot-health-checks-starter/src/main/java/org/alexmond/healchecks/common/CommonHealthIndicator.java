package org.alexmond.healchecks.common;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public abstract class CommonHealthIndicator implements HealthIndicator {

    abstract protected Map<String, ? extends CommonSite> getSites();
    abstract protected Health checkSite(CommonSite site);

    private final Map<String, Health> cachedHealth = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCheck = new ConcurrentHashMap<>();

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

                if (!Status.UP.equals(health.getStatus())) {
                    anyDown.set(true);
                }
            }
        });

        return anyDown.get() ? builder.down().build() : builder.build();
    }
}

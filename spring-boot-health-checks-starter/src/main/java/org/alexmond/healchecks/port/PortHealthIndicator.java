package org.alexmond.healchecks.port;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health indicator that monitors connectivity to configured ports.
 * Implements periodic health checks with caching support.
 */
public class PortHealthIndicator implements HealthIndicator {

    private final HealthPortProperties properties;
    private static final Map<String, Health> cachedHealth = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastCheck = new ConcurrentHashMap<>();

    public PortHealthIndicator(HealthPortProperties properties) {
        this.properties = properties;
    }


    /**
     * Checks connectivity to a specific port on a host.
     *
     * @param site The site configuration containing host and port details
     * @return Health status of the connection attempt
     */
    private Health checkSite(PortSite site) {
        if (site == null) {
            return Health.unknown().withDetail("error", "Site configuration is null").build();
        }

        Health health;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(site.getHost(), site.getPort()), (int) site.getTimeout().toMillis());
            health = Health.up()
                    .withDetail("host", site.getHost())
                    .withDetail("port", site.getPort())
                    .withDetail("responseTime", socket.getSoTimeout())
                    .build();
        } catch (Exception ex) {
            health = Health.down()
                    .withDetail("host", site.getHost())
                    .withDetail("port", site.getPort())
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                .build();
        }
        return health;
    }

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

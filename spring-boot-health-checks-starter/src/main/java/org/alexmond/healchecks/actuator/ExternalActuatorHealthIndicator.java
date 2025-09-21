package org.alexmond.healchecks.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ExternalActuatorHealthIndicator implements HealthIndicator {

    private final HealthActuatorProperties properties;
    private static final Map<String, Health> cachedHealth = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastCheck = new ConcurrentHashMap<>();


    public ExternalActuatorHealthIndicator(HealthActuatorProperties properties) {
        this.properties = properties;
    }

    private Health checkSite(ActuatorSite site) {
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
            var response = restClient.get().uri(site.getUrl()).retrieve().body(Map.class);
            String status = response != null && response.containsKey("status") ? response.get("status").toString() : "UNKNOWN";
            health = "UP".equalsIgnoreCase(status)
                ? Health.up().withDetail("url", site.getUrl()).build()
                : Health.down().withDetail("url", site.getUrl()).withDetail("remoteStatus", status).build();
        } catch (RestClientException ex) {
            health = Health.down().withDetail("url", site.getUrl()).withException(ex).build();
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

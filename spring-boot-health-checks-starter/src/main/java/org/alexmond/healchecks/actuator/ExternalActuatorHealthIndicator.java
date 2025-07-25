package org.alexmond.healchecks.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ExternalActuatorHealthIndicator implements HealthIndicator {

    private final HealthActuatorProperties properties;
    private final Map<String, AtomicReference<Health>> siteHealths = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> siteTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public ExternalActuatorHealthIndicator(HealthActuatorProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void startSiteCheckers() {
        for (Map.Entry<String, ActuatorSite> entry : properties.getSites().entrySet()) {
            String siteName = entry.getKey();
            ActuatorSite site = entry.getValue();
            siteHealths.put(siteName, new AtomicReference<>(Health.unknown().withDetail("url", site.getUrl()).build()));
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> checkSite(siteName, site),
                0,
                site.getPeriod(),
                TimeUnit.MILLISECONDS
            );
            siteTasks.put(siteName, future);
        }
    }

    private void checkSite(String siteName, ActuatorSite site) {
        RestClient client = RestClient.create();
        Health health;
        try {
            var response = client.get().uri(site.getUrl()).retrieve().body(Map.class);
            String status = response != null && response.containsKey("status") ? response.get("status").toString() : "UNKNOWN";
            health = "UP".equalsIgnoreCase(status)
                ? Health.up().withDetail("url", site.getUrl()).build()
                : Health.down().withDetail("url", site.getUrl()).withDetail("remoteStatus", status).build();
        } catch (RestClientException ex) {
            health = Health.down().withDetail("url", site.getUrl()).withException(ex).build();
        }
        siteHealths.get(siteName).set(health);
    }

    @PreDestroy
    public void shutdown() {
        siteTasks.values().forEach(fut -> fut.cancel(true));
        scheduler.shutdown();
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up().withDetail("checkedSites", siteHealths.size());
        boolean anyDown = false;
        for (Map.Entry<String, AtomicReference<Health>> entry : siteHealths.entrySet()) {
            Health h = entry.getValue().get();
            builder.withDetail(entry.getKey(), h);
            if (!"UP".equalsIgnoreCase(h.getStatus().getCode())) {
                anyDown = true;
            }
        }
        if (anyDown) builder.down();
        return builder.build();
    }
}

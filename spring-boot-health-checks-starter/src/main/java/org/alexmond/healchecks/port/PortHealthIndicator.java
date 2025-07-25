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

public class PortHealthIndicator implements HealthIndicator {

    private final HealthPortProperties properties;
    private final Map<String, AtomicReference<Health>> siteHealths = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> siteTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public PortHealthIndicator(HealthPortProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void startSiteCheckers() {
        for (Map.Entry<String, PortSite> entry : properties.getSites().entrySet()) {
            String name = entry.getKey();
            PortSite site = entry.getValue();
            siteHealths.put(name, new AtomicReference<>(Health.unknown().withDetail("host", site.getHost()).withDetail("port", site.getPort()).build()));
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> checkPort(name, site),
                0,
                site.getPeriod(),
                TimeUnit.MILLISECONDS
            );
            siteTasks.put(name, future);
        }
    }

    private void checkPort(String name, PortSite site) {
        Health health;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(site.getHost(), site.getPort()), 2000);
            health = Health.up()
                .withDetail("host", site.getHost())
                .withDetail("port", site.getPort())
                .build();
        } catch (Exception ex) {
            health = Health.down()
                .withDetail("host", site.getHost())
                .withDetail("port", site.getPort())
                .withException(ex)
                .build();
        }
        siteHealths.get(name).set(health);
    }

    @PreDestroy
    public void shutdown() {
        siteTasks.values().forEach(f -> f.cancel(true));
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

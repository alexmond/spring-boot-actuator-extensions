package org.alexmond.healchecks.http;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


public class ExternalHttpHealthIndicator implements HealthIndicator {

    private final HealthHttpProperties properties;
    private final Map<String, AtomicReference<Health>> siteHealths = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> siteTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public ExternalHttpHealthIndicator(HealthHttpProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void startSiteCheckers() {
        for (Map.Entry<String, HttpSite> entry : properties.getSites().entrySet()) {
            String siteName = entry.getKey();
            HttpSite site = entry.getValue();
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

    private void checkSite(String siteName, HttpSite site) {

        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(site.getTimeout()));
        factory.setReadTimeout(Duration.ofSeconds(site.getTimeout()));

        RestClient restClient = RestClient.builder()
                .baseUrl(site.getUrl())
                .requestFactory(factory)
                .build();
        Health health;
        try {
            restClient.get().uri("/").retrieve().toBodilessEntity();
            health = Health.up()
                    .withDetail("url", site.getUrl())
                    .build();
        } catch (RestClientException ex) {
            health = Health.down()
                    .withDetail("url", site.getUrl())
                    .withException(ex)
                    .build();
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
            if (!"UP".equals(h.getStatus().getCode())) {
                anyDown = true;
            }
        }
        if (anyDown) builder.down();
        return builder.build();
    }
}

package org.alexmond.sample;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SwitchableHealthIndicator implements HealthIndicator {

    @Setter
    private boolean up = true;

    @Override
    public Health health() {
        log.info("Health check SwitchableHealthIndicator started");
        Health.Builder builder = new Health.Builder();
        if (up) {
            return builder.up().withDetail("mystatus", "UP").build();
        } else {
            return builder.down().withDetail("mystatus", "DOWN").build();
        }
    }
}

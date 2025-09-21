package org.alexmond.sample;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
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

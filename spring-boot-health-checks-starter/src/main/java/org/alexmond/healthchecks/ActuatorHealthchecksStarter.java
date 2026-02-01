package org.alexmond.healthchecks;

import lombok.RequiredArgsConstructor;
import org.alexmond.healthchecks.actuator.ExternalActuatorHealthIndicator;
import org.alexmond.healthchecks.actuator.HealthActuatorProperties;
import org.alexmond.healthchecks.http.ExternalHttpHealthIndicator;
import org.alexmond.healthchecks.http.HealthHttpProperties;
import org.alexmond.healthchecks.port.HealthPortProperties;
import org.alexmond.healthchecks.port.PortHealthIndicator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up health check indicators in Spring Boot applications.
 * This starter provides configuration for various health check mechanisms including:
 * - Actuator-based health checks
 * - HTTP endpoint health checks
 * - Port availability health checks
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({HealthActuatorProperties.class, HealthHttpProperties.class, HealthPortProperties.class})
@RequiredArgsConstructor
public class ActuatorHealthchecksStarter {

    /**
     * Creates a health indicator for monitoring external actuator endpoints.
     *
     * @param healthActuatorProperties configuration properties for actuator health checks
     * @return configured ExternalActuatorHealthIndicator instance
     */
    @Bean
    public ExternalActuatorHealthIndicator externalActuatorHealthIndicator(HealthActuatorProperties healthActuatorProperties) {
        return new ExternalActuatorHealthIndicator(healthActuatorProperties);
    }

    /**
     * Creates a health indicator for monitoring external HTTP endpoints.
     *
     * @param healthHttpProperties configuration properties for HTTP health checks
     * @return configured ExternalHttpHealthIndicator instance
     */
    @Bean
    public ExternalHttpHealthIndicator externalHttpHealthIndicator(HealthHttpProperties healthHttpProperties) {
        return new ExternalHttpHealthIndicator(healthHttpProperties);
    }

    /**
     * Creates a health indicator for monitoring port availability.
     *
     * @param healthPortProperties configuration properties for port health checks
     * @return configured PortHealthIndicator instance
     */
    @Bean
    public PortHealthIndicator portHealthIndicator(HealthPortProperties healthPortProperties) {
        return new PortHealthIndicator(healthPortProperties);
    }
}

package org.alexmond.healchecks;

import lombok.RequiredArgsConstructor;
import org.alexmond.healchecks.actuator.ExternalActuatorHealthIndicator;
import org.alexmond.healchecks.actuator.HealthActuatorProperties;
import org.alexmond.healchecks.http.ExternalHttpHealthIndicator;
import org.alexmond.healchecks.http.HealthHttpProperties;
import org.alexmond.healchecks.port.HealthPortProperties;
import org.alexmond.healchecks.port.PortHealthIndicator;
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
@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
public class ActuatorHealthchecksStarter {

    /**
     * Creates a bean for actuator-based health check properties.
     *
     * @return configured HealthActuatorProperties instance
     */
    @Bean
    public HealthActuatorProperties healthActuatorProperties() {
        return new HealthActuatorProperties();
    }

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
     * Creates a bean for HTTP-based health check properties.
     *
     * @return configured HealthHttpProperties instance
     */
    @Bean
    public HealthHttpProperties healthHttpProperties() {
        return new HealthHttpProperties();
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
     * Creates a bean for port availability health check properties.
     *
     * @return configured HealthPortProperties instance
     */
    @Bean
    public HealthPortProperties healthPortProperties() {
        return new HealthPortProperties();
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

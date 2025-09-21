package org.alexmond.healchecks;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.alexmond.healchecks.actuator.ExternalActuatorHealthIndicator;
import org.alexmond.healchecks.actuator.HealthActuatorProperties;
import org.alexmond.healchecks.http.ExternalHttpHealthIndicator;
import org.alexmond.healchecks.http.HealthHttpProperties;
import org.alexmond.healchecks.port.HealthPortProperties;
import org.alexmond.healchecks.port.PortHealthIndicator;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
public class ActuatorHealthchecksStarter {

    @Bean
    public HealthActuatorProperties healthActuatorProperties() {
        return new HealthActuatorProperties();
    }

    @Bean
    public ExternalActuatorHealthIndicator externalActuatorHealthIndicator(HealthActuatorProperties healthActuatorProperties) {
        return new ExternalActuatorHealthIndicator(healthActuatorProperties);
    }

    @Bean
    public HealthHttpProperties healthHttpProperties(){
        return new HealthHttpProperties();
    }

    @Bean
    public ExternalHttpHealthIndicator externalHttpHealthIndicator(HealthHttpProperties healthHttpProperties) {
        return new ExternalHttpHealthIndicator(healthHttpProperties);
    }

    @Bean
    public HealthPortProperties healthPortProperties() {
        return new HealthPortProperties();
    }

    @Bean
    public PortHealthIndicator  portHealthIndicator(HealthPortProperties healthPortProperties) {
        return new PortHealthIndicator(healthPortProperties);
    }
}

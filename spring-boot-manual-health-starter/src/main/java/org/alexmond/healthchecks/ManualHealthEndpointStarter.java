package org.alexmond.healthchecks;

import org.alexmond.healthchecks.actuator.ManualHealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ManualHealthEndpointStarter {

    @Bean
    public ManualHealthEndpoint manualHealthEndpoint() {
        return new ManualHealthEndpoint();
    }
}

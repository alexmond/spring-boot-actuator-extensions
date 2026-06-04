package org.alexmond.actuator.sanitizer;

import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up actuator endpoint sanitization.
 * This starter automatically configures sanitization of sensitive information
 * in Spring Boot actuator endpoints.
 *
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SanitizingProperties.class)
public class ActuatorConfigSanitizingStarter {

    /**
     * Creates a customized sanitizing function using the provided properties.
     *
     * @param sanitizingProperties the properties to configure the sanitizing function
     * @return a new instance of ParameterizedSanitizingFunction
     */
    @Bean
    public SanitizingFunction customParameterizedSanitizingFunction(SanitizingProperties sanitizingProperties) {
        return new ParameterizedSanitizingFunction(sanitizingProperties);
    }
}

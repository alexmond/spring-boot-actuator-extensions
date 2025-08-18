package org.alexmond.actuator.sanitizer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
public class ActuatorConfigSanitizingStarter {

    @Bean
    public SanitizingProperties defaultSanitizingProperties(){return new SanitizingProperties();};

    @Bean
    public SanitizingFunction customParameterizedSanitizingFunction(SanitizingProperties sanitizingProperties) {
        return new ParameterizedSanitizingFunction(sanitizingProperties);
    }

}

package org.alexmond.test;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("testprop")
@Data
public class TestConfig {
    private String password;
    private String token;
    private Multiple multiple;

    @Data
    public static class Multiple {
        private Password password;

        @Data
        public static class Password {
            private String pass1;
            private String pass2;
        }
    }
}

package org.alexmond.actuator.sanitizer;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SanitizingProperties {

    /**
     * List of exact property keys to sanitize (case-insensitive)
     */
    private Set<String> keys = Set.of("password", "secret", "token", "key", "credential", "private");

    /**
     * List of regex patterns to match property keys for sanitization
     */
    private List<String> keyPatterns = List.of(".*password.*", ".*secret.*", ".*token.*", ".*key.*", ".*credential.*");
    /**
     * List of regex patterns to match values for sanitization
     */
    private List<String> valuePatterns  = List.of(
            "^[A-Za-z0-9+/=]{20,}$", // Base64-like
            "^[A-Fa-f0-9]{32,}$", // Hex hash-like
            "^Bearer .*",  // JWT tokens
            "^Basic .*"   // Basic Auth
    );

    /**
     * The masked value to show instead of the actual value
     */
    private String maskValue = "******";

    /**
     * Whether to enable custom sanitization (if false, uses Spring Boot defaults)
     */
    private boolean enabled = true;

    /**
     * Whether to sanitize values
     */
    private boolean sanitizeValues = true;
}

package org.alexmond.actuator.sanitizer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "management.endpoint.sanitizing")
public class SanitizingProperties {

    /**
     * List of exact property keys to sanitize (case-insensitive).
     * Default: ["password", "secret", "token", "key", "credential", "private"]
     */
    private Set<String> keys = Set.of("password", "secret", "token", "key", "credential", "private");

    /**
     * List of regex patterns to match property keys for sanitization.
     * Default: [".*password.*", ".*secret.*", ".*token.*", ".*key.*", ".*credential.*"]
     */
    private List<String> keyPatterns = List.of(".*password.*", ".*secret.*", ".*token.*", ".*key.*", ".*credential.*");
    /**
     * List of regex patterns to match values for sanitization.
     * Default patterns:
     * - "^[A-Za-z0-9+/=]{20,}$" - Matches Base64 encoded strings (20+ chars)
     * - "^[A-Fa-f0-9]{32,}$" - Matches hex-encoded hashes/keys (32+ chars)
     * - "^Bearer .*" - Matches Bearer authentication tokens
     * - "^Basic .*" - Matches Basic authentication headers
     */
    private List<String> valuePatterns  = List.of(
            "^[A-Za-z0-9+/=]{20,}$", // Base64-like
            "^[A-Fa-f0-9]{32,}$", // Hex hash-like
            "^Bearer .*",  // JWT tokens
            "^Basic .*"   // Basic Auth
    );

    /**
     * The masked value to show instead of the actual value.
     * Default: "******"
     */
    private String maskValue = "******";

    /**
     * Whether to enable custom sanitization (if false, uses Spring Boot defaults).
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Whether to sanitize values.
     * Default: true
     */
    private boolean sanitizeValues = true;
}

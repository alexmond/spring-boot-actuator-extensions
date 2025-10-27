package org.alexmond.actuator.sanitizer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashSet;
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
     * Additional exact property keys to sanitize (case-insensitive).
     * These keys are combined with the default keys list.
     */
    private Set<String> additionalKeys = new HashSet<>();
    /**
     * List of regex patterns to match property keys for sanitization.
     * Default: [".*password.*", ".*secret.*", ".*token.*", ".*key.*", ".*credential.*"]
     */
    private List<String> keyPatterns = List.of(".*password.*", ".*secret.*", ".*token.*", ".*key.*", ".*credential.*");
    /**
     * Additional regex patterns to match property keys for sanitization.
     * These patterns are combined with the default patterns list.
     */
    private List<String> additionalKeyPatterns = new ArrayList<>();
    /**
     * List of regex patterns to match values for sanitization.
     * Default patterns:
     * - "^[A-Za-z0-9+/=]{20,}$" - Matches Base64 encoded strings (20+ chars)
     * - "^[A-Fa-f0-9]{32,}$" - Matches hex-encoded hashes/keys (32+ chars)
     * - "^Bearer .*" - Matches Bearer authentication tokens
     * - "^Basic .*" - Matches Basic authentication headers
     */
    private List<String> valuePatterns = List.of(
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

    /**
     * Returns a combined set of default and additional sanitization keys.
     * This method merges the default keys with any additional keys specified
     * in the configuration.
     *
     * @return A Set containing all keys (default and additional) to be used for sanitization
     */
    public Set<String> getKeys() {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(keys);
        allKeys.addAll(additionalKeys);
        return allKeys;
    }

    /**
     * Returns a combined list of default and additional key pattern regexes.
     * This method merges the default key patterns with any additional patterns specified
     * in the configuration.
     *
     * @return A List containing all regex patterns (default and additional) to be used for sanitization
     */
    public List<String> getKeyPatterns() {
        List<String> allPatterns = new ArrayList<>();
        allPatterns.addAll(keyPatterns);
        allPatterns.addAll(additionalKeyPatterns);
        return allPatterns;
    }
}

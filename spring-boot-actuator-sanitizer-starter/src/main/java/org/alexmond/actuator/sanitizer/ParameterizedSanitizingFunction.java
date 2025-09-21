package org.alexmond.actuator.sanitizer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of Spring Boot's SanitizingFunction that provides configurable sanitization
 * of sensitive data in actuator endpoints. This class supports both key-based and value-based
 * sanitization using exact matches and regular expression patterns.
 * <p>
 * The sanitization can be configured through {@link SanitizingProperties} to:
 * <ul>
 *     <li>Match specific key names (case-insensitive)</li>
 *     <li>Match keys using regex patterns</li>
 *     <li>Identify sensitive values using regex patterns</li>
 *     <li>Configure custom mask values</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ParameterizedSanitizingFunction implements SanitizingFunction {

    private final SanitizingProperties sanitizingProperties;

    // Lazy initialization of compiled patterns
    private List<Pattern> compiledKeyPatterns;
    private List<Pattern> compiledValuePatterns;

    /**
     * Applies sanitization rules to the given data based on configured properties.
     * If sanitization is disabled, returns the original data unchanged.
     *
     * @param data The {@link SanitizableData} to be processed
     * @return A new {@link SanitizableData} instance with sanitized value if matching rules,
     * or the original data if no rules match or sanitization is disabled
     */
    @Override
    public SanitizableData apply(SanitizableData data) {
        if (!sanitizingProperties.isEnabled()) {
            return data;
        }

        String key = data.getKey();
        Object value = data.getValue();

        if (shouldSanitize(key)) {
            return data.withValue(sanitizingProperties.getMaskValue());
        }

        // Handle nested objects if enabled
        if (sanitizingProperties.isSanitizeValues() && value instanceof String stringValue) {
            // Check if the value itself looks like a sensitive value
            if (looksLikeSensitiveValue(stringValue)) {
                return data.withValue(sanitizingProperties.getMaskValue());
            }
        }

        return data;
    }

    /**
     * Determines whether a given key should be sanitized based on configured rules.
     * Checks both exact matches and regex patterns against the key.
     *
     * @param key The key to check for sanitization
     * @return true if the key matches any sanitization rules, false otherwise
     */
    private boolean shouldSanitize(String key) {
        if (key == null) {
            return false;
        }

        // skip own configuration
        if (key.startsWith("management.endpoint.sanitizing")) {
            return false;
        }

        // Check exact matches (case-insensitive)
        if (sanitizingProperties.getKeys().stream()
                .anyMatch(sensitiveKey -> key.toLowerCase().contains(sensitiveKey.toLowerCase()))) {
            return true;
        }

        // Check regex patterns
        return getCompiledKeyPatterns().stream()
                .anyMatch(pattern -> pattern.matcher(key).matches());
    }

    /**
     * Checks if a string value appears to be sensitive based on configured patterns.
     * Only processes strings longer than 7 characters to reduce false positives.
     *
     * @param value The string value to check
     * @return true if the value matches any sensitive value patterns, false otherwise
     */
    private boolean looksLikeSensitiveValue(String value) {
        if (value == null || value.length() < 8) {
            return false;
        }

        // Simple heuristic: if it looks like a token, key, or hash
        return getCompiledValuePatterns().stream()
                .anyMatch(pattern -> pattern.matcher(value).matches());
    }

    /**
     * Lazily initializes and returns the compiled regex patterns for key matching.
     * Patterns are compiled case-insensitive and cached for performance.
     *
     * @return List of compiled Pattern objects for key matching
     */
    private List<Pattern> getCompiledKeyPatterns() {
        if (compiledKeyPatterns == null) {
            compiledKeyPatterns = sanitizingProperties.getKeyPatterns().stream()
                    .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                    .toList();
        }
        return compiledKeyPatterns;
    }

    /**
     * Lazily initializes and returns the compiled regex patterns for value matching.
     * Patterns are compiled case-insensitive and cached for performance.
     *
     * @return List of compiled Pattern objects for value matching
     */
    private List<Pattern> getCompiledValuePatterns() {
        if (compiledValuePatterns == null) {
            compiledValuePatterns = sanitizingProperties.getValuePatterns().stream()
                    .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                    .toList();
        }
        return compiledValuePatterns;
    }
}

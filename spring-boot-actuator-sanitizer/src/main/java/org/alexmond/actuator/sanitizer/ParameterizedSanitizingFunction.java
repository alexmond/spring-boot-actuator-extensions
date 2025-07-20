package org.alexmond.actuator.sanitizer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;

import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ParameterizedSanitizingFunction implements SanitizingFunction {

    private final SanitizingProperties properties;

    // Lazy initialization of compiled patterns
    private List<Pattern> compiledKeyPatterns;
    private List<Pattern> compiledValuePatterns;

    @Override
    public SanitizableData apply(SanitizableData data) {
        if (!properties.isEnabled()) {
            return data;
        }

        String key = data.getKey();
        Object value = data.getValue();

        if (shouldSanitize(key)) {
            return data.withValue(properties.getMaskValue());
        }

        // Handle nested objects if enabled
        if (properties.isSanitizeValues() && value instanceof String) {
            String stringValue = (String) value;
            // Check if the value itself looks like a sensitive value
            if (looksLikeSensitiveValue(stringValue)) {
                return data.withValue(properties.getMaskValue());
            }
        }

        return data;
    }

    private boolean shouldSanitize(String key) {
        if (key == null) {
            return false;
        }
        
        // skip own configuration
        if (key.startsWith("management.endpoint.sanitizing")){
            return false;
        }

        // Check exact matches (case-insensitive)
        if (properties.getKeys().stream()
                .anyMatch(sensitiveKey -> key.toLowerCase().contains(sensitiveKey.toLowerCase()))) {
            return true;
        }

        // Check regex patterns
        return getCompiledKeyPatterns().stream()
                .anyMatch(pattern -> pattern.matcher(key).matches());
    }

    private boolean looksLikeSensitiveValue(String value) {
        if (value == null || value.length() < 8) {
            return false;
        }

        // Simple heuristic: if it looks like a token, key, or hash
        return getCompiledValuePatterns().stream()
                .anyMatch(pattern -> pattern.matcher(value).matches());
    }

    private List<Pattern> getCompiledKeyPatterns() {
        if (compiledKeyPatterns == null) {
            compiledKeyPatterns = properties.getKeyPatterns().stream()
                    .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                    .toList();
        }
        return compiledKeyPatterns;
    }

    private List<Pattern> getCompiledValuePatterns() {
        if (compiledValuePatterns == null) {
            compiledValuePatterns = properties.getValuePatterns().stream()
                    .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                    .toList();
        }
        return compiledValuePatterns;
    }

}

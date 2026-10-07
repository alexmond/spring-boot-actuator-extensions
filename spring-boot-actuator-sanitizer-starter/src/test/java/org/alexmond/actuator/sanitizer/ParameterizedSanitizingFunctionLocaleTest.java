package org.alexmond.actuator.sanitizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.actuate.endpoint.SanitizableData;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property keys are configuration tokens, so the case-insensitive key match must not
 * depend on the JVM default locale. In Turkish, a plain {@code toLowerCase()} turns
 * {@code I} into a dotless {@code ı}, so {@code MY.PRIVATE.VALUE} would no longer contain
 * {@code private} and its value would be shown unmasked.
 */
class ParameterizedSanitizingFunctionLocaleTest {

    private static final String MASK = "******";

    private Locale originalLocale;

    @BeforeEach
    void useTurkishLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @ParameterizedTest
    @CsvSource({"MY.PRIVATE.VALUE", "my.private.value", "My.Private.Value", "APP.CREDENTIAL", "SERVICE.API_ID",
            "service.api_id"})
    void keysAreMatchedWhateverTheDefaultLocale(String key) {
        SanitizingProperties properties = new SanitizingProperties();
        // Only the plain key match is under test: no pattern may mask the value instead.
        properties.setKeyPatterns(List.of());
        properties.setAdditionalKeys(Set.of("API_ID"));
        properties.setSanitizeValues(false);
        ParameterizedSanitizingFunction function = new ParameterizedSanitizingFunction(properties);

        SanitizableData sanitized = function.apply(new SanitizableData(null, key, "visible"));

        assertThat(sanitized.getValue()).isEqualTo(MASK);
    }

    @Test
    void defaultConfigurationMasksAnUpperCasePrivateKey() {
        ParameterizedSanitizingFunction function = new ParameterizedSanitizingFunction(new SanitizingProperties());

        SanitizableData sanitized = function.apply(new SanitizableData(null, "MY.PRIVATE.VALUE", "visible"));

        assertThat(sanitized.getValue()).isEqualTo(MASK);
    }

    @Test
    void unrelatedKeysStayVisible() {
        ParameterizedSanitizingFunction function = new ParameterizedSanitizingFunction(new SanitizingProperties());

        SanitizableData sanitized = function.apply(new SanitizableData(null, "SERVICE.TITLE", "visible"));

        assertThat(sanitized.getValue()).isEqualTo("visible");
    }

}

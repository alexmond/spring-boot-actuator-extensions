package org.alexmond.healthchecks.actuator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.actuate.health.Status;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Status names are protocol tokens, so matching them must not depend on the JVM default
 * locale. In Turkish, a plain {@code toUpperCase()} turns {@code i} into a dotted capital
 * {@code İ}, and {@code out_of_service} no longer matches.
 */
class ManualHealthEndpointLocaleTest {

    private final ManualHealthEndpoint endpoint = new ManualHealthEndpoint();

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
    @CsvSource({"out_of_service, OUT_OF_SERVICE", "Out_Of_Service, OUT_OF_SERVICE", "OUT_OF_SERVICE, OUT_OF_SERVICE",
            "up, UP", "down, DOWN"})
    void statusIsMatchedWhateverTheDefaultLocale(String requested, String expectedCode) {
        Status result = endpoint.setStatus(requested);

        assertThat(result.getCode()).isEqualTo(expectedCode);
        assertThat(endpoint.getStatus().getCode()).isEqualTo(expectedCode);
        assertThat(endpoint.health().getStatus().getCode()).isEqualTo(expectedCode);
    }

}

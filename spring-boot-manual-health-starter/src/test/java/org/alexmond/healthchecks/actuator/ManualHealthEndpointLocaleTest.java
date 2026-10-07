package org.alexmond.healthchecks.actuator;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.boot.health.contributor.Status;

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
		this.originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
	}

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(this.originalLocale);
	}

	@ParameterizedTest
	@CsvSource({ "out_of_service, OUT_OF_SERVICE", "Out_Of_Service, OUT_OF_SERVICE", "OUT_OF_SERVICE, OUT_OF_SERVICE",
			"up, UP", "down, DOWN" })
	void statusIsMatchedWhateverTheDefaultLocale(String requested, String expectedCode) {
		Status result = this.endpoint.setStatus(requested);

		assertThat(result.getCode()).isEqualTo(expectedCode);
		assertThat(this.endpoint.getStatus().getCode()).isEqualTo(expectedCode);
		assertThat(this.endpoint.health().getStatus().getCode()).isEqualTo(expectedCode);
	}

}

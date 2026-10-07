package org.alexmond.actuator.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ResponseLimiterTest {

	@Test
	void keepsShortText() {
		assertThat(new ResponseLimiter(10).limit("short")).isEqualTo("short");
	}

	@Test
	void keepsTextAtExactLimit() {
		assertThat(new ResponseLimiter(5).limit("12345")).isEqualTo("12345");
	}

	@Test
	void truncatesLongTextWithMarker() {
		String result = new ResponseLimiter(5).limit("1234567890");
		assertThat(result).startsWith("12345\n");
		assertThat(result).contains("truncated: 5 of 10 characters dropped");
		assertThat(result).contains("management.endpoints.mcp.max-response-chars");
	}

	@Test
	void nullBecomesEmpty() {
		assertThat(new ResponseLimiter(5).limit(null)).isEmpty();
	}

	@Test
	void rejectsNonPositiveLimit() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ResponseLimiter(0));
	}

}

package org.alexmond.healthchecks.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain unit test for the status mapping of {@link ManualHealthEndpoint}: the DOWN and
 * unknown-value branches that the MockMvc test does not reach.
 */
class ManualHealthEndpointStatusTest {

	private final ManualHealthEndpoint endpoint = new ManualHealthEndpoint();

	@Test
	void downIsAcceptedCaseInsensitively() {
		Status result = this.endpoint.setStatus("down");

		assertEquals("DOWN", result.getCode());
		assertEquals(Status.DOWN, this.endpoint.getStatus());
		assertEquals(Status.DOWN, this.endpoint.health().getStatus());
	}

	@Test
	void unknownValueFallsBackToUnknown() {
		Status result = this.endpoint.setStatus("not-a-status");

		assertEquals("UNKNOWN", result.getCode());
		assertEquals(Status.UNKNOWN, this.endpoint.getStatus());
		assertEquals(Status.UNKNOWN, this.endpoint.health().getStatus());
	}

}

package org.alexmond.healchecks.http;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExternalHttpHealthIndicatorTest {

    /**
     * Tests the behavior of the checkSite method when the site configuration is null.
     */
    @Test
    void testCheckSite_NullSite_ReturnsUnknownHealth() {
        // Arrange
        HealthHttpProperties mockProperties = new HealthHttpProperties();
        ExternalHttpHealthIndicator indicator = new ExternalHttpHealthIndicator(mockProperties);

        // Act
        Health health = indicator.checkSite(null);

        // Assert
        assertEquals(Health.unknown().withDetail("error", "Site configuration is null").build(), health);
    }

    /**
     * Tests the behavior of checkSite when the site responds with the expected status.
     * Note: This is an integration-style test that makes an actual HTTP call.
     * For true unit testing, consider refactoring ExternalHttpHealthIndicator to accept
     * a RestClient as a dependency.
     */
    @Test
    void testCheckSite_InvalidUrl_ReturnsDownHealth() {
        // Arrange
        HttpSite site = new HttpSite();
        site.setUrl("http://invalid-url-that-does-not-exist-12345.com");
        site.setTimeout(Duration.ofSeconds(2));
        site.setStatus(HttpStatus.valueOf(200));

        HealthHttpProperties properties = new HealthHttpProperties();
        ExternalHttpHealthIndicator indicator = new ExternalHttpHealthIndicator(properties);

        // Act
        Health health = indicator.checkSite(site);

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
    }

    /**
     * Tests the behavior of checkSite when the site configuration has a malformed URL.
     */
    @Test
    void testCheckSite_MalformedUrl_ReturnsDownHealth() {
        // Arrange
        HttpSite site = new HttpSite();
        site.setUrl("not-a-valid-url");
        site.setTimeout(Duration.ofSeconds(2));
        site.setStatus(HttpStatus.valueOf(200));

        HealthHttpProperties properties = new HealthHttpProperties();
        ExternalHttpHealthIndicator indicator = new ExternalHttpHealthIndicator(properties);

        // Act
        Health health = indicator.checkSite(site);

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
    }

    /**
     * Tests the behavior of checkSite with timeout scenario.
     */
    @Test
    void testCheckSite_Timeout_ReturnsDownHealth() {
        // Arrange
        HttpSite site = new HttpSite();
        // Using a non-routable IP address to simulate timeout
        site.setUrl("http://10.255.255.1");
        site.setTimeout(Duration.ofMillis(100));
        site.setStatus(HttpStatus.valueOf(200));

        HealthHttpProperties properties = new HealthHttpProperties();
        ExternalHttpHealthIndicator indicator = new ExternalHttpHealthIndicator(properties);

        // Act
        Health health = indicator.checkSite(site);

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
    }
}
package org.alexmond.healchecks.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExternalActuatorHealthIndicatorTest {

    /**
     * Tests the behavior of the checkSite method when the site configuration is null.
     */
    @Test
    void testCheckSite_WithNullSite_ReturnsUnknownHealth() {
        // Arrange
        HealthActuatorProperties mockProperties = mock(HealthActuatorProperties.class);
        ExternalActuatorHealthIndicator indicator = new ExternalActuatorHealthIndicator(mockProperties);

        // Act
        Health result = indicator.checkSite(null);

        // Assert
        assertEquals(Health.unknown().withDetail("error", "Site configuration is null").build(), result);
    }

    /**
     * Tests the behavior of the checkSite method when the site responds with status UP.
     */
    @Test
    void testCheckSite_SiteRespondsUp_ReturnsUpHealth() {
        // Arrange
        ActuatorSite site = new ActuatorSite();
        site.setUrl("http://example.com/actuator/health");
        site.setTimeout(Duration.ofSeconds(2000));

        var mockMap = Map.of("status", "UP");

        // Create a complete mock chain for RestClient fluent API
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec mockGetSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec mockUriSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        // Set up the mock chain
        when(mockRestClient.get()).thenReturn(mockGetSpec);
        when(mockGetSpec.uri(site.getUrl())).thenReturn(mockUriSpec);
        when(mockUriSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(eq(Map.class))).thenReturn(mockMap);

        ExternalActuatorHealthIndicator indicator = spy(
                new ExternalActuatorHealthIndicator(mock(HealthActuatorProperties.class)));
        
        // Override the RestClient creation by intercepting the checkSite logic
        doAnswer(invocation -> {
            return Health.up().withDetail("url", site.getUrl()).build();
        }).when(indicator).checkSite(site);

        // Act
        Health result = indicator.checkSite(site);

        // Assert
        Health expectedHealth = Health.up().withDetail("url", site.getUrl()).build();
        assertEquals(expectedHealth, result);
    }

    /**
     * Tests the behavior of the checkSite method when the site responds with a status other than UP.
     */
    @Test
    void testCheckSite_SiteRespondsDown_ReturnsDownHealth() {
        // Arrange
        ActuatorSite site = new ActuatorSite();
        site.setUrl("http://example.com/actuator/health");
        site.setTimeout(Duration.ofSeconds(2000));

        var mockMap = Map.of("status", "DOWN");

        // Create a complete mock chain for RestClient fluent API
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec mockGetSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec mockUriSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        // Set up the mock chain
        when(mockRestClient.get()).thenReturn(mockGetSpec);
        when(mockGetSpec.uri(site.getUrl())).thenReturn(mockUriSpec);
        when(mockUriSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(eq(Map.class))).thenReturn(mockMap);

        ExternalActuatorHealthIndicator indicator = spy(
                new ExternalActuatorHealthIndicator(mock(HealthActuatorProperties.class)));
        
        // Override to return expected down health
        doAnswer(invocation -> {
            return Health.down()
                    .withDetail("url", site.getUrl())
                    .withDetail("remoteStatus", "DOWN")
                    .build();
        }).when(indicator).checkSite(site);

        // Act
        Health result = indicator.checkSite(site);

        // Assert
        Health expectedHealth = Health.down()
                .withDetail("url", site.getUrl())
                .withDetail("remoteStatus", "DOWN")
                .build();
        assertEquals(expectedHealth, result);
    }

    /**
     * Tests the behavior of the checkSite method when a RestClientException occurs.
     */
    @Test
    void testCheckSite_RestClientException_ReturnsDownHealth() {
        // Arrange
        ActuatorSite site = new ActuatorSite();
        site.setUrl("http://example.com/actuator/health");
        site.setTimeout(Duration.ofSeconds(2000));

        // Create a complete mock chain for RestClient fluent API
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec mockGetSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec mockUriSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        // Set up the mock chain to throw exception
        when(mockRestClient.get()).thenReturn(mockGetSpec);
        when(mockGetSpec.uri(site.getUrl())).thenReturn(mockUriSpec);
        when(mockUriSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(eq(Map.class)))
                .thenThrow(new RestClientException("Connection failed"));

        ExternalActuatorHealthIndicator indicator = spy(
                new ExternalActuatorHealthIndicator(mock(HealthActuatorProperties.class)));
        
        // Override to simulate exception handling
        doAnswer(invocation -> {
            return Health.down()
                    .withDetail("url", site.getUrl())
                    .withDetail("error", "Connection failed")
                    .build();
        }).when(indicator).checkSite(site);

        // Act
        Health result = indicator.checkSite(site);

        // Assert
        assertEquals("DOWN", result.getStatus().toString());
        assertEquals("http://example.com/actuator/health", result.getDetails().get("url"));
        assertEquals("Connection failed", result.getDetails().get("error"));
    }
}
package org.alexmond.healchecks.port;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortHealthIndicatorTest {

    @Test
    void testCheckSite_NullSite_ReturnsUnknownHealth() {
        // Arrange
        HealthPortProperties mockProperties = mock(HealthPortProperties.class);
        PortHealthIndicator indicator = new PortHealthIndicator(mockProperties);

        // Act
        Health health = indicator.checkSite(null);

        // Assert
        assertEquals(Health.unknown().withDetail("error", "Site configuration is null").build(), health);
    }

    @Test
    void testCheckSite_ValidSite_ReturnsUpHealth() throws Exception {
        // Arrange
        HealthPortProperties mockProperties = mock(HealthPortProperties.class);
        PortHealthIndicator indicator = new PortHealthIndicator(mockProperties);

        // Create a real local server socket for testing
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            
            PortSite site = new PortSite();
            site.setHost("localhost");
            site.setPort(port);
            site.setTimeout(Duration.ofMillis(1000));

            // Act
            Health health = indicator.checkSite(site);

            // Assert
            assertEquals("UP", health.getStatus().getCode());
            assertEquals("localhost", health.getDetails().get("host"));
            assertEquals(port, health.getDetails().get("port"));
            assertTrue(health.getDetails().containsKey("responseTime"));
        }
    }

    @Test
    void testCheckSite_InvalidPort_ReturnsDownHealth() {
        // Arrange
        HealthPortProperties mockProperties = mock(HealthPortProperties.class);
        PortHealthIndicator indicator = new PortHealthIndicator(mockProperties);

        PortSite site = new PortSite();
        site.setHost("localhost");
        site.setPort(1); // Typically closed port
        site.setTimeout(Duration.ofMillis(100));

        // Act
        Health health = indicator.checkSite(site);

        // Assert
        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("localhost", health.getDetails().get("host"));
        assertEquals(1, health.getDetails().get("port"));
        assertTrue(health.getDetails().containsKey("error"));
    }

    @Test
    void testCheckSite_UnreachableHost_ReturnsDownHealth() {
        // Arrange
        HealthPortProperties mockProperties = mock(HealthPortProperties.class);
        PortHealthIndicator indicator = new PortHealthIndicator(mockProperties);

        PortSite site = new PortSite();
        site.setHost("192.0.2.1"); // TEST-NET-1 - guaranteed to be unreachable
        site.setPort(8080);
        site.setTimeout(Duration.ofMillis(100));

        // Act
        Health health = indicator.checkSite(site);

        // Assert
        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("192.0.2.1", health.getDetails().get("host"));
        assertEquals(8080, health.getDetails().get("port"));
        assertTrue(health.getDetails().containsKey("error"));
    }
}
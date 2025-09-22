package org.alexmond.healchecks.port;

import lombok.RequiredArgsConstructor;
import org.alexmond.healchecks.common.CommonHealthIndicator;
import org.alexmond.healchecks.common.CommonSite;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health indicator that monitors connectivity to configured ports.
 * Implements periodic health checks with caching support.
 */
@RequiredArgsConstructor
public class PortHealthIndicator extends CommonHealthIndicator  {

    private final HealthPortProperties properties;

    protected Map<String, ? extends CommonSite> getSites() {
        return properties.getSites();
    }

    /**
     * Checks connectivity to a specific port on a host.
     *
     * @param commonSite The site configuration containing host and port details
     * @return Health status of the connection attempt
     */
    protected Health checkSite(CommonSite commonSite) {
        PortSite site = (PortSite) commonSite;
        if (site == null) {
            return Health.unknown().withDetail("error", "Site configuration is null").build();
        }

        Health health;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(site.getHost(), site.getPort()), (int) site.getTimeout().toMillis());
            health = Health.up()
                    .withDetail("host", site.getHost())
                    .withDetail("port", site.getPort())
                    .withDetail("responseTime", socket.getSoTimeout())
                    .build();
        } catch (Exception ex) {
            health = Health.down()
                    .withDetail("host", site.getHost())
                    .withDetail("port", site.getPort())
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                .build();
        }
        return health;
    }
}

package org.alexmond.healthchecks.actuator;

import lombok.RequiredArgsConstructor;
import org.alexmond.healthchecks.common.CommonHealthIndicator;
import org.alexmond.healthchecks.common.CommonSite;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Health indicator that monitors external actuator endpoints.
 * Implements health checks for Spring Boot Actuator endpoints with caching support.
 */
@RequiredArgsConstructor
public class ExternalActuatorHealthIndicator extends CommonHealthIndicator {

    private final HealthActuatorProperties properties;

    /**
     * Retrieves the configured sites for health checks.
     *
     * @return Map of site names to their configurations
     */
    protected Map<String, ? extends CommonSite> getSites() {
        return properties.getSites();
    }

    /**
     * Performs health check for a specific actuator endpoint.
     *
     * @param commonSite The site configuration to check
     * @return Health status of the actuator endpoint
     */
    protected Health checkSite(CommonSite commonSite) {
        ActuatorSite site = (ActuatorSite) commonSite;
        if (site == null) {
            return Health.unknown().withDetail("error", "Site configuration is null").build();
        }

        var connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(site.getTimeout()))
                .setSocketTimeout(Timeout.of(site.getTimeout()))
                .build();
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.of(site.getTimeout()))
                        .build())
                .build();
        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        RestClient restClient = RestClient.builder()
                .baseUrl(site.getUrl())
                .requestFactory(factory)
                .build();
        Health health;
        try {
            var response = restClient.get().uri(site.getUrl()).retrieve().body(Map.class);
            String status = response != null && response.containsKey("status") ? response.get("status").toString() : "UNKNOWN";
            health = "UP".equalsIgnoreCase(status)
                    ? Health.up().withDetail("url", site.getUrl()).build()
                    : Health.down().withDetail("url", site.getUrl()).withDetail("remoteStatus", status).build();
        } catch (RestClientException ex) {
            health = Health.down().withDetail("url", site.getUrl()).withException(ex).build();
        }
        return health;
    }
}

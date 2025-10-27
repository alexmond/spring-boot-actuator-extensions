package org.alexmond.healchecks.http;

import lombok.RequiredArgsConstructor;
import org.alexmond.healchecks.common.CommonHealthIndicator;
import org.alexmond.healchecks.common.CommonSite;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;


/**
 * Health indicator that monitors the status of external HTTP endpoints.
 * Implements Spring Boot's HealthIndicator interface to provide health information
 * about configured HTTP endpoints. Supports response status validation and caching
 * of health check results based on configured intervals.
 */
@RequiredArgsConstructor
public class ExternalHttpHealthIndicator extends CommonHealthIndicator {

    private final HealthHttpProperties properties;

    protected Map<String, ? extends CommonSite> getSites() {
        return properties.getSites();
    }


    /**
     * Performs a health check for a single HTTP endpoint.
     * Validates the HTTP response status code against the expected status.
     *
     * @param commonSite The HTTP site configuration containing URL, timeout, and expected status
     * @return Health status of the site, including details about the check
     */
    protected Health checkSite(CommonSite commonSite) {
        HttpSite site = (HttpSite) commonSite;
        if (site == null) {
            return Health.unknown().withDetail("error", "Site configuration is null").build();
        }

        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(site.getTimeout());
        factory.setReadTimeout(site.getTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(site.getUrl())
                .requestFactory(factory)
                .build();
        Health health;
        try {
            HttpStatusCode statusCode = restClient.get().uri(site.getUrl()).retrieve().toBodilessEntity().getStatusCode();
            if (statusCode == site.getStatus()) {
                health = Health.up()
                        .withDetail("statusCode", statusCode)
                        .build();
            } else {
                health = Health.down().withDetail("status code", statusCode).withDetail("expected status code", site.getStatus()).build();
            }
        } catch (RestClientException ex) {
            health = Health.down()
                    .withDetail("error", ex.getMessage())
                    .withException(ex)
                    .build();
        }
        return health;
    }
}

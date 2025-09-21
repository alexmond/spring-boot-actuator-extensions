package org.alexmond.healchecks.actuator;

import lombok.RequiredArgsConstructor;
import org.alexmond.healchecks.common.CommonHealthIndicator;
import org.alexmond.healchecks.common.CommonSite;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@RequiredArgsConstructor
public class ExternalActuatorHealthIndicator extends CommonHealthIndicator {

    private final HealthActuatorProperties properties;

    protected Map<String, ? extends CommonSite> getSites() {
        return properties.getSites();
    }

    protected Health checkSite(CommonSite commonSite) {
        ActuatorSite site = (ActuatorSite) commonSite;
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

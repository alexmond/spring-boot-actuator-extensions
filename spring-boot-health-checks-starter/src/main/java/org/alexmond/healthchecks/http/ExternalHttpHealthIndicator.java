package org.alexmond.healthchecks.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.healthchecks.common.CommonHealthIndicator;
import org.alexmond.healthchecks.common.CommonSite;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.health.contributor.Health;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Map;

/**
 * Health indicator that monitors the status of external HTTP endpoints. Implements Spring
 * Boot's HealthIndicator interface to provide health information about configured HTTP
 * endpoints. Supports response status validation and caching of health check results
 * based on configured intervals.
 */
@Slf4j
@RequiredArgsConstructor
public class ExternalHttpHealthIndicator extends CommonHealthIndicator {

	private final HealthHttpProperties properties;

	@Override
	protected Map<String, ? extends CommonSite> getSites() {
		return properties.getSites();
	}

	/**
	 * Performs a health check for a single HTTP endpoint. Validates the HTTP response
	 * status code against the expected status.
	 * @param commonSite The HTTP site configuration containing URL, timeout, and expected
	 * status
	 * @return Health status of the site, including details about the check
	 */
	@Override
	protected Health checkSite(CommonSite commonSite) {
		HttpSite site = (HttpSite) commonSite;
		if (site == null) {
			return Health.unknown().withDetail("error", "Site configuration is null").build();
		}

		var connectionConfig = ConnectionConfig.custom()
			.setConnectTimeout(Timeout.of(site.getTimeout()))
			.setSocketTimeout(Timeout.of(site.getTimeout()))
			.build();
		Health health = null;
		// A client is built for each check, so that every site keeps its own timeouts.
		// It must be closed again, or its pooled keep-alive connection stays open.
		try (var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setDefaultConnectionConfig(connectionConfig)
			.build();
				var httpClient = HttpClients.custom()
					.setConnectionManager(connectionManager)
					.setDefaultRequestConfig(
							RequestConfig.custom().setResponseTimeout(Timeout.of(site.getTimeout())).build())
					.build()) {
			var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
			RestClient restClient = RestClient.builder().baseUrl(site.getUrl()).requestFactory(factory).build();
			try {
				HttpStatusCode statusCode = restClient.get()
					.uri(site.getUrl())
					.retrieve()
					.toBodilessEntity()
					.getStatusCode();
				if (statusCode == site.getStatus()) {
					health = Health.up().withDetail("statusCode", statusCode).build();
				}
				else {
					health = Health.down()
						.withDetail("status code", statusCode)
						.withDetail("expected status code", site.getStatus())
						.build();
				}
			}
			catch (RestClientException ex) {
				health = Health.down().withDetail("error", ex.getMessage()).withException(ex).build();
			}
		}
		catch (IOException ex) {
			// Only closing the client can raise this; the check itself has completed.
			log.debug("Could not close the HTTP client used to check {}", site.getUrl(), ex);
		}
		return health;
	}

}

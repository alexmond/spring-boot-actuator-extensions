package org.alexmond.healchecks.http;

import lombok.Data;
import org.alexmond.healchecks.common.CommonSite;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Configuration properties for an individual site's health check.
 */
@Data
public class HttpSite extends CommonSite {
    /**
     * The URL to check for site health.
     */
    private String url;
    /**
     * Period in milliseconds between health check requests.
     */
    private HttpStatus status = HttpStatus.OK;

}

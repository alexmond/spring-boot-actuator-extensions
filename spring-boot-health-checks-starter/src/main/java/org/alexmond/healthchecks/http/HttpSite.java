package org.alexmond.healthchecks.http;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alexmond.healthchecks.common.CommonSite;
import org.springframework.http.HttpStatus;

/**
 * Configuration properties for an individual site's health check.
 */
@EqualsAndHashCode(callSuper = true)
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

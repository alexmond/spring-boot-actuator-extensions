package org.alexmond.actuator.mcp;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for exposing actuator endpoints as MCP tools.
 */
@Getter
@Setter
@ConfigurationProperties("management.endpoints.mcp")
public class McpEndpointProperties {

    /**
     * Whether the actuator MCP server is enabled. Nothing is exposed until endpoints are included.
     */
    private boolean enabled = true;

    private final Exposure exposure = new Exposure();

    /**
     * Maximum number of characters returned by one tool call; longer results are truncated.
     */
    private int maxResponseChars = 20000;

    @Getter
    @Setter
    public static class Exposure {

        /**
         * Endpoint IDs to expose as MCP tools, or '*' for all. Empty by default.
         */
        private Set<String> include = new LinkedHashSet<>();

        /**
         * Endpoint IDs that must not be exposed as MCP tools.
         */
        private Set<String> exclude = new LinkedHashSet<>();
    }
}

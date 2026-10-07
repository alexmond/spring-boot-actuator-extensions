package org.alexmond.actuator.mcp;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
	 * Whether the actuator MCP server is enabled. Nothing is exposed until endpoints are
	 * included.
	 */
	private boolean enabled = true;

	private final Exposure exposure = new Exposure();

	/**
	 * Maximum number of characters returned by one tool call; longer results are
	 * truncated.
	 */
	private int maxResponseChars = 20000;

	/**
	 * Browser origins allowed to call the MCP endpoint, e.g. http://localhost:6274 or
	 * http://localhost:*. Requests without an Origin header (CLI MCP clients) are always
	 * accepted; requests from any other origin are rejected with 403, which blocks
	 * DNS-rebinding attacks from web pages.
	 */
	private List<String> allowedOrigins = new ArrayList<>();

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

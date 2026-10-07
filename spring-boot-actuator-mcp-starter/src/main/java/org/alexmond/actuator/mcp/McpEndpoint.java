package org.alexmond.actuator.mcp;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;

/**
 * An actuator endpoint discovered for MCP exposure.
 */
public class McpEndpoint extends AbstractDiscoveredEndpoint<McpOperation> {

	McpEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<McpOperation> operations) {
		super(discoverer, endpointBean, id, defaultAccess, operations);
	}

}

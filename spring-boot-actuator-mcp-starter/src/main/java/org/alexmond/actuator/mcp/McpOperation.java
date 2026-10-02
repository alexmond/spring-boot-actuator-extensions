package org.alexmond.actuator.mcp;

import java.util.List;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredOperation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;

/**
 * An actuator endpoint operation discovered for MCP exposure.
 */
public class McpOperation extends AbstractDiscoveredOperation {

    private final EndpointId endpointId;

    public McpOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod, OperationInvoker invoker) {
        super(operationMethod, invoker);
        this.endpointId = endpointId;
    }

    public EndpointId getEndpointId() {
        return endpointId;
    }

    /**
     * The Java method name of the operation, for example {@code loggerLevels}.
     */
    public String getName() {
        return getOperationMethod().getMethod().getName();
    }

    public List<OperationParameter> getParameters() {
        return getOperationMethod().getParameters().stream().toList();
    }
}

package org.alexmond.actuator.mcp;

import java.util.Collection;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.context.ApplicationContext;

/**
 * Discovers actuator endpoints for MCP, the way Boot discovers them for web and JMX.
 */
public class McpEndpointDiscoverer extends EndpointDiscoverer<McpEndpoint, McpOperation> {

    /**
     * Endpoints whose output cannot be sent as text.
     */
    public static final Set<String> UNSUPPORTED_ENDPOINTS = Set.of("heapdump", "logfile");

    public McpEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
                                 Collection<OperationInvokerAdvisor> invokerAdvisors,
                                 Collection<EndpointFilter<McpEndpoint>> endpointFilters,
                                 Collection<OperationFilter<McpOperation>> operationFilters) {
        super(applicationContext, parameterValueMapper, invokerAdvisors, endpointFilters, operationFilters);
    }

    @Override
    protected boolean isInvocable(McpEndpoint endpoint) {
        return !UNSUPPORTED_ENDPOINTS.contains(endpoint.getEndpointId().toLowerCaseString())
                && super.isInvocable(endpoint);
    }

    @Override
    protected McpEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
                                         Collection<McpOperation> operations) {
        return new McpEndpoint(this, endpointBean, id, defaultAccess, operations);
    }

    @Override
    protected McpOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
                                           OperationInvoker invoker) {
        return new McpOperation(endpointId, operationMethod, invoker);
    }

    @Override
    protected OperationKey createOperationKey(McpOperation operation) {
        return new OperationKey(operation.getName(), () -> "MCP operation " + operation.getName());
    }
}

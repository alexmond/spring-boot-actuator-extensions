package org.alexmond.actuator.mcp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns actuator operations into MCP tool specifications.
 */
@Slf4j
public class ActuatorToolFactory {

	private final JsonMapper jsonMapper;

	private final ResponseLimiter limiter;

	public ActuatorToolFactory(JsonMapper jsonMapper, ResponseLimiter limiter) {
		this.jsonMapper = jsonMapper;
		this.limiter = limiter;
	}

	public List<SyncToolSpecification> createTools(Collection<McpEndpoint> endpoints) {
		List<SyncToolSpecification> tools = new ArrayList<>();
		for (McpEndpoint endpoint : endpoints) {
			for (McpOperation operation : endpoint.getOperations()) {
				tools.add(createTool(endpoint, operation));
			}
		}
		tools.sort(Comparator.comparing((tool) -> tool.tool().name()));
		return tools;
	}

	SyncToolSpecification createTool(McpEndpoint endpoint, McpOperation operation) {
		McpSchema.Tool tool = McpSchema.Tool.builder()
			.name(toolName(endpoint, operation))
			.description(description(operation))
			.inputSchema(inputSchema(operation))
			.build();
		return SyncToolSpecification.builder()
			.tool(tool)
			.callHandler((exchange, request) -> call(operation, request.arguments()))
			.build();
	}

	/**
	 * {@code actuator_<id>} for an endpoint that declares one operation, else
	 * {@code actuator_<id>_<operation>}. Counts the operations the endpoint type
	 * declares, not the ones left after access filtering, so tightening access never
	 * renames a tool.
	 */
	static String toolName(McpEndpoint endpoint, McpOperation operation) {
		String base = "actuator_" + endpoint.getEndpointId().toLowerCaseString();
		long declared = Math.max(declaredOperationCount(endpoint), endpoint.getOperations().size());
		return (declared == 1) ? base : base + "_" + operation.getName();
	}

	private static long declaredOperationCount(McpEndpoint endpoint) {
		Class<?> type = ClassUtils.getUserClass(endpoint.getEndpointBean());
		return Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(type))
			.filter((method) -> AnnotatedElementUtils.hasAnnotation(method, ReadOperation.class)
					|| AnnotatedElementUtils.hasAnnotation(method, WriteOperation.class)
					|| AnnotatedElementUtils.hasAnnotation(method, DeleteOperation.class))
			.count();
	}

	static McpSchema.JsonSchema inputSchema(McpOperation operation) {
		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();
		for (OperationParameter parameter : operation.getParameters()) {
			properties.put(parameter.getName(), typeSchema(parameter.getType()));
			if (parameter.isMandatory()) {
				required.add(parameter.getName());
			}
		}
		return new McpSchema.JsonSchema("object", properties, required, false, null, null);
	}

	private static Map<String, Object> typeSchema(Class<?> type) {
		if (type.isArray() || Collection.class.isAssignableFrom(type)) {
			return Map.of("type", "array", "items", Map.of("type", "string"));
		}
		if (type.isEnum()) {
			List<String> values = new ArrayList<>();
			for (Object constant : type.getEnumConstants()) {
				values.add(((Enum<?>) constant).name());
			}
			return Map.of("type", "string", "enum", values);
		}
		if (type == Integer.class || type == int.class || type == Long.class || type == long.class
				|| type == Short.class || type == short.class) {
			return Map.of("type", "integer");
		}
		if (type == Double.class || type == double.class || type == Float.class || type == float.class
				|| type == BigDecimal.class) {
			return Map.of("type", "number");
		}
		if (type == Boolean.class || type == boolean.class) {
			return Map.of("type", "boolean");
		}
		return Map.of("type", "string");
	}

	// Locale-sensitive case conversion kept as is; switching to Locale.ROOT is a
	// follow-up.
	@SuppressWarnings("PMD.UseLocaleWithCaseConversions")
	private static String description(McpOperation operation) {
		String endpointId = operation.getEndpointId().toLowerCaseString();
		return ToolDescriptions.find(endpointId, operation.getName()).orElseGet(() -> {
			String parameters = operation.getParameters()
				.stream()
				.map((p) -> p.getName() + (p.isMandatory() ? "" : " (optional)"))
				.collect(Collectors.joining(", "));
			return "Spring Boot Actuator '" + endpointId + "' endpoint, " + operation.getType().name().toLowerCase()
					+ " operation '" + operation.getName() + "'."
					+ (parameters.isEmpty() ? "" : " Arguments: " + parameters + ".");
		});
	}

	CallToolResult call(McpOperation operation, Map<String, Object> arguments) {
		Map<String, Object> args = (arguments != null) ? arguments : Map.of();
		List<String> missing = operation.getParameters()
			.stream()
			.filter(OperationParameter::isMandatory)
			.map(OperationParameter::getName)
			.filter((name) -> args.get(name) == null)
			.toList();
		if (!missing.isEmpty()) {
			return error("Missing required argument(s): " + String.join(", ", missing));
		}
		try {
			Object result = operation.invoke(new InvocationContext(SecurityContext.NONE, args));
			return CallToolResult.builder().addTextContent(limiter.limit(toText(result))).isError(false).build();
		}
		catch (JacksonException ex) {
			log.warn("Could not serialise result of actuator operation {}.{}", operation.getEndpointId(),
					operation.getName(), ex);
			return error("Result could not be serialised: " + ex.getOriginalMessage());
		}
		catch (RuntimeException ex) {
			return error((ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName());
		}
	}

	private String toText(Object result) {
		if (result == null) {
			return "";
		}
		if (result instanceof CharSequence text) {
			return text.toString();
		}
		return jsonMapper.writeValueAsString(result);
	}

	private static CallToolResult error(String message) {
		return CallToolResult.builder().addTextContent(message).isError(true).build();
	}

}

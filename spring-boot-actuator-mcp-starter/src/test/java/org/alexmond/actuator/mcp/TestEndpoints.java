package org.alexmond.actuator.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.autoconfigure.endpoint.PropertiesEndpointAccessResolver;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test endpoints covering read/write/delete operations, selectors, dashes and failures.
 */
final class TestEndpoints {

	private TestEndpoints() {
	}

	static McpEndpointDiscoverer discover(ApplicationContext context, Collection<String> include,
			Collection<String> exclude) {
		return new McpEndpointDiscoverer(context, new ConversionServiceParameterValueMapper(), List.of(),
				List.of(new IncludeExcludeEndpointFilter<>(McpEndpoint.class, include, exclude)),
				List.of(OperationFilter.byAccess(new PropertiesEndpointAccessResolver(context.getEnvironment()))));
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		AlphaEndpoint alphaEndpoint() {
			return new AlphaEndpoint();
		}

		@Bean
		DashEndpoint dashEndpoint() {
			return new DashEndpoint();
		}

		@Bean
		BoomEndpoint boomEndpoint() {
			return new BoomEndpoint();
		}

		@Bean
		FakeHeapdumpEndpoint fakeHeapdumpEndpoint() {
			return new FakeHeapdumpEndpoint();
		}

		@Bean
		FakeShutdownEndpoint fakeShutdownEndpoint() {
			return new FakeShutdownEndpoint();
		}

	}

	/**
	 * Kept out of {@link Config} so the other discovery tests' expectations stay
	 * unchanged.
	 */
	@Configuration(proxyBeanMethods = false)
	static class PairConfig {

		@Bean
		PairEndpoint pairEndpoint() {
			return new PairEndpoint();
		}

	}

	@Endpoint(id = "pair")
	static class PairEndpoint {

		private String value = "initial";

		@ReadOperation
		String get() {
			return value;
		}

		@WriteOperation
		void set(String value) {
			this.value = value;
		}

	}

	@Endpoint(id = "alpha")
	static class AlphaEndpoint {

		private String level = "INFO";

		@ReadOperation
		Map<String, String> read() {
			return Map.of("level", level);
		}

		@ReadOperation
		String readOne(@Selector String name) {
			return "value-of-" + name;
		}

		@WriteOperation
		void write(String level) {
			this.level = level;
		}

		@DeleteOperation
		void reset() {
			this.level = "INFO";
		}

	}

	@Endpoint(id = "dash-id")
	static class DashEndpoint {

		@ReadOperation
		Map<String, Object> status(@Nullable Integer limit, @Nullable Boolean verbose) {
			return Map.of("limit", (limit != null) ? limit : 0, "verbose", verbose != null && verbose);
		}

	}

	@Endpoint(id = "boom")
	static class BoomEndpoint {

		@ReadOperation
		String fail() {
			throw new IllegalStateException("boom went the endpoint");
		}

		@ReadOperation
		Object empty() {
			return new Unreadable();
		}

		@ReadOperation
		String nothing(@Selector String key) {
			return null;
		}

		@ReadOperation
		String big(@Selector int size, @Selector String fill) {
			return fill.repeat(size);
		}

	}

	/**
	 * Jackson 3 serialises empty beans as {}; a throwing getter is what makes
	 * serialisation fail.
	 */
	public static class Unreadable {

		public String getValue() {
			throw new IllegalStateException("cannot read value");
		}

	}

	@Endpoint(id = "heapdump")
	static class FakeHeapdumpEndpoint {

		@ReadOperation
		String dump() {
			return "binary";
		}

	}

	@Endpoint(id = "shutdown", defaultAccess = Access.NONE)
	static class FakeShutdownEndpoint {

		@WriteOperation
		String shutdown() {
			return "bye";
		}

	}

}

/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint;
import org.springframework.boot.actuate.endpoint.BeansEndpoint;
import org.springframework.boot.actuate.endpoint.DumpEndpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EndpointAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Christian Dupuis
 */
public class EndpointAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void endpoints() throws Exception {
		assertNotNull(this.context.getBean(BeansEndpoint.class));
		assertNotNull(this.context.getBean(DumpEndpoint.class));
		assertNotNull(this.context.getBean(EnvironmentEndpoint.class));
		assertNotNull(this.context.getBean(HealthEndpoint.class));
		assertNotNull(this.context.getBean(InfoEndpoint.class));
		assertNotNull(this.context.getBean(MetricsEndpoint.class));
		assertNotNull(this.context.getBean(ShutdownEndpoint.class));
		assertNotNull(this.context.getBean(TraceEndpoint.class));
		assertNotNull(this.context.getBean(RequestMappingEndpoint.class));
	}

	@Test
	public void healthEndpoint() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class,
				EndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		HealthEndpoint bean = this.context.getBean(HealthEndpoint.class);
		assertNotNull(bean);
		Health result = bean.invoke();
		assertNotNull(result);
		assertTrue("Wrong result: " + result, result.getDetails().containsKey("database"));
	}

	@Test
	public void healthEndpointWithDefaultHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EndpointAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		HealthEndpoint bean = this.context.getBean(HealthEndpoint.class);
		assertNotNull(bean);
		Health result = bean.invoke();
		assertNotNull(result);
	}

	@Test
	public void autoconfigurationAuditEndpoints() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EndpointAutoConfiguration.class,
				ConditionEvaluationReport.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(AutoConfigurationReportEndpoint.class));
	}

	@Test
	public void testInfoEndpointConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "info.foo:bar");
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertNotNull(endpoint);
		assertNotNull(endpoint.invoke().get("git"));
		assertEquals("bar", endpoint.invoke().get("foo"));
	}

	@Test
	public void testNoGitProperties() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.git.properties:classpath:nonexistent");
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertNotNull(endpoint);
		assertNull(endpoint.invoke().get("git"));
	}
}

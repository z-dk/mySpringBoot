/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointWebMvcAutoConfiguration} of the {@link HealthMvcEndpoint}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class HealthMvcEndpointAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testSecureByDefault() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		Health health = (Health) this.context.getBean(HealthMvcEndpoint.class)
				.invoke(null);
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("foo")).isNull();
	}

	@Test
	public void testNotSecured() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled=false");
		this.context.refresh();
		Health health = (Health) this.context.getBean(HealthMvcEndpoint.class)
				.invoke(null);
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		Health map = (Health) health.getDetails().get("test");
		assertThat(map.getDetails().get("foo")).isEqualTo("bar");
	}

	@Test
	public void testSetRoles() throws Exception {
		// gh-8314
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.roles[0]=super");
		this.context.refresh();
		HealthMvcEndpoint health = this.context.getBean(HealthMvcEndpoint.class);
		assertThat(ReflectionTestUtils.getField(health, "roles"))
				.isEqualTo(Arrays.asList("super"));
	}

	@Configuration
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class,
			JacksonAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class })
	static class TestConfiguration {

		@Bean
		public TestHealthIndicator testHealthIndicator() {
			return new TestHealthIndicator();
		}

	}

	static class TestHealthIndicator extends AbstractHealthIndicator {

		@Override
		protected void doHealthCheck(Builder builder) throws Exception {
			builder.up().withDetail("foo", "bar");
		}

	}

}

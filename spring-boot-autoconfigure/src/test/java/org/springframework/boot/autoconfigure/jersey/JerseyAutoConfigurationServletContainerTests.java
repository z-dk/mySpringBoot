/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfigurationServletContainerTests.Application;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Tests that verify the behavior when deployed to a Servlet container where Jersey may
 * have already initialized itself.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@IntegrationTest("server.port=0")
@WebAppConfiguration
public class JerseyAutoConfigurationServletContainerTests {

	@ClassRule
	public static OutputCapture output = new OutputCapture();

	@Value("${local.server.port}")
	private int port;

	@Test
	public void existingJerseyServletIsAmended() {
		assertThat(output.toString(),
				containsString("Configuring existing registration for Jersey servlet"));
		assertThat(output.toString(), containsString(
				"Servlet " + Application.class.getName() + " was not registered"));
	}

	@ImportAutoConfiguration({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class, JerseyAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	@Import(ContainerConfiguration.class)
	@Path("/hello")
	public static class Application extends ResourceConfig {

		@Value("${message:World}")
		private String msg;

		public Application() {
			register(Application.class);
		}

		@GET
		public String message() {
			return "Hello " + this.msg;
		}

	}

	@Configuration
	public static class ContainerConfiguration {

		@Bean
		public TomcatEmbeddedServletContainerFactory tomcat() {
			return new TomcatEmbeddedServletContainerFactory() {

				@Override
				protected void postProcessContext(Context context) {
					Wrapper jerseyServlet = context.createWrapper();
					String servletName = Application.class.getName();
					jerseyServlet.setName(servletName);
					jerseyServlet.setServletClass(ServletContainer.class.getName());
					jerseyServlet.setServlet(new ServletContainer());
					jerseyServlet.setOverridable(false);
					context.addChild(jerseyServlet);
					context.addServletMapping("/*", servletName);
				}

			};
		}

	}

}

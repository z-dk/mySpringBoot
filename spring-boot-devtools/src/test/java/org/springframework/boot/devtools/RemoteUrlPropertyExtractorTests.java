/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools;

import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link RemoteUrlPropertyExtractor}.
 *
 * @author Phillip Webb
 */
public class RemoteUrlPropertyExtractorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void preventRunFailuresFromPollutingLoggerContext() {
		((Logger) LoggerFactory.getLogger(RemoteUrlPropertyExtractorTests.class))
				.getLoggerContext().getTurboFilterList().clear();
	}

	@Test
	public void missingUrl() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("No remote URL specified");
		doTest();
	}

	@Test
	public void malformedUrl() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Malformed URL '::://wibble'");
		doTest("::://wibble");

	}

	@Test
	public void multipleUrls() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Multiple URLs specified");
		doTest("http://localhost:8080", "http://localhost:9090");
	}

	@Test
	public void validUrl() throws Exception {
		ApplicationContext context = doTest("http://localhost:8080");
		assertThat(context.getEnvironment().getProperty("remoteUrl"),
				equalTo("http://localhost:8080"));
		assertThat(context.getEnvironment().getProperty("spring.thymeleaf.cache"),
				is(nullValue()));
	}

	@Test
	public void cleanValidUrl() throws Exception {
		ApplicationContext context = doTest("http://localhost:8080/");
		assertThat(context.getEnvironment().getProperty("remoteUrl"),
				equalTo("http://localhost:8080"));
	}

	private ApplicationContext doTest(String... args) {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.addListeners(new RemoteUrlPropertyExtractor());
		return application.run(args);
	}

	@Configuration
	static class Config {

	}

}
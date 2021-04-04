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

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.MongoHealthIndicator;
import org.springframework.boot.actuate.health.RabbitHealthIndicator;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.actuate.health.SolrHealthIndicator;
import org.springframework.boot.actuate.health.VanillaHealthIndicator;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link HealthIndicatorAutoConfiguration}.
 *
 * @author Christian Dupuis
 */
public class HealthIndicatorAutoConfigurationTests {

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
	public void defaultHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(VanillaHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void redisHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(RedisAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(RedisHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void notRedisHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(RedisAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "health.redis.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(VanillaHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void mongoHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(MongoHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void notMongoHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "health.mongo.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(VanillaHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void combinedHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MongoAutoConfiguration.class, RedisAutoConfiguration.class,
				MongoDataAutoConfiguration.class, SolrAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(3, beans.size());
	}

	@Test
	public void dataSourceHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(DataSourceHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void notDataSourceHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "health.db.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(VanillaHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void rabbitHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(RabbitAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(RabbitHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void notRabbitHealthIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(RabbitAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "health.rabbit.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(VanillaHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void solrHeathIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(SolrAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(SolrHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}

	@Test
	public void notSolrHeathIndicator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(SolrAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "health.solr.enabled:false");
		this.context.refresh();
		Map<String, HealthIndicator> beans = this.context
				.getBeansOfType(HealthIndicator.class);
		assertEquals(1, beans.size());
		assertEquals(VanillaHealthIndicator.class, beans.values().iterator().next()
				.getClass());
	}
}

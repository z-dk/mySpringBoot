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

package org.springframework.boot.autoconfigure.mongo;

import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link MongoProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class MongoPropertiesTests {

	@Test
	public void canBindCharArrayPassword() {
		// gh-1572
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "spring.data.mongodb.password:word");
		context.register(Conf.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.getPassword(), equalTo("word".toCharArray()));
	}

	@Test
	public void portCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setPort(12345);
		MongoClient client = properties.createMongoClient(null, null);
		List<ServerAddress> allAddresses = client.getAllAddress();
		assertThat(allAddresses, hasSize(1));
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	public void hostCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setHost("mongo.example.com");
		MongoClient client = properties.createMongoClient(null, null);
		List<ServerAddress> allAddresses = client.getAllAddress();
		assertThat(allAddresses, hasSize(1));
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	public void credentialsCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = properties.createMongoClient(null, null);
		assertMongoCredential(client.getCredentialsList().get(0), "user", "secret",
				"test");
	}

	@Test
	public void databaseCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = properties.createMongoClient(null, null);
		assertMongoCredential(client.getCredentialsList().get(0), "user", "secret",
				"foo");
	}

	@Test
	public void authenticationDatabaseCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setAuthenticationDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = properties.createMongoClient(null, null);
		assertMongoCredential(client.getCredentialsList().get(0), "user", "secret",
				"foo");
	}

	@Test
	public void uriCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://user:secret@mongo1.example.com:12345,"
				+ "mongo2.example.com:23456/test");
		MongoClient client = properties.createMongoClient(null, null);
		List<ServerAddress> allAddresses = client.getAllAddress();
		assertEquals(2, allAddresses.size());
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		List<MongoCredential> credentialsList = client.getCredentialsList();
		assertEquals(1, credentialsList.size());
		assertMongoCredential(credentialsList.get(0), "user", "secret", "test");
	}

	@Test
	public void allMongoClientOptionsCanBeSet() throws UnknownHostException {
		MongoClientOptions mco = MongoClientOptions.builder()
				.alwaysUseMBeans(true)
				.connectionsPerHost(101)
				.connectTimeout(10001)
				.cursorFinalizerEnabled(false)
				.description("test")
				.maxWaitTime(120001)
				.socketKeepAlive(true)
				.socketTimeout(1000)
				.threadsAllowedToBlockForConnectionMultiplier(6)
				.minConnectionsPerHost(0)
				.maxConnectionIdleTime(60000)
				.maxConnectionLifeTime(60000)
				.heartbeatFrequency(10001)
				.minHeartbeatFrequency(501)
				.heartbeatConnectTimeout(20001)
				.heartbeatSocketTimeout(20001)
				.localThreshold(20)
				.requiredReplicaSetName("testReplicaSetName")
				.build();

		MongoProperties properties = new MongoProperties();
		MongoClient client = properties.createMongoClient(mco, null);
		MongoClientOptions wrappedMco = client.getMongoClientOptions();

		assertThat(wrappedMco.isAlwaysUseMBeans(), equalTo(mco.isAlwaysUseMBeans()));
		assertThat(wrappedMco.getConnectionsPerHost(), equalTo(mco.getConnectionsPerHost()));
		assertThat(wrappedMco.getConnectTimeout(), equalTo(mco.getConnectTimeout()));
		assertThat(wrappedMco.isCursorFinalizerEnabled(), equalTo(mco.isCursorFinalizerEnabled()));
		assertThat(wrappedMco.getDescription(), equalTo(mco.getDescription()));
		assertThat(wrappedMco.getMaxWaitTime(), equalTo(mco.getMaxWaitTime()));
		assertThat(wrappedMco.getSocketTimeout(), equalTo(mco.getSocketTimeout()));
		assertThat(wrappedMco.isSocketKeepAlive(), equalTo(mco.isSocketKeepAlive()));
		assertThat(wrappedMco.getThreadsAllowedToBlockForConnectionMultiplier(), equalTo(
				mco.getThreadsAllowedToBlockForConnectionMultiplier()));
		assertThat(wrappedMco.getMinConnectionsPerHost(), equalTo(mco.getMinConnectionsPerHost()));
		assertThat(wrappedMco.getMaxConnectionIdleTime(), equalTo(mco.getMaxConnectionIdleTime()));
		assertThat(wrappedMco.getMaxConnectionLifeTime(), equalTo(mco.getMaxConnectionLifeTime()));
		assertThat(wrappedMco.getHeartbeatFrequency(), equalTo(mco.getHeartbeatFrequency()));
		assertThat(wrappedMco.getMinHeartbeatFrequency(), equalTo(mco.getMinHeartbeatFrequency()));
		assertThat(wrappedMco.getHeartbeatConnectTimeout(), equalTo(mco.getHeartbeatConnectTimeout()));
		assertThat(wrappedMco.getHeartbeatSocketTimeout(), equalTo(mco.getHeartbeatSocketTimeout()));
		assertThat(wrappedMco.getLocalThreshold(), equalTo(mco.getLocalThreshold()));
		assertThat(wrappedMco.getRequiredReplicaSetName(), equalTo(mco.getRequiredReplicaSetName()));
	}

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost,
			int expectedPort) {
		assertThat(serverAddress.getHost(), equalTo(expectedHost));
		assertThat(serverAddress.getPort(), equalTo(expectedPort));
	}

	private void assertMongoCredential(MongoCredential credentials,
			String expectedUsername, String expectedPassword, String expectedSource) {
		assertThat(credentials.getUserName(), equalTo(expectedUsername));
		assertThat(credentials.getPassword(), equalTo(expectedPassword.toCharArray()));
		assertThat(credentials.getSource(), equalTo(expectedSource));
	}

	@Configuration
	@EnableConfigurationProperties(MongoProperties.class)
	static class Conf {

	}

}

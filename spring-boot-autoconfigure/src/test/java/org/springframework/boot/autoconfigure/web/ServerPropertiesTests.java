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

package org.springframework.boot.autoconfigure.web;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServerProperties}.
 * 
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class ServerPropertiesTests {

	private final ServerProperties properties = new ServerProperties();

	@Test
	public void testAddressBinding() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(Collections.singletonMap("server.address",
				"127.0.0.1")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(InetAddress.getByName("127.0.0.1"), this.properties.getAddress());
	}

	@Test
	public void testPortBinding() throws Exception {
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				Collections.singletonMap("server.port", "9000")));
		assertEquals(9000, this.properties.getPort().intValue());
	}

	@Test
	public void testTomcatBinding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.access_log_pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.protocol_header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remote_ip_header", "Remote-Ip");
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				map));
		assertEquals("%h %t '%r' %s %b", this.properties.getTomcat()
				.getAccessLogPattern());
		assertEquals("Remote-Ip", this.properties.getTomcat().getRemoteIpHeader());
		assertEquals("X-Forwarded-Protocol", this.properties.getTomcat()
				.getProtocolHeader());
	}

	@Test
	public void testCustomizeTomcat() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(ConfigurableEmbeddedServletContainer.class);
		this.properties.customize(factory);
		verify(factory, never()).setContextPath("");
	}

	@Test
	public void testCustomizeTomcatPort() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(ConfigurableEmbeddedServletContainer.class);
		this.properties.setPort(8080);
		this.properties.customize(factory);
		verify(factory).setPort(8080);
	}

	@Test
	public void testCustomizeUriEncoding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.uriEncoding", "US-ASCII");
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				map));
		assertEquals("US-ASCII", this.properties.getTomcat().getUriEncoding());
	}

}

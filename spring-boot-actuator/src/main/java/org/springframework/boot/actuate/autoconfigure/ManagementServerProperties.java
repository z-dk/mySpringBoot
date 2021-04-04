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

import java.net.InetAddress;

import javax.validation.constraints.NotNull;

import org.springframework.boot.autoconfigure.security.SecurityPrequisite;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.util.ClassUtils;

/**
 * Properties for the management server (e.g. port and path settings).
 * 
 * @author Dave Syer
 * @see ServerProperties
 */
@ConfigurationProperties(prefix = "management", ignoreUnknownFields = false)
public class ManagementServerProperties implements SecurityPrequisite {

	private static final String SECURITY_CHECK_CLASS = "org.springframework.security.config.http.SessionCreationPolicy";

	private Integer port;

	private InetAddress address;

	@NotNull
	private String contextPath = "";

	private final Security security = maybeCreateSecurity();

	/**
	 * Returns the management port or {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used.
	 * @see #setPort(Integer)
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the management server, use {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used. To disable use 0.
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Security getSecurity() {
		return this.security;
	}

	/**
	 * Security configuration.
	 */
	public static class Security {

		private boolean enabled = true;

		private String role = "ADMIN";

		private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

		public SessionCreationPolicy getSessions() {
			return this.sessions;
		}

		public void setSessions(SessionCreationPolicy sessions) {
			this.sessions = sessions;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getRole() {
			return this.role;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	private static Security maybeCreateSecurity() {
		if (ClassUtils.isPresent(SECURITY_CHECK_CLASS, null)) {
			return new Security();
		}
		return null;
	}

}

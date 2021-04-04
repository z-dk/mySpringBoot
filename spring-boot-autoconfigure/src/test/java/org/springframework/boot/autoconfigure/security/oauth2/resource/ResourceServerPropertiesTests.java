/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceServerProperties}.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 */
public class ResourceServerPropertiesTests {

	private ResourceServerProperties properties = new ResourceServerProperties("client",
			"secret");

	@Test
	@SuppressWarnings("unchecked")
	public void json() throws Exception {
		this.properties.getJwt().setKeyUri("https://example.com/token_key");
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(this.properties);
		Map<String, Object> value = mapper.readValue(json, Map.class);
		Map<String, Object> jwt = (Map<String, Object>) value.get("jwt");
		assertThat(jwt.get("keyUri")).isNotNull();
	}

	@Test
	public void tokenKeyDerivedFromUserInfoUri() throws Exception {
		this.properties.setUserInfoUri("https://example.com/userinfo");
		assertThat(this.properties.getJwt().getKeyUri())
				.isEqualTo("https://example.com/token_key");
	}

	@Test
	public void tokenKeyDerivedFromTokenInfoUri() throws Exception {
		this.properties.setTokenInfoUri("https://example.com/check_token");
		assertThat(this.properties.getJwt().getKeyUri())
				.isEqualTo("https://example.com/token_key");
	}

}

/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.health;

import org.apache.solr.client.solrj.SolrServer;

/**
 * {@link HealthIndicator} for Apache Solr.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class SolrHealthIndicator extends AbstractHealthIndicator {

	private final SolrServer solrServer;

	public SolrHealthIndicator(SolrServer solrServer) {
		this.solrServer = solrServer;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Object status = this.solrServer.ping().getResponse().get("status");
		builder.up().withDetail("solrStatus", status);
	}

}

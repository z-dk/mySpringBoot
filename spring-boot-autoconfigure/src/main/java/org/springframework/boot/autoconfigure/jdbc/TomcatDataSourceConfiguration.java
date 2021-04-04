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

package org.springframework.boot.autoconfigure.jdbc;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for a Tomcat database pool. The Tomcat pool provides superior performance
 * and tends not to deadlock in high volume environments.
 * 
 * @author Dave Syer
 * @see DataSourceAutoConfiguration
 */
@Configuration
public class TomcatDataSourceConfiguration extends AbstractDataSourceConfiguration {

	private String jdbcInterceptors;
	private long validationInterval = 30000;
	private org.apache.tomcat.jdbc.pool.DataSource pool;

	@Bean(destroyMethod = "close")
	public DataSource dataSource() {
		this.pool = new org.apache.tomcat.jdbc.pool.DataSource();
		this.pool.setDriverClassName(getDriverClassName());
		this.pool.setUrl(getUrl());
		if (getUsername() != null) {
			this.pool.setUsername(getUsername());
		}
		if (getPassword() != null) {
			this.pool.setPassword(getPassword());
		}
		this.pool.setInitialSize(getInitialSize());
		this.pool.setMaxActive(getMaxActive());
		this.pool.setMaxIdle(getMaxIdle());
		this.pool.setMinIdle(getMinIdle());
		this.pool.setTestOnBorrow(isTestOnBorrow());
		this.pool.setTestOnReturn(isTestOnReturn());
		this.pool.setTestWhileIdle(isTestWhileIdle());
		if (getTimeBetweenEvictionRunsMillis() != null) {
			this.pool
					.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		}
		if (getMinEvictableIdleTimeMillis() != null) {
			this.pool.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		}
		this.pool.setValidationQuery(getValidationQuery());
		this.pool.setValidationInterval(this.validationInterval);
		if (getMaxWaitMillis() != null) {
			this.pool.setMaxWait(getMaxWaitMillis());
		}
		if (this.jdbcInterceptors != null) {
			this.pool.setJdbcInterceptors(this.jdbcInterceptors);
		}
		return this.pool;
	}

	@PreDestroy
	public void close() {
		if (this.pool != null) {
			this.pool.close();
		}
	}

	public void setJdbcInterceptors(String jdbcInterceptors) {
		this.jdbcInterceptors = jdbcInterceptors;
	}

	public void setValidationInterval(long validationInterval) {
		this.validationInterval = validationInterval;
	}
}

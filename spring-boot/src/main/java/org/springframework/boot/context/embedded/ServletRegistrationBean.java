/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 * <p>
 * The {@link #setServlet(Servlet) servlet} must be specified before calling
 * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
 * omitted when mapping to '/*'. The servlet name will be deduced if not specified.
 *
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addServlet(String, Servlet)
 */
public class ServletRegistrationBean extends RegistrationBean {

	private static Log logger = LogFactory.getLog(ServletRegistrationBean.class);

	private static final String[] DEFAULT_MAPPINGS = { "/*" };

	private Servlet servlet;

	private Set<String> urlMappings = new LinkedHashSet<String>();

	private int loadOnStartup = -1;

	private MultipartConfigElement multipartConfig;

	/**
	 * Create a new {@link ServletRegistrationBean} instance.
	 */
	public ServletRegistrationBean() {
	}

	/**
	 * Create a new {@link ServletRegistrationBean} instance with the specified
	 * {@link Servlet} and URL mappings.
	 * @param servlet the servlet being mapped
	 * @param urlMappings the URLs being mapped
	 */
	public ServletRegistrationBean(Servlet servlet, String... urlMappings) {
		Assert.notNull(servlet, "Servlet must not be null");
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.servlet = servlet;
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Returns the servlet being registered.
	 */
	protected Servlet getServlet() {
		return this.servlet;
	}

	/**
	 * Sets the servlet to be registered.
	 */
	public void setServlet(Servlet servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = servlet;
	}

	/**
	 * Set the URL mappings for the servlet. If not specified the mapping will default to
	 * '/'. This will replace any previously specified mappings.
	 * @param urlMappings the mappings to set
	 * @see #addUrlMappings(String...)
	 */
	public void setUrlMappings(Collection<String> urlMappings) {
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.urlMappings = new LinkedHashSet<String>(urlMappings);
	}

	/**
	 * Return a mutable collection of the URL mappings for the servlet.
	 * @return the urlMappings
	 */
	public Collection<String> getUrlMappings() {
		return this.urlMappings;
	}

	/**
	 * Add URL mappings for the servlet.
	 * @param urlMappings the mappings to add
	 * @see #setUrlMappings(Collection)
	 */
	public void addUrlMappings(String... urlMappings) {
		Assert.notNull(urlMappings, "UrlMappings must not be null");
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Sets the <code>loadOnStartup</code> priority. See
	 * {@link ServletRegistration.Dynamic#setLoadOnStartup} for details.
	 */
	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	/**
	 * Set the the {@link MultipartConfigElement multi-part configuration}.
	 * @param multipartConfig the muti-part configuration to set or {@code null}
	 */
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
		this.multipartConfig = multipartConfig;
	}

	/**
	 * Returns the {@link MultipartConfigElement multi-part configuration} to be applied
	 * or {@code null}.
	 */
	public MultipartConfigElement getMultipartConfig() {
		return this.multipartConfig;
	}

	/**
	 * Returns the servlet name that will be registered.
	 */
	public String getServletName() {
		return getOrDeduceName(this.servlet);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		Assert.notNull(this.servlet, "Servlet must not be null");
		String name = getServletName();
		if (!isEnabled()) {
			logger.info("Filter " + name + " was not registered (disabled)");
			return;
		}
		logger.info("Mapping servlet: '" + name + "' to " + this.urlMappings);
		Dynamic added = servletContext.addServlet(name, this.servlet);
		if (added == null) {
			logger.info("Servlet " + name + " was not registered "
					+ "(possibly already registered?)");
			return;
		}
		configure(added);
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 */
	protected void configure(ServletRegistration.Dynamic registration) {
		super.configure(registration);
		String[] urlMapping = this.urlMappings
				.toArray(new String[this.urlMappings.size()]);
		if (urlMapping.length == 0) {
			urlMapping = DEFAULT_MAPPINGS;
		}
		registration.addMapping(urlMapping);
		registration.setLoadOnStartup(this.loadOnStartup);
		if (this.multipartConfig != null) {
			registration.setMultipartConfig(this.multipartConfig);
		}
	}

}

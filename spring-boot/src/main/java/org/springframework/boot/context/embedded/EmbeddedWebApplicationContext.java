/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A {@link WebApplicationContext} that can be used to bootstrap itself from a contained
 * {@link EmbeddedServletContainerFactory} bean.
 * <p>
 * This context will create, initialize and run an {@link EmbeddedServletContainer} by
 * searching for a single {@link EmbeddedServletContainerFactory} bean within the
 * {@link ApplicationContext} itself. The {@link EmbeddedServletContainerFactory} is free
 * to use standard Spring concepts (such as dependency injection, lifecycle callbacks and
 * property placeholder variables).
 * <p>
 * In addition, any {@link Servlet} or {@link Filter} beans defined in the context will be
 * automatically registered with the embedded Servlet container. In the case of a single
 * Servlet bean, the '/' mapping will be used. If multiple Servlet beans are found then
 * the lowercase bean name will be used as a mapping prefix. Any Servlet named
 * 'dispatcherServlet' will always be mapped to '/'. Filter beans will be mapped to all
 * URLs ('/*').
 * <p>
 * For more advanced configuration, the context can instead define beans that implement
 * the {@link ServletContextInitializer} interface (most often
 * {@link ServletRegistrationBean}s and/or {@link FilterRegistrationBean}s). To prevent
 * double registration, the use of {@link ServletContextInitializer} beans will disable
 * automatic Servlet and Filter bean registration.
 * <p>
 * Although this context can be used directly, most developers should consider using the
 * {@link AnnotationConfigEmbeddedWebApplicationContext} or
 * {@link XmlEmbeddedWebApplicationContext} variants.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see AnnotationConfigEmbeddedWebApplicationContext
 * @see XmlEmbeddedWebApplicationContext
 * @see EmbeddedServletContainerFactory
 */
public class EmbeddedWebApplicationContext extends GenericWebApplicationContext {

	/**
	 * Constant value for the DispatcherServlet bean name. A Servlet bean with this name
	 * is deemed to be the "main" servlet and is automatically given a mapping of "/" by
	 * default. To change the default behaviour you can use a
	 * {@link ServletRegistrationBean} or a different bean name.
	 */
	public static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

	private EmbeddedServletContainer embeddedServletContainer;

	private ServletConfig servletConfig;

	private String namespace;

	/**
	 * Register ServletContextAwareProcessor.
	 * @see ServletContextAwareProcessor
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory
				.addBeanPostProcessor(new WebApplicationContextServletContextAwareProcessor(
						this));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
	}

	@Override
	public final void refresh() throws BeansException, IllegalStateException {
		try {
			super.refresh();
		}
		catch (RuntimeException ex) {
			stopAndReleaseEmbeddedServletContainer();
			throw ex;
		}
	}

	@Override
	protected void onRefresh() {
		super.onRefresh();
		try {
			createEmbeddedServletContainer();
		}
		catch (Throwable ex) {
			throw new ApplicationContextException("Unable to start embedded container",
					ex);
		}
	}

	@Override
	protected void finishRefresh() {
		super.finishRefresh();
		startEmbeddedServletContainer();
		if (this.embeddedServletContainer != null) {
			publishEvent(new EmbeddedServletContainerInitializedEvent(this,
					this.embeddedServletContainer));
		}
	}

	@Override
	protected void doClose() {
		super.doClose();
		stopAndReleaseEmbeddedServletContainer();
	}

	private synchronized void createEmbeddedServletContainer() {
		if (this.embeddedServletContainer == null && getServletContext() == null) {
			EmbeddedServletContainerFactory containerFactory = getEmbeddedServletContainerFactory();
			this.embeddedServletContainer = containerFactory
					.getEmbeddedServletContainer(getSelfInitializer());
		}
		else if (getServletContext() != null) {
			try {
				getSelfInitializer().onStartup(getServletContext());
			}
			catch (ServletException ex) {
				throw new ApplicationContextException(
						"Cannot initialize servlet context", ex);
			}
		}
		initPropertySources();
	}

	/**
	 * Returns the {@link EmbeddedServletContainerFactory} that should be used to create
	 * the embedded servlet container. By default this method searches for a suitable bean
	 * in the context itself.
	 * @return a {@link EmbeddedServletContainerFactory} (never {@code null})
	 */
	protected EmbeddedServletContainerFactory getEmbeddedServletContainerFactory() {
		// Use bean names so that we don't consider the hierarchy
		String[] beanNames = getBeanFactory().getBeanNamesForType(
				EmbeddedServletContainerFactory.class);
		if (beanNames.length == 0) {
			throw new ApplicationContextException(
					"Unable to start EmbeddedWebApplicationContext due to missing "
							+ "EmbeddedServletContainerFactory bean.");
		}
		if (beanNames.length > 1) {
			throw new ApplicationContextException(
					"Unable to start EmbeddedWebApplicationContext due to multiple "
							+ "EmbeddedServletContainerFactory beans : "
							+ StringUtils.arrayToCommaDelimitedString(beanNames));
		}
		return getBeanFactory().getBean(beanNames[0],
				EmbeddedServletContainerFactory.class);
	}

	/**
	 * Returns the {@link ServletContextInitializer} that will be used to complete the
	 * setup of this {@link WebApplicationContext}.
	 * @see #prepareEmbeddedWebApplicationContext(ServletContext)
	 */
	private ServletContextInitializer getSelfInitializer() {
		return new ServletContextInitializer() {
			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				prepareEmbeddedWebApplicationContext(servletContext);
				WebApplicationContextUtils.registerWebApplicationScopes(getBeanFactory(),
						getServletContext());
				WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(),
						getServletContext());
				for (ServletContextInitializer beans : getServletContextInitializerBeans()) {
					beans.onStartup(servletContext);
				}
			}
		};
	}

	/**
	 * Returns {@link ServletContextInitializer}s that should be used with the embedded
	 * Servlet context. By default this method will first attempt to find
	 * {@link ServletContextInitializer} beans, if none are found it will instead search
	 * for {@link Servlet} and {@link Filter} beans.
	 */
	protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {

		Set<ServletContextInitializer> initializers = new LinkedHashSet<ServletContextInitializer>();
		Set<Servlet> servletRegistrations = new LinkedHashSet<Servlet>();
		Set<Filter> filterRegistrations = new LinkedHashSet<Filter>();
		Set<EventListener> listenerRegistrations = new LinkedHashSet<EventListener>();

		for (Entry<String, ServletContextInitializer> initializerBean : getOrderedBeansOfType(ServletContextInitializer.class)) {
			ServletContextInitializer initializer = initializerBean.getValue();
			initializers.add(initializer);
			if (initializer instanceof ServletRegistrationBean) {
				ServletRegistrationBean servlet = (ServletRegistrationBean) initializer;
				servletRegistrations.add(servlet.getServlet());
			}
			if (initializer instanceof FilterRegistrationBean) {
				FilterRegistrationBean filter = (FilterRegistrationBean) initializer;
				filterRegistrations.add(filter.getFilter());
			}
			if (initializer instanceof ServletListenerRegistrationBean) {
				listenerRegistrations
						.add(((ServletListenerRegistrationBean<?>) initializer)
								.getListener());
			}
		}

		List<Entry<String, Servlet>> servletBeans = getOrderedBeansOfType(Servlet.class);
		for (Entry<String, Servlet> servletBean : servletBeans) {
			final String name = servletBean.getKey();
			Servlet servlet = servletBean.getValue();
			if (!servletRegistrations.contains(servlet)) {
				String url = (servletBeans.size() == 1 ? "/" : "/" + name + "/");
				if (name.equals(DISPATCHER_SERVLET_NAME)) {
					url = "/"; // always map the main dispatcherServlet to "/"
				}
				ServletRegistrationBean registration = new ServletRegistrationBean(
						servlet, url);
				registration.setName(name);
				registration.setMultipartConfig(getMultipartConfig());
				initializers.add(registration);
			}
		}

		for (Entry<String, Filter> filterBean : getOrderedBeansOfType(Filter.class)) {
			String name = filterBean.getKey();
			Filter filter = filterBean.getValue();
			if (!filterRegistrations.contains(filter)) {
				FilterRegistrationBean registration = new FilterRegistrationBean(filter);
				registration.setName(name);
				initializers.add(registration);
			}
		}

		Set<Class<?>> listenerTypes = ServletListenerRegistrationBean.getSupportedTypes();
		for (Class<?> type : listenerTypes) {
			for (Entry<String, ?> listenerBean : getOrderedBeansOfType(type)) {
				String name = listenerBean.getKey();
				EventListener listener = (EventListener) listenerBean.getValue();
				if (ServletListenerRegistrationBean.isSupportedType(listener)
						&& !filterRegistrations.contains(listener)) {
					ServletListenerRegistrationBean<EventListener> registration = new ServletListenerRegistrationBean<EventListener>(
							listener);
					registration.setName(name);
					initializers.add(registration);
				}
			}
		}

		return initializers;
	}

	private MultipartConfigElement getMultipartConfig() {
		List<Entry<String, MultipartConfigElement>> beans = getOrderedBeansOfType(MultipartConfigElement.class);
		if (beans.isEmpty()) {
			return null;
		}
		return beans.get(0).getValue();
	}

	/**
	 * Prepare the {@link WebApplicationContext} with the given fully loaded
	 * {@link ServletContext}. This method is usually called from
	 * {@link ServletContextInitializer#onStartup(ServletContext)} and is similar to the
	 * functionality usually provided by a {@link ContextLoaderListener}.
	 * @param servletContext the operational servlet context
	 */
	protected void prepareEmbeddedWebApplicationContext(ServletContext servletContext) {
		Object rootContext = servletContext
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (rootContext != null) {
			if (rootContext == this) {
				throw new IllegalStateException(
						"Cannot initialize context because there is already a root application context present - "
								+ "check whether you have multiple ServletContextInitializers!");
			}
			else {
				return;
			}
		}
		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring embedded WebApplicationContext");
		try {
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this);
			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name ["
						+ WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
						+ "]");
			}
			setServletContext(servletContext);
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - getStartupDate();
				logger.info("Root WebApplicationContext: initialization completed in "
						+ elapsedTime + " ms");
			}
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
		catch (Error ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
	}

	private <T> List<Entry<String, T>> getOrderedBeansOfType(Class<T> type) {
		List<Entry<String, T>> beans = new ArrayList<Entry<String, T>>();
		Comparator<Entry<String, T>> comparator = new Comparator<Entry<String, T>>() {
			@Override
			public int compare(Entry<String, T> o1, Entry<String, T> o2) {
				return AnnotationAwareOrderComparator.INSTANCE.compare(o1.getValue(),
						o2.getValue());
			}
		};
		String[] names = getBeanFactory().getBeanNamesForType(type, true, false);
		Map<String, T> map = new LinkedHashMap<String, T>();
		for (String name : names) {
			map.put(name, getBeanFactory().getBean(name, type));
		}
		beans.addAll(map.entrySet());
		Collections.sort(beans, comparator);
		return beans;
	}

	private void startEmbeddedServletContainer() {
		if (this.embeddedServletContainer != null) {
			this.embeddedServletContainer.start();
		}
	}

	private synchronized void stopAndReleaseEmbeddedServletContainer() {
		if (this.embeddedServletContainer != null) {
			try {
				this.embeddedServletContainer.stop();
				this.embeddedServletContainer = null;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	protected Resource getResourceByPath(String path) {
		if (getServletContext() == null) {
			return new ClassPathContextResource(path, getClassLoader());
		}
		return new ServletContextResource(getServletContext(), path);
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	/**
	 * Returns the {@link EmbeddedServletContainer} that was created by the context or
	 * {@code null} if the container has not yet been created.
	 */
	public EmbeddedServletContainer getEmbeddedServletContainer() {
		return this.embeddedServletContainer;
	}

}

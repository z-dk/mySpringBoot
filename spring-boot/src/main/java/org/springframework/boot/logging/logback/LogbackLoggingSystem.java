/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;

/**
 * {@link LoggingSystem} for <a href="http://logback.qos.ch">logback</a>.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class LogbackLoggingSystem extends AbstractLoggingSystem {

	private static final Map<LogLevel, Level> LEVELS;
	static {
		Map<LogLevel, Level> levels = new HashMap<LogLevel, Level>();
		levels.put(LogLevel.TRACE, Level.TRACE);
		levels.put(LogLevel.DEBUG, Level.DEBUG);
		levels.put(LogLevel.INFO, Level.INFO);
		levels.put(LogLevel.WARN, Level.WARN);
		levels.put(LogLevel.ERROR, Level.ERROR);
		levels.put(LogLevel.FATAL, Level.ERROR);
		levels.put(LogLevel.OFF, Level.OFF);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	public LogbackLoggingSystem(ClassLoader classLoader) {
		super(classLoader, "logback-test.groovy", "logback-test.xml", "logback.groovy",
				"logback.xml");
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();
		configureJdkLoggingBridgeHandler();
		configureJBossLoggingToUseSlf4j();
	}

	@Override
	public void cleanUp() {
		removeJdkLoggingBridgeHandler();
	}

	private void configureJdkLoggingBridgeHandler() {
		try {
			if (bridgeHandlerIsAvailable()) {
				removeJdkLoggingBridgeHandler();
				SLF4JBridgeHandler.install();
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	private boolean bridgeHandlerIsAvailable() {
		return ClassUtils.isPresent("org.slf4j.bridge.SLF4JBridgeHandler",
				getClassLoader());
	}

	private void configureJBossLoggingToUseSlf4j() {
		System.setProperty("org.jboss.logging.provider", "slf4j");
	}

	private void removeJdkLoggingBridgeHandler() {
		try {
			if (bridgeHandlerIsAvailable()) {
				try {
					SLF4JBridgeHandler.removeHandlersForRootLogger();
				}
				catch (NoSuchMethodError ex) {
					// Method missing in older versions of SLF4J like in JBoss AS 7.1
					SLF4JBridgeHandler.uninstall();
				}
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	@Override
	public void initialize(String configLocation) {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
		ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
		Assert.isInstanceOf(
				LoggerContext.class,
				factory,
				String.format(
						"LoggerFactory is not a Logback LoggerContext but Logback is on "
								+ "the classpath. Either remove Logback or the competing "
								+ "implementation (%s loaded from %s).",
						factory.getClass(), getLocation(factory)));

		LoggerContext context = (LoggerContext) factory;
		context.stop();
		context.reset();
		try {
			URL url = ResourceUtils.getURL(resolvedLocation);
			new ContextInitializer(context).configureByResource(url);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize logging from "
					+ configLocation, ex);
		}
	}

	private Object getLocation(ILoggerFactory factory) {
		try {
			ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource != null) {
				return codeSource.getLocation();
			}
		}
		catch (SecurityException ex) {
			// Unable to determine location
		}
		return "unknown location";
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
		Logger logger = factory
				.getLogger(StringUtils.isEmpty(loggerName) ? Logger.ROOT_LOGGER_NAME
						: loggerName);
		((ch.qos.logback.classic.Logger) logger).setLevel(LEVELS.get(level));
	}

}

/*
 * Copyright 2010-2014 the original author or authors.
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

package org.springframework.boot.actuate.system;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves application PID
 * into file. This application listener will be triggered exactly once per JVM.
 * 
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.2
 */
public class ApplicationPidListener implements
		ApplicationListener<ApplicationStartedEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(ApplicationPidListener.class);

	private static final String DEFAULT_FILE_NAME = "application.pid";

	private static final AtomicBoolean created = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE + 13;

	private final File file;

	/**
	 * Create a new {@link ApplicationPidListener} instance using the filename
	 * 'application.pid'.
	 */
	public ApplicationPidListener() {
		this.file = new File(DEFAULT_FILE_NAME);
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with a specified filename.
	 * @param filename the name of file containing pid
	 */
	public ApplicationPidListener(String filename) {
		Assert.notNull(filename, "Filename must not be null");
		this.file = new File(filename);
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with a specified file.
	 * @param file the file containing pid
	 */
	public ApplicationPidListener(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		if (created.compareAndSet(false, true)) {
			try {
				new ApplicationPid().write(this.file);
			}
			catch (Exception ex) {
				logger.warn(String.format("Cannot create pid file %s", this.file));
			}
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Reset the created flag for testing purposes.
	 */
	static void reset() {
		created.set(false);
	}
}

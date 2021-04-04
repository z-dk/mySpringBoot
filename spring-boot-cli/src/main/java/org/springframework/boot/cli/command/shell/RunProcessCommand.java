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

package org.springframework.boot.cli.command.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.util.ReflectionUtils;

/**
 * Special {@link Command} used to run a process from the shell. NOTE: this command is not
 * directly installed into the shell.
 * 
 * @author Phillip Webb
 */
class RunProcessCommand extends AbstractCommand {

	private static final Method INHERIT_IO_METHOD = ReflectionUtils.findMethod(
			ProcessBuilder.class, "inheritIO");

	private static final long JUST_ENDED_LIMIT = 500;

	private final String[] command;

	private volatile Process process;

	private volatile long endTime;

	public RunProcessCommand(String... command) {
		super(null, null);
		this.command = command;
	}

	@Override
	public void run(String... args) throws Exception {
		run(Arrays.asList(args));
	}

	protected void run(Collection<String> args) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(this.command);
		builder.command().addAll(args);
		builder.redirectErrorStream(true);
		boolean inheritedIO = inheritIO(builder);
		try {
			this.process = builder.start();
			if (!inheritedIO) {
				redirectOutput(this.process);
			}
			try {
				this.process.waitFor();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		finally {
			this.endTime = System.currentTimeMillis();
			this.process = null;
		}
	}

	private boolean inheritIO(ProcessBuilder builder) {
		try {
			INHERIT_IO_METHOD.invoke(builder);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	private void redirectOutput(Process process) {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		new Thread() {
			@Override
			public void run() {
				try {
					String line = reader.readLine();
					while (line != null) {
						System.out.println(line);
						line = reader.readLine();
					}
					reader.close();
				}
				catch (Exception ex) {
				}
			};
		}.start();
	}

	/**
	 * @return the running process or {@code null}
	 */
	public Process getRunningProcess() {
		return this.process;
	}

	/**
	 * @return {@code true} if the process was stopped.
	 */
	public boolean handleSigInt() {

		// if the process has just ended, probably due to this SIGINT, consider handled.
		if (hasJustEnded()) {
			return true;
		}

		// destroy the running process
		Process process = this.process;
		if (process != null) {
			try {
				process.destroy();
				process.waitFor();
				this.process = null;
				return true;
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		return false;
	}

	public boolean hasJustEnded() {
		return System.currentTimeMillis() < (this.endTime + JUST_ENDED_LIMIT);
	}

}

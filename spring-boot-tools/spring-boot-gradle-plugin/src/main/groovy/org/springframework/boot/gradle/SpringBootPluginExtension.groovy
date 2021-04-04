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

package org.springframework.boot.gradle

import org.springframework.boot.loader.tools.Layout
import org.springframework.boot.loader.tools.Layouts


/**
 * Gradle DSL Extension for 'Spring Boot'.  Most of the time Spring Boot can guess the
 * settings in this extension, but occasionally you might need to explicitly set one
 * or two of them. E.g.
 *
 * <pre>
 *     apply plugin: "spring-boot"
 *     springBoot {
 *         mainClass = 'org.demo.Application'
 *         layout = 'ZIP'
 *     }
 * </pre>
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class SpringBootPluginExtension {

	static enum LayoutType {

		JAR(new Layouts.Jar()),

		WAR(new Layouts.War()),

		ZIP(new Layouts.Expanded()),

		DIR(new Layouts.Expanded()),

		NONE(new Layouts.None());

		Layout layout;

		private LayoutType(Layout layout) {
			this.layout = layout;
		}
	}

	/**
	 * The main class that should be run. Instead of setting this explicitly you can use the
	 * 'mainClassName' of the project or the 'main' of the 'run' task. If not specified the
	 * value from the MANIFEST will be used, or if no manifest entry is the archive will be
	 * searched for a suitable class.
	 */
	String mainClass

	/**
	 * The classifier (file name part before the extension). Instead of setting this explicitly
	 * you can use the 'classifier' property of the 'bootRepackage' task. If not specified the archive
	 * will be replaced instead of renamed.
	 */
	String classifier

	/**
	 * The name of the ivy configuration name to treat as 'provided' (when packaging
	 * those dependencies in a separate path). If not specified 'providedRuntime' will
	 * be used.
	 */
	String providedConfiguration

	/**
	 * The name of the custom configuration to use.
	 */
	String customConfiguration

	/**
	 * If the original source archive should be backed-up before being repackaged.
	 */
	boolean backupSource = true;

	/**
	 * The layout of the archive if it can't be derived from the file extension.
	 * Valid values are JAR, WAR, ZIP, DIR (for exploded zip file). ZIP and DIR
	 * are actually synonymous, and should be used if there is no MANIFEST.MF
	 * available, or if you want the MANIFEST.MF 'Main-Class' to be
	 * PropertiesLauncher. Gradle will coerce literal String values to the
	 * correct type.
	 */
	LayoutType layout;

	/**
	 * Convenience method for use in a custom task.
	 * @return the Layout to use or null if not explicitly set
	 */
	Layout convertLayout() {
		(layout == null ? null : layout.layout)
	}

	/**
	 * Libraries that must be unpacked from fat jars in order to run. Use Strings in the
	 * form {@literal groupId:artifactId}.
	 */
	Set<String> requiresUnpack;

	/**
	 * Location of an agent jar to attach to the VM when running the application with runJar task.
	 */
	File agent;

	/**
	 * Flag to indicate that the agent requires -noverify (and the plugin will refuse to start if it is not set)
	 */
	Boolean noverify;

	/**
	 * If exclude rules should be applied to dependencies based on the spring-dependencies-bom
	 */
	boolean applyExcludeRules = true;

}

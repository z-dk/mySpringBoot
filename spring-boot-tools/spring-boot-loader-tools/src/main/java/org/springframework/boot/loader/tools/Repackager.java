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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.tools.MainClassFinder.ClassNameCallback;

/**
 * Utility class that can be used to repackage an archive so that it can be executed using
 * '{@literal java -jar}'.
 * 
 * @author Phillip Webb
 */
public class Repackager {

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	private static final String BOOT_VERSION_ATTRIBUTE = "Spring-Boot-Version";

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private String mainClass;

	private boolean backupSource = true;

	private final File source;

	private Layout layout;

	public Repackager(File source) {
		if (source == null || !source.exists() || !source.isFile()) {
			throw new IllegalArgumentException("Source must refer to an existing file");
		}
		this.source = source.getAbsoluteFile();
		this.layout = Layouts.forFile(source);
	}

	/**
	 * Sets the main class that should be run. If not specified the value from the
	 * MANIFEST will be used, or if no manifest entry is found the archive will be
	 * searched for a suitable class.
	 * @param mainClass the main class name
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Sets if source files should be backed up when they would be overwritten.
	 * @param backupSource if source files should be backed up
	 */
	public void setBackupSource(boolean backupSource) {
		this.backupSource = backupSource;
	}

	/**
	 * Sets the layout to use for the jar. Defaults to {@link Layouts#forFile(File)}.
	 * @param layout the layout
	 */
	public void setLayout(Layout layout) {
		if (layout == null) {
			throw new IllegalArgumentException("Layout must not be null");
		}
		this.layout = layout;
	}

	/**
	 * Repackage the source file so that it can be run using '{@literal java -jar}'
	 * @param libraries the libraries required to run the archive
	 * @throws IOException
	 */
	public void repackage(Libraries libraries) throws IOException {
		repackage(this.source, libraries);
	}

	/**
	 * Repackage to the given destination so that it can be launched using '
	 * {@literal java -jar}'
	 * @param destination the destination file (may be the same as the source)
	 * @param libraries the libraries required to run the archive
	 * @throws IOException
	 */
	public void repackage(File destination, Libraries libraries) throws IOException {
		if (destination == null || destination.isDirectory()) {
			throw new IllegalArgumentException("Invalid destination");
		}
		if (libraries == null) {
			throw new IllegalArgumentException("Libraries must not be null");
		}
		destination = destination.getAbsoluteFile();
		File workingSource = this.source;
		if (this.source.equals(destination)) {
			workingSource = new File(this.source.getParentFile(), this.source.getName()
					+ ".original");
			workingSource.delete();
			renameFile(this.source, workingSource);
		}
		destination.delete();
		try {
			JarFile jarFileSource = new JarFile(workingSource);
			try {
				repackage(jarFileSource, destination, libraries);
			}
			finally {
				jarFileSource.close();
			}
		}
		finally {
			if (!this.backupSource && !this.source.equals(workingSource)) {
				deleteFile(workingSource);
			}
		}
	}

	private void repackage(JarFile sourceJar, File destination, Libraries libraries)
			throws IOException {
		final JarWriter writer = new JarWriter(destination);
		try {
			writer.writeManifest(buildManifest(sourceJar));
			writer.writeEntries(sourceJar);

			libraries.doWithLibraries(new LibraryCallback() {
				@Override
				public void library(File file, LibraryScope scope) throws IOException {
					if (isZip(file)) {
						String destination = Repackager.this.layout
								.getLibraryDestination(file.getName(), scope);
						if (destination != null) {
							writer.writeNestedLibrary(destination, file);
						}
					}
				}
			});

			if (!(this.layout instanceof Layouts.None)) {
				writer.writeLoaderClasses();
			}
		}
		finally {
			try {
				writer.close();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	private boolean isZip(File file) {
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			try {
				return isZip(fileInputStream);
			}
			finally {
				fileInputStream.close();
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isZip(InputStream inputStream) throws IOException {
		for (int i = 0; i < ZIP_FILE_HEADER.length; i++) {
			if (inputStream.read() != ZIP_FILE_HEADER[i]) {
				return false;
			}
		}
		return true;
	}

	private Manifest buildManifest(JarFile source) throws IOException {
		Manifest manifest = source.getManifest();
		if (manifest == null) {
			manifest = new Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		}
		manifest = new Manifest(manifest);
		String startClass = this.mainClass;
		if (startClass == null) {
			startClass = manifest.getMainAttributes().getValue(MAIN_CLASS_ATTRIBUTE);
		}
		if (startClass == null) {
			startClass = findMainMethod(source);
		}
		String launcherClassName = this.layout.getLauncherClassName();
		if (launcherClassName != null) {
			manifest.getMainAttributes()
					.putValue(MAIN_CLASS_ATTRIBUTE, launcherClassName);
			if (startClass == null) {
				throw new IllegalStateException("Unable to find main class");
			}
			manifest.getMainAttributes().putValue(START_CLASS_ATTRIBUTE, startClass);
		}
		else if (startClass != null) {
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE, startClass);
		}

		String bootVersion = getClass().getPackage().getImplementationVersion();
		manifest.getMainAttributes().putValue(BOOT_VERSION_ATTRIBUTE, bootVersion);

		return manifest;
	}

	protected String findMainMethod(JarFile source) throws IOException {
		MainClassesCallback callback = new MainClassesCallback();
		MainClassFinder.doWithMainClasses(source, this.layout.getClassesLocation(),
				callback);
		return callback.getMainClass();
	}

	private void renameFile(File file, File dest) {
		if (!file.renameTo(dest)) {
			throw new IllegalStateException("Unable to rename '" + file + "' to '" + dest
					+ "'");
		}
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			throw new IllegalStateException("Unable to delete '" + file + "'");
		}
	}

	private static class MainClassesCallback implements ClassNameCallback<Object> {

		private final List<String> classNames = new ArrayList<String>();

		@Override
		public Object doWith(String className) {
			this.classNames.add(className);
			return null;
		}

		public String getMainClass() {
			if (this.classNames.size() > 1) {
				throw new IllegalStateException(
						"Unable to find a single main class from the following candidates "
								+ this.classNames);
			}
			return this.classNames.isEmpty() ? null : this.classNames.get(0);
		}

	}
}

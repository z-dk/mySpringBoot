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

package org.springframework.boot.loader.jar;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Manifest;

import org.springframework.boot.loader.util.AsciiBytes;

/**
 * {@link java.net.JarURLConnection} used to support {@link JarFile#getUrl()}.
 * 
 * @author Phillip Webb
 */
class JarURLConnection extends java.net.JarURLConnection {

	static final String PROTOCOL = "jar";

	static final String SEPARATOR = "!/";

	private static final String PREFIX = PROTOCOL + ":" + "file:";

	private final JarFile jarFile;

	private JarEntryData jarEntryData;

	private String jarEntryName;

	private String contentType;

	private URL jarFileUrl;

	protected JarURLConnection(URL url, JarFile jarFile) throws MalformedURLException {
		super(new URL(buildRootUrl(jarFile)));
		this.url = url;
		this.jarFile = jarFile;

		String spec = url.getFile();
		int separator = spec.lastIndexOf(SEPARATOR);
		if (separator == -1) {
			throw new MalformedURLException("no " + SEPARATOR + " found in url spec:"
					+ spec);
		}
		if (separator + 2 != spec.length()) {
			this.jarEntryName = decode(spec.substring(separator + 2));
		}

		String container = spec.substring(0, separator);
		if (container.indexOf(SEPARATOR) == -1) {
			this.jarFileUrl = new URL(container);
		}
		else {
			this.jarFileUrl = new URL("jar:" + container);
		}
	}

	@Override
	public void connect() throws IOException {
		if (this.jarEntryName != null) {
			this.jarEntryData = this.jarFile.getJarEntryData(this.jarEntryName);
			if (this.jarEntryData == null) {
				throw new FileNotFoundException("JAR entry " + this.jarEntryName
						+ " not found in " + this.jarFile.getName());
			}
		}
		this.connected = true;
	}

	@Override
	public Manifest getManifest() throws IOException {
		try {
			return super.getManifest();
		}
		finally {
			this.connected = false;
		}
	}

	@Override
	public JarFile getJarFile() throws IOException {
		connect();
		return this.jarFile;
	}

	@Override
	public URL getJarFileURL() {
		return this.jarFileUrl;
	}

	@Override
	public JarEntry getJarEntry() throws IOException {
		connect();
		return (this.jarEntryData == null ? null : this.jarEntryData.asJarEntry());
	}

	@Override
	public String getEntryName() {
		return this.jarEntryName;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		connect();
		if (this.jarEntryName == null) {
			throw new IOException("no entry name specified");
		}
		return this.jarEntryData.getInputStream();
	}

	@Override
	public int getContentLength() {
		try {
			connect();
			return this.jarEntryData == null ? this.jarFile.size() : this.jarEntryData
					.getSize();
		}
		catch (IOException ex) {
			return -1;
		}
	}

	@Override
	public Object getContent() throws IOException {
		connect();
		return (this.jarEntryData == null ? this.jarFile : super.getContent());
	}

	@Override
	public String getContentType() {
		if (this.contentType == null) {
			// Guess the content type, don't bother with steams as mark is not
			// supported
			this.contentType = (this.jarEntryName == null ? "x-java/jar" : null);
			this.contentType = (this.contentType == null ? guessContentTypeFromName(this.jarEntryName)
					: this.contentType);
			this.contentType = (this.contentType == null ? "content/unknown"
					: this.contentType);
		}
		return this.contentType;
	}

	private static String buildRootUrl(JarFile jarFile) {
		String path = jarFile.getRootJarFile().getFile().getPath();
		StringBuilder builder = new StringBuilder(PREFIX.length() + path.length()
				+ SEPARATOR.length());
		builder.append(PREFIX);
		builder.append(path);
		builder.append(SEPARATOR);
		return builder.toString();
	}

	private static String decode(String source) {
		int length = source.length();
		if ((length == 0) || (source.indexOf('%') < 0)) {
			return source;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if ((i + 2) >= length) {
					throw new IllegalArgumentException("Invalid encoded sequence \""
							+ source.substring(i) + "\"");
				}
				ch = decodeEscapeSequence(source, i);
				i += 2;
			}
			bos.write(ch);
		}
		// AsciiBytes is what is used to store the JarEntries so make it symmetric
		return new AsciiBytes(bos.toByteArray()).toString();

	}

	private static char decodeEscapeSequence(String source, int i) {
		int hi = Character.digit(source.charAt(i + 1), 16);
		int lo = Character.digit(source.charAt(i + 2), 16);
		if (hi == -1 || lo == -1) {
			throw new IllegalArgumentException("Invalid encoded sequence \""
					+ source.substring(i) + "\"");
		}
		return ((char) ((hi << 4) + lo));
	}
}

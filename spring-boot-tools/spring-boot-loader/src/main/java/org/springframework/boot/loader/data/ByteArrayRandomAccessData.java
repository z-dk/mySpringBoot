/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * {@link RandomAccessData} implementation backed by a byte array.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class ByteArrayRandomAccessData implements RandomAccessData {

	private final byte[] bytes;

	private final long offset;

	private final long length;

	public ByteArrayRandomAccessData(byte[] bytes) {
		this(bytes, 0, (bytes != null) ? bytes.length : 0);
	}

	public ByteArrayRandomAccessData(byte[] bytes, long offset, long length) {
		this.bytes = (bytes != null) ? bytes : new byte[0];
		this.offset = offset;
		this.length = length;
	}

	@Override
	public InputStream getInputStream(ResourceAccess access) {
		return new ByteArrayInputStream(this.bytes, (int) this.offset, (int) this.length);
	}

	@Override
	public RandomAccessData getSubsection(long offset, long length) {
		return new ByteArrayRandomAccessData(this.bytes, this.offset + offset, length);
	}

	@Override
	public long getSize() {
		return this.length;
	}

}

package org.janelia.saalfeldlab.n5.universe.factory.options;

import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Options;

/**
 * Configuration for the plain HTTP backend. Pure data, like
 * {@link AmazonS3Options} — but there is no matching provider interface,
 * because {@code HttpKeyValueRoot} is an ordinary n5 type that
 * {@code KeyValueRoots} can build itself without any optional
 * dependency.
 * <p>
 * The timeouts also bound the single HTTP request used to tell an
 * S3-compatible endpoint from an ordinary HTTP server.
 */
public class HttpOptions {

	private int connectionTimeoutMillis = 5000;

	private int readTimeoutMillis = 5000;

	public HttpOptions connectionTimeoutMillis(final int connectionTimeoutMillis) {

		this.connectionTimeoutMillis = connectionTimeoutMillis;
		return this;
	}

	public int connectionTimeoutMillis() {

		return connectionTimeoutMillis;
	}

	public HttpOptions readTimeoutMillis(final int readTimeoutMillis) {

		this.readTimeoutMillis = readTimeoutMillis;
		return this;
	}

	public int readTimeoutMillis() {

		return readTimeoutMillis;
	}

}

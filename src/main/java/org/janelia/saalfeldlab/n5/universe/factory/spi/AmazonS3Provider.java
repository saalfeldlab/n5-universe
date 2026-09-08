package org.janelia.saalfeldlab.n5.universe.factory.spi;

import org.janelia.saalfeldlab.n5.KeyValueRoot;

import java.net.URI;

/**
 * The Amazon S3 backend: knows how to create a {@link KeyValueRoot}
 * given a URI and {@link AmazonS3Options}.
 * <p>
 * This is the service interface of the {@code n5-aws-s3} module.
 */
public interface AmazonS3Provider {

	/**
	 * A handle to one S3 location. The S3 client is built lazily inside
	 * the handle. It is only built once and re-used so that it doesn't
	 * have to be re-created after probing an HTTP URI.
	 *
	 * @param uri the container URI
	 * @param options the backend configuration
	 * @return the handle
	 */
	Endpoint endpoint(URI uri, AmazonS3Options options);

	/**
	 * A handle to the S3 location at a specific URI. Created by {@link #endpoint}.
	 * <p>
	 * An implementation builds its S3 client at most once and both
	 * {@link #isS3()} and {@link #createKeyValueRoot} use it, so
	 * probing an endpoint and then opening a container on it does not
	 * build two clients.
	 * <p>
	 * Use once and discard. Deliberately <em>not</em> {@code AutoCloseable}:
	 * {@link #createKeyValueRoot} hands the client to the returned
	 * {@link KeyValueRoot}, which outlives this handle.
	 * <p>
	 * TODO: where *should* the S3Client be closed?
	 */
	interface Endpoint {

		/**
		 * Whether this location is served by an S3-compatible endpoint,
		 * determined by asking an S3 client. This is the expensive check:
		 * building the client resolves the credential chain, and the request
		 * may be retried before it fails. {@code KeyValueRoots} only
		 * calls it when {@code BackendOptions.probeHttpWithS3Client()} is set.
		 *
		 * @return true if the endpoint appears to speak S3
		 */
		boolean isS3();

		/**
		 * Create a {@link KeyValueRoot} for this location.
		 *
		 * @param readOnly whether the container is read-only
		 * @return the root
		 */
		KeyValueRoot createKeyValueRoot(boolean readOnly);
	}
}

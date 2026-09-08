package org.janelia.saalfeldlab.n5.universe.factory.spi;

import org.janelia.saalfeldlab.n5.KeyValueRoot;

import java.net.URI;

/**
 * The Google Cloud Storage backend: knows how to create a {@link KeyValueRoot}
 * given a URI and {@link GoogleCloudOptions}.
 * <p>
 * This is the service interface of the {@code n5-google-cloud} module.
 */
public interface GoogleCloudProvider {

	/**
	 * Create a {@link KeyValueRoot} for the given {@code uri}.
	 *
	 * @param uri the container URI
	 * @param readOnly whether the container is read-only
	 * @param options the backend configuration
	 * @return the root
	 */
	KeyValueRoot createKeyValueRoot(URI uri, boolean readOnly, GoogleCloudOptions options);
}

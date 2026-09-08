package org.janelia.saalfeldlab.n5.universe.kvr.split;

import com.google.cloud.storage.Storage;
import java.net.URI;
import java.util.function.Supplier;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudUtils;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueRoot;
import org.janelia.saalfeldlab.n5.universe.factory.spi.GoogleCloudOptions;
import org.janelia.saalfeldlab.n5.universe.factory.spi.GoogleCloudProvider;

/**
 * Default {@link GoogleCloudProvider}, backed by {@code n5-google-cloud} and
 * the Google Cloud SDK.
 * <p>
 * Stateless: everything it needs comes from the {@link GoogleCloudOptions}
 * passed in.
 */
public class DefaultGoogleCloudProvider implements GoogleCloudProvider {

	@Override
	public KeyValueRoot createKeyValueRoot(final URI uri, final boolean readOnly, final GoogleCloudOptions options) {

		return new GoogleCloudStorageKeyValueRoot(createClient(options), new GoogleCloudStorageURI(uri), !readOnly);
	}

	private Storage createClient(final GoogleCloudOptions settings) {

		final Supplier<Storage> clientProvider = settings.clientProvider();
		if (clientProvider != null)
			return clientProvider.get();

		return GoogleCloudUtils.createGoogleCloudStorage(settings.projectId(), settings.builderConfig());
	}
}

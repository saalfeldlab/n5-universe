package org.janelia.saalfeldlab.n5.universe.factory.spi;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Configuration for the Google Cloud Storage backend. Pure data; building the
 * client and the root is {@link GoogleCloudProvider}'s job. Like
 * {@link AmazonS3Options}, it names SDK types only as generic parameters, so
 * it stays in {@code n5-universe-core} after the split (see CLAUDE.md).
 */
public class GoogleCloudOptions {

	private String projectId = null;

	private Consumer<StorageOptions.Builder> builderConfig = null;

	private Supplier<Storage> clientProvider = null;

	/**
	 * The Google Cloud project id to build the {@link Storage} client with. If
	 * {@code null} (the default), the default project id is used.
	 *
	 * @param projectId the google cloud project id
	 * @return this settings object
	 */
	public GoogleCloudOptions projectId(final String projectId) {

		this.projectId = projectId;
		return this;
	}

	public String projectId() {

		return projectId;
	}

	/**
	 * Use {@code builderConfig} to configure the
	 * {@link StorageOptions.Builder}, and thereby the {@link Storage} client.
	 *
	 * @param builderConfig configures the google cloud storage options builder
	 * @return this settings object
	 */
	public GoogleCloudOptions builderConfig(final Consumer<StorageOptions.Builder> builderConfig) {

		this.builderConfig = builderConfig;
		return this;
	}

	public Consumer<StorageOptions.Builder> builderConfig() {

		return builderConfig;
	}

	/**
	 * Build {@link Storage} clients with {@code clientProvider} instead of with
	 * the default mechanism. This replaces client creation entirely, so
	 * {@link #projectId} and {@link #builderConfig} are then ignored.
	 *
	 * @param clientProvider builds a client
	 * @return this settings object
	 */
	public GoogleCloudOptions clientProvider(final Supplier<Storage> clientProvider) {

		this.clientProvider = clientProvider;
		return this;
	}

	public Supplier<Storage> clientProvider() {

		return clientProvider;
	}
}

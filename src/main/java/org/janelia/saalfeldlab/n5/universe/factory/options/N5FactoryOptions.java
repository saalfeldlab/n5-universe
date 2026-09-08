package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.cloud.storage.StorageOptions;
import com.google.gson.GsonBuilder;

import java.util.function.Consumer;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Options;
import org.janelia.saalfeldlab.n5.universe.factory.spi.GoogleCloudOptions;
import org.janelia.saalfeldlab.n5.universe.factory.spi.HDF5Options;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Everything the factory can be configured with, split along the two axes the
 * rest of the design rests on: {@link #format()} is what the data looks like,
 * {@link #backend()} is how the bytes are fetched. They are independent, and
 * each half is handed only to the code that needs it —
 * {@code KeyValueRoots} takes a {@link BackendOptions} and never sees
 * format options.
 * <p>
 * Replaces {@code org.janelia.saalfeldlab.n5.universe.factory.options.N5FactoryOptions}.
 * The flat {@code n5}/{@code zarr2}/{@code zarr3}/{@code hdf5}/
 * {@code cacheAttributes}/{@code gsonBuilder} methods below are kept for source
 * compatibility with it — downstream code calls them, e.g.
 * {@code options { opts -> opts.zarr2 { … }; opts.gsonBuilder(…) } }. There are
 * deliberately no such shortcuts for the backend half: that API is new, so
 * nothing depends on a flat spelling of it, and {@link #backend()} is the one
 * way in.
 */
public class N5FactoryOptions {

	private final FormatOptions format = new FormatOptions();
	private final BackendOptions backend = new BackendOptions();

	/**
	 * @return options describing what the data looks like
	 */
	public FormatOptions format() {

		return format;
	}

	/**
	 * @return options describing how the bytes are fetched
	 */
	public BackendOptions backend() {

		return backend;
	}

	public N5FactoryOptions format(final Consumer<FormatOptions> configure) {

		configure.accept(format);
		return this;
	}

	public N5FactoryOptions backend(final Consumer<BackendOptions> configure) {

		configure.accept(backend);
		return this;
	}

	// ------------------------------------------------------------------------
	//
	// -- flat view of the format options, for source compatibility --
	//

	public N5FactoryOptions n5(final Consumer<N5Options> configure) {

		format.n5(configure);
		return this;
	}

	public N5FactoryOptions hdf5(final Consumer<HDF5Options> configure) {

		format.hdf5(configure);
		return this;
	}

	public N5FactoryOptions zarr2(final Consumer<Zarr2Options> configure) {

		format.zarr2(configure);
		return this;
	}

	public N5FactoryOptions zarr3(final Consumer<Zarr3Options> configure) {

		format.zarr3(configure);
		return this;
	}

	/**
	 * Set the attribute caching behaviour for every format that was not
	 * configured individually.
	 *
	 * @param cacheAttributes flag to use for otherwise unconfigured formats
	 * @return this
	 */
	public N5FactoryOptions cacheAttributes(final boolean cacheAttributes) {

		format.cacheAttributes(cacheAttributes);
		return this;
	}

	/**
	 * Set the {@link GsonBuilder} for every format that was not configured
	 * individually.
	 *
	 * @param gsonBuilder builder to use for otherwise unconfigured formats
	 * @return this
	 */
	public N5FactoryOptions gsonBuilder(final GsonBuilder gsonBuilder) {

		format.gsonBuilder(gsonBuilder);
		return this;
	}

	// ------------------------------------------------------------------------
	//
	// -- flat view of the backend options, for source compatibility --
	//

	public N5FactoryOptions s3(final Consumer<AmazonS3Options> configure) {

		backend.s3(configure);
		return this;
	}

	public N5FactoryOptions s3Configuration(final Consumer<S3ClientBuilder> builderConfig) {

		backend.s3().builderConfig(builderConfig);
		return this;
	}

	public N5FactoryOptions googleCloud(final Consumer<GoogleCloudOptions> configure) {

		backend.googleCloud(configure);
		return this;
	}

	public N5FactoryOptions googleCloudProjectId(final String projectId) {

		backend.googleCloud().projectId(projectId);
		return this;
	}

	public N5FactoryOptions googleCloudConfiguration(final Consumer<StorageOptions.Builder> builderConfig) {

		backend.googleCloud().builderConfig(builderConfig);
		return this;
	}

	public N5FactoryOptions http(final Consumer<HttpOptions> configure) {

		backend.http(configure);
		return this;
	}
}

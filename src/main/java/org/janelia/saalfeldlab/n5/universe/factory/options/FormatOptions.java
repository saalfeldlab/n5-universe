package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.gson.GsonBuilder;

import java.util.function.Consumer;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.factory.spi.HDF5Options;

/**
 * What the data looks like: one options object per storage format.
 * <p>
 * This half of {@link N5FactoryOptions} owns the fallback chain —
 * {@link #cacheAttributes} and {@link #gsonBuilder} set values that a format
 * uses unless it was configured individually. That is a format-axis concept
 * only, which is why it lives here rather than on {@link N5FactoryOptions}.
 */
public class FormatOptions {

	private final FallbackN5Options fallback = new FallbackN5Options();

	private final N5Options n5Options = new N5Options(fallback);
	private final HDF5Options hdf5Options = new HDF5Options(fallback::gsonBuilder);
	private final Zarr2Options zarr2Options = new Zarr2Options(fallback);
	private final Zarr3Options zarr3Options = new Zarr3Options(fallback);

	private StorageFormat preferredStorageFormat = null;


	/**
	 * Configure the options used when the factory accesses an N5 container.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public FormatOptions n5(final Consumer<N5Options> configure) {

		configure.accept(n5Options);
		return this;
	}

	public N5Options n5() {

		return n5Options;
	}

	/**
	 * Configure the options used when the factory accesses an HDF5 container.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public FormatOptions hdf5(final Consumer<HDF5Options> configure) {

		configure.accept(hdf5Options);
		return this;
	}

	public HDF5Options hdf5() {

		return hdf5Options;
	}

	/**
	 * Configure the options used when the factory accesses a Zarr v2 container.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public FormatOptions zarr2(final Consumer<Zarr2Options> configure) {

		configure.accept(zarr2Options);
		return this;
	}

	public Zarr2Options zarr2() {

		return zarr2Options;
	}

	/**
	 * Configure the options used when the factory accesses a Zarr v3 container.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public FormatOptions zarr3(final Consumer<Zarr3Options> configure) {

		configure.accept(zarr3Options);
		return this;
	}

	public Zarr3Options zarr3() {

		return zarr3Options;
	}

	/**
	 * Set the attribute caching behaviour for every format that was not
	 * configured individually.
	 *
	 * @param cacheAttributes flag to use for otherwise unconfigured formats
	 * @return this
	 */
	public FormatOptions cacheAttributes(final boolean cacheAttributes) {

		fallback.cacheAttributes(cacheAttributes);
		return this;
	}

	/**
	 * Set the {@link GsonBuilder} for every format that was not configured
	 * individually.
	 *
	 * @param gsonBuilder builder to use for otherwise unconfigured formats
	 * @return this
	 */
	public FormatOptions gsonBuilder(final GsonBuilder gsonBuilder) {

		fallback.gsonBuilder(gsonBuilder);
		return this;
	}

	/**
	 * When N5Factory cannot determine the StorageFormat from the URI, it will attempt to
	 * try and create a reader/writer with all known StorageFormat values.
	 * The order of th StorageFormat will dictate which will formats will be attempted first.
	 * Some URI's may be valid for multiple StorageFormat types. (e.g. N5 and Zarr Writer
	 * at a new location). This format will be tried first if specified.
	 *
	 * @param format the storage format to try first.
	 * @return this
	 */
	public FormatOptions preferredStorageFormat(final StorageFormat format) {

		this.preferredStorageFormat = format;
		return this;
	}

	/**
	 * Every {@link StorageFormat} in the order they are tried when the format is
	 * not known, {@link #preferredStorageFormat(StorageFormat) preferred} first.
	 *
	 * @return the formats to try, in order
	 */
	public StorageFormat[] orderedStorageFormats() {

		final StorageFormat[] formats = StorageFormat.values();
		if (preferredStorageFormat == null)
			return formats;

		// NB: StorageFormat.values() is a new array, so modifying is safe.
		// System.arrayCopy handles overlapping range correctly.
		System.arraycopy(formats, 0, formats, 1, preferredStorageFormat.ordinal());
		formats[0] = preferredStorageFormat;
		return formats;
	}
}

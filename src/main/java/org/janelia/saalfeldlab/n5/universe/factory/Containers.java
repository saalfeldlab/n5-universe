package org.janelia.saalfeldlab.n5.universe.factory;

import java.net.URI;
import javax.annotation.Nullable;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.factory.spi.HDF5Options;
import org.janelia.saalfeldlab.n5.universe.factory.options.FormatOptions;
import org.janelia.saalfeldlab.n5.universe.factory.options.N5Options;
import org.janelia.saalfeldlab.n5.universe.factory.options.Zarr2Options;
import org.janelia.saalfeldlab.n5.universe.factory.options.Zarr3Options;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;

/**
 * To open a reader or writer, {@link N5Factory} needs to choose a
 * <em>backend</em> and a container <em>format</em>.
 * <p>
 * {@link Containers} handles the <em>foramt</em> axis: Read a N5 container
 * at a given {@link KeyValueRoot} with the appropriate {@link StorageFormat}.
 * This is a package-private helper class, split out for clarity, and only
 * used by {@link N5Factory}.
 * <p>
 * {@link #openReader} and {@link #openWriter} are the entry points. They both
 * go through the following helpers:
 * <ul>
 * <li>{@link #inferFormat} identifies the format of an existing container,
 *     from an explicit storage-scheme prefix on the URI, else by probing keys,
 *     else from the path suffix.</li>
 * <li>{@link #buildReader}/{@link #buildWriter} builds for a format that is
 *     decided. ({@link StorageFormat#ZARR} is only half decided and is refined
 *     to V2 or V3 by probing the container.)</li>
 * <li>{@link #openAnyFormatReader}/{@link #openAnyFormatWriter} — the last
 *     resort when nothing identified a format: try every format
 *     ({@link FormatOptions#preferredStorageFormat(StorageFormat)
 *     preferredStorageFormat} first) and use whatever works.</li>
 * </ul>
 * <p>
 * NB: The other axis (<em>backend</em>) is handled by {@link KeyValueRoots}:
 * {@code KeyValueRoots} turns a URI into a {@code KeyValueRoot}, and
 * {@code Containers} turns that root into a container.
 * <p>
 * HDF5 is the one format that does not fit the axis split nicely. It is
 * not served through a {@code KeyValueRoot} at all. Basically, a {@code
 * FileSystemKeyValueRoot} is used as a wrapper to pass the URI of
 * the single {@code .h5} file into {@code Containers}.
 */
final class Containers {

	private Containers() {}

	/**
	 * Open an {@link N5Reader} at {@code root} in the given {@code format}, or,
	 * if {@code format} is {@code null}, in whichever format the container
	 * turns out to be in.
	 * <p>
	 * Why the additional {@code uri} argument? It is the
	 * {@link KeyValueRoot#uri() uri} of {@code root}, but possibly with an
	 * additional storage-scheme prefix, which is what makes format inference
	 * authoritative when there is one.
	 *
	 * @param root root of the container
	 * @param format format of the container, or {@code null} to infer it
	 * @param uri root URI, possibly with an additional storage-scheme prefix
	 * @param options the per-format configuration
	 * @return the N5Reader
	 * @throws N5Exception if the container could not be opened
	 */
	static N5Reader openReader(
			final KeyValueRoot root,
			@Nullable StorageFormat format,
			final URI uri,
			final FormatOptions options) {

		if (format == null)
			format = inferFormat(root, uri);

		return format == null
				? openAnyFormatReader(root, options)
				: buildReader(root, format, options);
	}

	/**
	 * Open an {@link N5Writer} at {@code root} in the given {@code format}, or,
	 * if {@code format} is {@code null}, in whichever format the container
	 * turns out to be in — creating it if it is not there at all.
	 * <p>
	 * Why the additional {@code uri} argument? It is the
	 * {@link KeyValueRoot#uri() uri} of {@code root}, but possibly with an
	 * additional storage-scheme prefix, which is what makes format inference
	 * authoritative when there is one.
	 *
	 * @param root root of the container
	 * @param format format of the container, or {@code null} to infer it
	 * @param uri root URI, possibly with an additional storage-scheme prefix
	 * @param options the per-format configuration
	 * @return the N5Writer
	 * @throws N5Exception if the container could not be opened
	 */
	static N5Writer openWriter(
			final KeyValueRoot root,
			@Nullable StorageFormat format,
			final URI uri,
			final FormatOptions options) {

		if (format == null)
			format = inferFormat(root, uri);

		return format == null
				? openAnyFormatWriter(root, options)
				: buildWriter(root, format, options);
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ infer format                                                         │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Infer the {@link StorageFormat} of the container at {@code root}: from an
	 * explicit storage-scheme prefix on {@code uri}, else by probing the keys at
	 * {@code root}, else from the path suffix of {@code uri}.
	 *
	 * @param root root of the container
	 * @param uri root URI, possibly with an additional storage-scheme prefix
	 * @return the format, or {@code null} if nothing identified one
	 */
	@Nullable
	private static StorageFormat inferFormat(final KeyValueRoot root, final URI uri) {

		final InferredStorageFormat inferred = InferredStorageFormat.of(uri);
		return inferred.isExplicit()
				? inferred.format()
				: inferred.resolveFromKeys(root).format();
	}

	/**
	 * Last resort when nothing identified the format: try every
	 * {@link StorageFormat} in {@link FormatOptions#orderedStorageFormats()}
	 * order and return the first reader that opens.
	 *
	 * @param root root of the container
	 * @param options the per-format configuration
	 * @return the N5Reader
	 * @throws N5Exception if no format could be opened
	 */
	private static N5Reader openAnyFormatReader(final KeyValueRoot root, final FormatOptions options) {

		for (final StorageFormat format : options.orderedStorageFormats()) {
			try {
				return buildReader(root, format, options);
			} catch (final Throwable ignored) {
			}
		}
		throw new N5Exception("Unable to open " + root.uri() + " as N5Reader");
	}

	/**
	 * Last resort when nothing identified the format: try every
	 * {@link StorageFormat} in {@link FormatOptions#orderedStorageFormats()}
	 * order and return the first writer that opens.
	 * <p>
	 * NB: opening a writer creates the container, so the first format that works
	 * is the format the new container gets. That is what
	 * {@link FormatOptions#preferredStorageFormat(StorageFormat)} is for.
	 *
	 * @param root root of the container
	 * @param options the per-format configuration
	 * @return the N5Writer
	 * @throws N5Exception if no format could be opened
	 */
	private static N5Writer openAnyFormatWriter(final KeyValueRoot root, final FormatOptions options) {

		for (final StorageFormat format : options.orderedStorageFormats()) {
			try {
				return buildWriter(root, format, options);
			} catch (final Throwable ignored) {
			}
		}
		throw new N5Exception("Unable to open " + root.uri() + " as N5Writer");
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ dispatch on known format                                             │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Build the writer for {@code format}, taking its configuration from
	 * {@code options}.
	 * <p>
	 * {@link StorageFormat#ZARR} means "whichever version is there": an
	 * existing container is opened at its own version, and only when there is
	 * none is a new one created, at the newest version.
	 *
	 * @param root the container
	 * @param format which format to build; must not be null
	 * @param options the per-format configuration
	 * @return the writer
	 */
	private static N5Writer buildWriter(final KeyValueRoot root, final StorageFormat format, final FormatOptions options) {

		switch (format) {
		case N5:
			return buildN5Writer(root, options.n5());
		case ZARR2:
			return buildZarr2Writer(root, options.zarr2());
		case ZARR3:
			return buildZarr3Writer(root, options.zarr3());
		case HDF5:
			return buildHdf5Writer(root, options.hdf5());
		case ZARR:
			return openAnyZarrVersionWriter(root, options);
		}
		throw new N5Exception("Unsupported storage format: " + format);
	}

	/**
	 * Build the reader for {@code format}, taking its configuration from
	 * {@code options}.
	 * <p>
	 * {@link StorageFormat#ZARR} means "whichever version is there", newest
	 * first.
	 *
	 * @param root the container
	 * @param format which format to build; must not be null
	 * @param options the per-format configuration
	 * @return the reader
	 */
	private static N5Reader buildReader(final KeyValueRoot root, final StorageFormat format, final FormatOptions options) {

		switch (format) {
		case N5:
			return buildN5Reader(root, options.n5());
		case ZARR2:
			return buildZarr2Reader(root, options.zarr2());
		case ZARR3:
			return buildZarr3Reader(root, options.zarr3());
		case HDF5:
			return buildHdf5Reader(root, options.hdf5());
		case ZARR:
			return openAnyZarrVersionReader(root, options);
		}
		throw new N5Exception("Unsupported storage format: " + format);
	}


	// ZARR is a pseudo-format: it does not say which version, so that
	// must be resolved against the container. Zarr versions are tried
	// in this order. This is meaningful for writing a new container.
	// There the first one (ZARR3) will work.
	private static final StorageFormat[] ZARR_VERSIONS = {StorageFormat.ZARR3, StorageFormat.ZARR2};

	private static N5Reader openAnyZarrVersionReader(final KeyValueRoot root, final FormatOptions options) {

		final StorageFormat probed = inferZarrVersion(root);
		if (probed != null) {
			try {
				return buildReader(root, probed, options);
			} catch (final Exception ignored) {
				// the keys are there but it does not open as that version
			}
		}

		// Backstop: probing found nothing, or what it found does not open.
		for (final StorageFormat version : ZARR_VERSIONS) {
			if (version == probed)
				continue;
			try {
				return buildReader(root, version, options);
			} catch (final Exception ignored) {
			}
		}
		throw new N5Exception("Unable to open " + root.uri() + " as a Zarr reader");
	}

	private static N5Writer openAnyZarrVersionWriter(final KeyValueRoot root, final FormatOptions options) {

		/*
		 * Opening a writer creates the container, so an existing v2 container
		 * must not be shadowed by a freshly created v3 one: probe for a version
		 * that is already there and write at that one.
		 */
		final StorageFormat existing = inferZarrVersion(root);
		if (existing != null) {
			try {
				return buildWriter(root, existing, options);
			} catch (final Exception ignored) {
				// the keys are there but it does not open as that version
			}
		}

		// Nothing is there: create, newest version that works.
		for (final StorageFormat version : ZARR_VERSIONS) {
			if (version == existing)
				continue;
			try {
				return buildWriter(root, version, options);
			} catch (final Exception ignored) {
			}
		}
		throw new N5Exception("Unable to open " + root.uri() + " as a Zarr writer");
	}

	/**
	 * Which Zarr version is already at {@code root}, by probing keys:
	 * {@code zarr.json} for v3, {@code .zgroup}/{@code .zarray}/{@code .zattrs}
	 * for v2. One {@code isFile} per key, and no container is constructed.
	 *
	 * @return {@link StorageFormat#ZARR3}, {@link StorageFormat#ZARR2}, or null
	 * 		if neither is there
	 */
	private static StorageFormat inferZarrVersion(final KeyValueRoot root) {

		final InferredStorageFormat inferred = InferredStorageFormat.of(root.uri());

		// NB: inferred.resolveFromKeys() always probes:
		// root.uri() is a backend URI and has no storage scheme prefix.
		final StorageFormat probed = inferred.resolveFromKeys(root).format();

		// NB: need to check whether probed is ZARR2 or ZARR3
		// (it could be N5 if there is an attributes.json).
		return probed == StorageFormat.ZARR3 || probed == StorageFormat.ZARR2 ? probed : null;
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ instantiate concrete reader/writer                                   │
	// └──────────────────────────────────────────────────────────────────────┘

	private static N5KeyValueWriter buildN5Writer(final KeyValueRoot root, final N5Options options) {

		return new N5KeyValueWriter(root, options.gsonBuilder(), options.cacheAttributes());
	}

	private static N5KeyValueReader buildN5Reader(final KeyValueRoot root, final N5Options options) {

		return new N5KeyValueReader(root, options.gsonBuilder(), options.cacheAttributes());
	}

	private static ZarrKeyValueWriter buildZarr2Writer(final KeyValueRoot root, final Zarr2Options options) {

		return new ZarrKeyValueWriter(root, options.gsonBuilder(), options.mapN5DatasetAttributes(),
				options.mergeAttributes(), options.dimensionSeparator(), options.cacheAttributes());
	}

	private static ZarrKeyValueReader buildZarr2Reader(final KeyValueRoot root, final Zarr2Options options) {

		final ZarrKeyValueReader reader = new ZarrKeyValueReader(root, options.gsonBuilder(),
				options.mapN5DatasetAttributes(), options.mergeAttributes(), options.cacheAttributes());
		// NB: the dimension separator given to a reader is just a fallback.
		// The only use is for converting DatasetAttributes that did not come from the container,
		// which are passed through getConvertedDatasetAttributes().
		// This is a very artificial case, but why not ...
		reader.setDimensionSeparator(options.dimensionSeparator());
		return reader;
	}

	private static ZarrV3KeyValueWriter buildZarr3Writer(final KeyValueRoot root, final Zarr3Options options) {

		final ZarrV3KeyValueWriter writer = new ZarrV3KeyValueWriter(root, options.gsonBuilder(), options.cacheAttributes());
		writer.setDimensionSeparator(options.dimensionSeparator());
		return writer;
	}

	private static ZarrV3KeyValueReader buildZarr3Reader(final KeyValueRoot root, final Zarr3Options options) {

		final ZarrV3KeyValueReader reader = new ZarrV3KeyValueReader(root, options.gsonBuilder(), options.cacheAttributes());
		// NB: the dimension separator given to a reader is just a fallback.
		// The only use is for converting DatasetAttributes that did not come from the container,
		// which are passed through getConvertedDatasetAttributes().
		// This is a very artificial case, but why not ...
		reader.setDimensionSeparator(options.dimensionSeparator());
		return reader;
	}

	private static N5Writer buildHdf5Writer(final KeyValueRoot root, final HDF5Options options) {

		return Providers.hdf5().buildWriter(hdf5Path(root), options);
	}

	private static N5Reader buildHdf5Reader(final KeyValueRoot root, final HDF5Options options) {

		return Providers.hdf5().buildReader(hdf5Path(root), options);
	}

	private static String hdf5Path(final KeyValueRoot root) {

		final String path = root.uri().getPath();
		final String scheme = root.uri().getScheme();
		if (path == null || scheme != null && !"file".equalsIgnoreCase(scheme))
			throw new N5Exception("HDF5 containers can only be opened from the local file system, not " + root.uri());
		return path;
	}
}

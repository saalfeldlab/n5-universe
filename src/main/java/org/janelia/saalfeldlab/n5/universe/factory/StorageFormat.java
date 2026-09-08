package org.janelia.saalfeldlab.n5.universe.factory;

import java.util.regex.Pattern;

/**
 * The format ("dialect") of a container.
 * <p>
 * This is the identity of a storage format and how it is spelled as a URI
 * scheme prefix — nothing more. Working out which format a given URI or
 * container actually <em>is</em> belongs to {@link InferredStorageFormat}.
 */
public enum StorageFormat {

	// NB: Declaration order is meaningful and important!
	// N5Factory.orderedStorageFormats() sorts based on this, so the
	// declaration order here defines the order in which formats are
	// tried if the format cannot be inferred any other way.

	/** Zarr of whichever version is present; resolved to {@link #ZARR3} then {@link #ZARR2} at open time. */
	ZARR("zarr", "zarr"),
	ZARR3("zarr3", "zarr3"),
	ZARR2("zarr2", "zarr2"),
	N5("n5", "n5"),
	HDF5("hdf5", "h(df)?5");

	private final String scheme;
	private final Pattern schemePattern;

	StorageFormat(final String scheme, final String schemeRegex) {

		this.scheme = scheme;
		this.schemePattern = Pattern.compile(schemeRegex, Pattern.CASE_INSENSITIVE);
	}

	/**
	 * The canonical scheme prefix for this format, lower case, e.g.
	 * {@code "zarr3"}. Use this when <em>writing</em> a URI.
	 *
	 * @return the canonical scheme token
	 */
	public String scheme() {

		return scheme;
	}

	/**
	 * Every scheme token accepted for this format — for {@link #HDF5} both
	 * {@code "h5"} and {@code "hdf5"}. Use this when <em>reading</em> a URI.
	 *
	 * @return the scheme pattern
	 */
	Pattern schemePattern() {

		return schemePattern;
	}
}

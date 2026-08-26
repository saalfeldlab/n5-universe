package org.janelia.saalfeldlab.n5.universe.kvr;

import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5Dialect;
import org.janelia.saalfeldlab.n5.zarr.ZarrV2Dialect;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3Dialect;

/**
 * Properties of known {@code ContainerDialect}s.
 * <ul>
 *     <li>{@link #attributeFileNames} for harvesting a hierarchy into an in-memory {@link DirectoryStore}.</li>
 *     <li>{@link #jqResource} dialect-specific jq definitions to read in {@code JqTranslation}</li>*
 * </ul>
 */
enum DialectInfo {
	N5       ("/kvr/dialect-n5.jq",    "attributes.json"),
	ZARR_V2  ("/kvr/dialect-zarr2.jq", ".zarray", ".zattrs", ".zgroup"),
	ZARR_V3  ("/kvr/dialect-zarr3.jq", "zarr.json");
	// TODO: jq resources should move out of /kvr/ when old translation is phased out

	public static DialectInfo of(GsonN5Reader n5) {
		ContainerDialect dialect = n5.getContainerDialect();

		// strip possible NotifyingDialect wrapper
		if (dialect instanceof NotifyingDialect)
				dialect = ((NotifyingDialect) dialect).delegate;

		if (dialect instanceof N5Dialect)
			return N5;
		else if (dialect instanceof ZarrV2Dialect)
			return ZARR_V2;
		else if (dialect instanceof ZarrV3Dialect)
			return ZARR_V3;
		else
			throw new IllegalArgumentException("Unsupported ContainerDialect: " + dialect.getClass().getSimpleName());
	}

	private final String jqResource;

	private final String[] attributeFileNames;

	DialectInfo(final String jqResource, final String... attributeFileNames) {
		this.jqResource = jqResource;
		this.attributeFileNames = attributeFileNames;
	}

	/**
	 * Resource path of the jq accessors ({@code attrs}, {@code setAttrs},
	 * {@code dsAttrs}, {@code dsDimensions}) for this dialect.
	 */
	String jqResource() {
		return jqResource;
	}

	/**
	 * Attribute file name(s) used by this dialect. These are what {@link
	 * Directory#collectFrom(GsonN5Reader)} tries to read.
	 */
	String[] attributeFileNames() {
		return attributeFileNames;
	}
}

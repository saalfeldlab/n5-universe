package org.janelia.saalfeldlab.n5.universe.kvr;

import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5Dialect;
import org.janelia.saalfeldlab.n5.hdf5.Hdf5Dialect;
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
	N5       (true, "/kvr/dialect-n5.jq",    "attributes.json"),
	ZARR_V2  (true, "/kvr/dialect-zarr2.jq", ".zarray", ".zattrs", ".zgroup"),
	ZARR_V3  (true, "/kvr/dialect-zarr3.jq", "zarr.json"),
	// HDF5 has no attribute files. Hdf5HierarchyStore synthesizes virtual
	// "attributes.json" that works with N5 for reading.
	// Writing is not supported because for HDF5
	//  - a dataset is not just directory + DatasetAttributes
	//  - if possible, attributes are not written through attributes.json
	//    because that is lossy wrt attribute type
	HDF5     (false, "/kvr/dialect-n5.jq", "attributes.json");
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
		else if (dialect instanceof Hdf5Dialect)
			return HDF5;
		else
			throw new IllegalArgumentException("Unsupported ContainerDialect: " + dialect.getClass().getSimpleName());
	}

	private final boolean supportsWriting;

	private final String jqResource;

	private final String[] attributeFileNames;

	DialectInfo(final boolean supportsWriting, final String jqResource, final String... attributeFileNames) {
		this.supportsWriting = supportsWriting;
		this.jqResource = jqResource;
		this.attributeFileNames = attributeFileNames;
	}

	/**
	 * Whether a translated hierarchy can be written back to a container of this
	 * dialect.
	 * <p>
	 * Currently, all dialects support write-back except HDF5. A HDF5 dataset is
	 * not a directory (group) with attributes. HDF5 attributes are natively
	 * typed, so writing them back from json would re-type everything.
	 */
	boolean supportsWriting() {
		return supportsWriting;
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

package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadataGroup;

public class CanonicalMultiscaleMetadata extends AbstractN5Metadata implements CanonicalMetadata, SpatialMetadataGroup< CalibratedTransformMetadata > {

	private final MultiResolutionSpatialMetadataCanonical multiscales;

	public CanonicalMultiscaleMetadata(final String path, 
			final MultiResolutionSpatialMetadataCanonical multiscales) {
		super( path );
		this.multiscales = multiscales;
	}

	@Override
	public String[] getPaths() {
		return multiscales.getPaths();
	}

	@Override
	public CalibratedTransformMetadata[] getChildrenMetadata() {
		return multiscales.getChildrenMetadata();
	}

	@Override
	public String[] units() {
		return multiscales.units();
	}
}

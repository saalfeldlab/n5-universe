package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import net.imglib2.realtransform.Scale;

public class IdentityCoordinateTransformation extends AbstractCoordinateTransformation<Scale> {

	public static final String TYPE = "identity";

	public IdentityCoordinateTransformation() {
		super(TYPE);
	}

	@Override
	public Scale getTransform() {
		// max dimensionality is v0.4
		return new Scale(1, 1, 1, 1, 1);
	}

}

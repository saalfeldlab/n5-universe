package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractCoordinateTransform.TransformOptions;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;

import net.imglib2.realtransform.Scale;

public class IdentityCoordinateTransformation extends AbstractCoordinateTransformation<Scale> {

	public static final String TYPE = "identity";

	public IdentityCoordinateTransformation() {
		super(TYPE);
	}

	@Override
	public Scale getTransform() {
		// 5 is the max dimensionality for OME-Zarr v0.4.
		return new Scale(1, 1, 1, 1, 1);
	}

	@Override
	public CoordinateTransform<Scale> copy(TransformOptions<?> copyParams) {

		return new IdentityCoordinateTransformation();
	}

}

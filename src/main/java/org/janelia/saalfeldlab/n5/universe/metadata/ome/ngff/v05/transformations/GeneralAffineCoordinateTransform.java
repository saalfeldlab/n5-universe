package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.realtransform.AffineRealTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class GeneralAffineCoordinateTransform extends AbstractAffineCoordinateTransform<AffineRealTransform> {

	protected GeneralAffineCoordinateTransform() {
		super();
	}

	public GeneralAffineCoordinateTransform(final GeneralAffineCoordinateTransform ct) {
		super(ct);
	}

	public GeneralAffineCoordinateTransform(final double[][] affine) {
		super(affine);
	}

	public GeneralAffineCoordinateTransform(final String name, final String inputSpace,
			final String outputSpace, final double[][] affine) {
		super(name, inputSpace, outputSpace, affine);
	}

	public GeneralAffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(name, n5, path, inputSpace, outputSpace);
	}

	public GeneralAffineCoordinateTransform(final String name,
			final int[] inputAxes, final int[] outputAxes, final double[][] affine) {
		super(name, inputAxes, outputAxes, affine);
	}

	public GeneralAffineCoordinateTransform(final String name,
			final String inputSpace, final String outputSpace, final String path) {
		super(name, inputSpace, outputSpace, path);
	}

	@Override
	protected AffineRealTransform createTransform() {
		return new AffineRealTransform(numSourceDimensions(), numTargetDimensions(),
				TransformUtils.flatten(affine));
	}

	private int numSourceDimensions() {
		return affine[0].length - 1;
	}

	private int numTargetDimensions() {
		return affine.length;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class InvertibleAffineCoordinateTransform extends AbstractAffineCoordinateTransform<AffineGet>
		implements InvertibleCoordinateTransform<AffineGet> {

	protected InvertibleAffineCoordinateTransform() {
		super();
	}

	public InvertibleAffineCoordinateTransform(final InvertibleAffineCoordinateTransform ct) {
		super(ct);
	}

	public InvertibleAffineCoordinateTransform(final double[][] affine) {
		super(affine);
	}

	public InvertibleAffineCoordinateTransform(final String name, final String inputSpace,
			final String outputSpace, final double[][] affine) {
		super(name, inputSpace, outputSpace, affine);
	}

	public InvertibleAffineCoordinateTransform(final String name, final OmeNgffReference inputRef,
			final OmeNgffReference outputRef, final double[][] affine) {
		super(name, inputRef, outputRef, affine);
	}

	public InvertibleAffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(name, n5, path, inputSpace, outputSpace);
	}

	public InvertibleAffineCoordinateTransform(final String name,
			final int[] inputAxes, final int[] outputAxes, final double[][] affine) {
		super(name, inputAxes, outputAxes, affine);
	}

	public InvertibleAffineCoordinateTransform(final String name,
			final String inputSpace, final String outputSpace, final String path) {
		super(name, inputSpace, outputSpace, path);
	}

	public InvertibleAffineCoordinateTransform(final String name,
			final OmeNgffReference inputSpace, final OmeNgffReference outputSpace, final String path) {
		super(name, inputSpace, outputSpace, path);
	}

	@Override
	protected AffineGet createTransform() {

		final double[] affineFlat = TransformUtils.flatten(affine);
		if (affineFlat.length == 6) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set(affineFlat);
			return tmp;
		} else if (affineFlat.length == 12) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set(affineFlat);
			return tmp;
		} else {
			int nd = (int) Math.floor(Math.sqrt(affineFlat.length));
			AffineTransform tmp = new AffineTransform(nd);
			tmp.set(affineFlat);
			return tmp;
		}
	}

}

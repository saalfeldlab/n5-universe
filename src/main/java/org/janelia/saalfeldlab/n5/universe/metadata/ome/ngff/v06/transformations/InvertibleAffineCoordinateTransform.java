package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

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

	protected InvertibleAffineCoordinateTransform(final InvertibleAffineCoordinateTransform other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
	}

	@Override
	public InvertibleAffineCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public InvertibleAffineCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new InvertibleAffineCoordinateTransform(this, copyParams);
	}

	private InvertibleAffineCoordinateTransform(final Options options) {
		super(options);
	}

	public InvertibleAffineCoordinateTransform(final double[][] affine) {
		this(new Options().affine(affine));
	}

	public static class Options extends AbstractAffineOptions<Options> {

		public InvertibleAffineCoordinateTransform build() {
			return new InvertibleAffineCoordinateTransform(this);
		}
	}

	public static Options options() {
		return new Options();
	}

	@Override
	protected InvertibleAffineCoordinateTransform withPath(final String path) {

		return copy(new BasicOptions().path(path));
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

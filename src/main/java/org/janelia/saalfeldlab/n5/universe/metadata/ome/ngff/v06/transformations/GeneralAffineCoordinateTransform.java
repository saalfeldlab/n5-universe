package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.realtransform.AffineRealTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import net.imglib2.realtransform.AffineGet;

public class GeneralAffineCoordinateTransform extends AbstractAffineCoordinateTransform<AffineRealTransform> {

	protected GeneralAffineCoordinateTransform() {
		super();
	}

	protected GeneralAffineCoordinateTransform(final GeneralAffineCoordinateTransform other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
	}

	@Override
	public GeneralAffineCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public GeneralAffineCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new GeneralAffineCoordinateTransform(this, copyParams);
	}

	private GeneralAffineCoordinateTransform(final Options options) {
		super(options);
	}

	public GeneralAffineCoordinateTransform(final double[][] affine) {
		this(new Options().affine(affine));
	}

	public static class Options extends AbstractAffineOptions<Options> {

		public GeneralAffineCoordinateTransform build() {
			return new GeneralAffineCoordinateTransform(this);
		}
	}

	public static Options options() {
		return new Options();
	}

	@Override
	protected GeneralAffineCoordinateTransform withPath(final String path) {

		return copy(new BasicOptions().path(path));
	}

	@Override
	protected AffineRealTransform createTransform() {
		return new AffineRealTransform(numSourceDimensions(), numTargetDimensions(),
				TransformUtils.flatten(affine));
	}

	private int numSourceDimensions() {

		if (affine == null)
			return -1;

		return affine[0].length - 1;
	}

	private int numTargetDimensions() {

		if (affine == null)
			return -1;

		return affine.length;
	}

	/**
	 * Returns an invertible AffineGet if possible, otherwise throws a Runtime exception.
	 * 
	 * @return an invertible affine
	 */
	public InverseCoordinateTransform<AffineGet, InvertibleAffineCoordinateTransform> tryInverse() {

		// a path affine's shape is only known once its parameters are read
		final double[][] params = affine != null ? affine : getParameters();
		if (params == null)
			throw new N5Exception("Cannot invert affine: no parameters"
					+ (getParameterPath() != null ? " at \"" + getParameterPath() + "\"" : ""));

		final int nSource = params[0].length - 1;
		final int nTarget = params.length;
		if (nSource != nTarget)
			throw new N5Exception(String.format(
					"Cannot invert affine with different source dimensions (%d) and target dimensions (%d).",
					nSource, nTarget));

		try {
			final InvertibleAffineCoordinateTransform invertible =
					InvertibleAffineCoordinateTransform.options()
					.name(getName())
					.input(getInput())
					.output(getOutput())
					.affine(params)
					.build();
			return new InverseCoordinateTransform(invertible);
		} catch (RuntimeException e) {
			throw new N5Exception("Affine not invertible: matrix is singular");
		}
	}

}

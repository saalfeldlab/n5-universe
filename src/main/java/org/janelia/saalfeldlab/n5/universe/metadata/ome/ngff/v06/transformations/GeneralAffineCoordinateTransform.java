package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.realtransform.AffineRealTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import net.imglib2.realtransform.AffineGet;

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

	public GeneralAffineCoordinateTransform(final String name, final OmeNgffReference inputRef,
			final OmeNgffReference outputRef, final double[][] affine) {
		super(name, inputRef, outputRef, affine);
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
					new InvertibleAffineCoordinateTransform(getName(), getInput(), getOutput(), params);
			invertible.setInputAxes(getInputAxes());
			invertible.setOutputAxes(getOutputAxes());
			return new InverseCoordinateTransform(invertible);
		} catch (RuntimeException e) {
			throw new N5Exception("Affine not invertible: matrix is singular");
		}
	}

}

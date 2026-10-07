package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractAffineCoordinateTransform<T extends RealTransform>
		extends AbstractParametrizedTransform<T, double[][]> {

	public static final String TYPE = "affine";
	public double[][] affine;
	public transient T transform;

	protected AbstractAffineCoordinateTransform() {
		super(TYPE);
	}

	protected AbstractAffineCoordinateTransform(final AbstractAffineCoordinateTransform<T> ct) {
		super(ct);
	}

	protected AbstractAffineCoordinateTransform(final double[][] affine) {
		super(TYPE);
		this.affine = affine;
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final String name, final String inputSpace,
			final String outputSpace, final double[][] affine) {
		super(TYPE, name, inputSpace, outputSpace);
		this.affine = affine;
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final String name, final OmeNgffReference inputRef,
			final OmeNgffReference outputRef, final double[][] affine) {
		super(TYPE, name, inputRef, outputRef);
		this.affine = affine;
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace);
		bindParameters(n5, OmeNgffMetadataParser.reverse(n5));
		buildTransform(getParameters());
	}

	protected AbstractAffineCoordinateTransform(final String name,
			final int[] inputAxes, final int[] outputAxes, final double[][] affine) {
		super(TYPE, name, null, inputAxes, outputAxes);
		this.affine = affine;
	}

	protected AbstractAffineCoordinateTransform(final String name,
			final String inputSpace, final String outputSpace, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace);
	}

	protected AbstractAffineCoordinateTransform(final String name,
			final OmeNgffReference input, final OmeNgffReference output, final String path) {
		super(TYPE, name, path, input, output);
	}

	@Override
	public T getTransform() {

		if (transform != null)
			return transform;
		else if (path != null)
			return buildTransform(getParameters());

		return null;
	}

	@Override
	protected double[][] readParameters(final N5Reader n5, final String path, final boolean reverse) {

		return CoordinateTransformAdapter.affineMatrixSupplier(n5, path, reverse);
	}

	public final T buildTransform(final double[][] params) {
		this.affine = params;
		return buildTransform();
	}

	public final T buildTransform() {
		if (affine == null)
			return null;
		transform = createTransform();
		return transform;
	}

	protected abstract T createTransform();

	public void write(final N5Writer n5, final String dataset) {

		final double[][] stored = OmeNgffMetadataParser.reverse(n5)
				? TransformUtils.reverseCoordinates(affine)
				: affine;
		CoordinateTransformAdapter.writeDoubleArray2d(n5, dataset, stored);
	}

}

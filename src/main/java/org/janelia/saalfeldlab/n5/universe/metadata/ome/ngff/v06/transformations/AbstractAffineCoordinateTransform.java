package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
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

	protected AbstractAffineCoordinateTransform(final AffineOptions options) {
		super(TYPE, options);
		this.affine = deepCopy(options.affine);
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final AbstractAffineCoordinateTransform<T> other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		this.affine = deepCopy((copyParams instanceof AffineOptions && ((AffineOptions) copyParams).isAffineSet())
				? ((AffineOptions) copyParams).getAffine()
				: other.affine);
		buildTransform();
	}

	public static class AffineOptions extends TransformOptions<AffineOptions> {

		private boolean affineSet = false;
		private double[][] affine;

		public AffineOptions withAffine(double[][] affine) {
			this.affine = affine;
			this.affineSet = true;
			return this;
		}

		public boolean isAffineSet() {
			return affineSet;
		}

		public double[][] getAffine() {
			return affine;
		}
	}

	@Override
	public T getTransform() {

		if (transform != null)
			return transform;
		else if (path != null)
			return buildTransform(requireParameters());

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

	/**
	 * @param path the parameter path
	 * @return a copy of this transform with the same matrix that references its parameters by {@code path}
	 */
	protected abstract AbstractAffineCoordinateTransform<T> withPath(final String path);

	/**
	 * Writes this transform's matrix to an array.
	 *
	 * @param n5 the writer
	 * @param dataset the dataset to write the matrix to
	 * @return a copy of this transform that references the matrix by {@code dataset}, bound to {@code n5}.
	 *         It keeps the matrix, so it does not read the array to build its transform.
	 */
	public AbstractAffineCoordinateTransform<T> write(final N5Writer n5, final String dataset) {

		final boolean reverse = OmeNgffMetadataParser.storesDimensionsReversed(n5);
		final double[][] stored = reverse
				? TransformUtils.reverseCoordinates(affine)
				: affine;
		CoordinateTransformAdapter.writeDoubleArray2d(n5, dataset, stored);

		final AbstractAffineCoordinateTransform<T> written = withPath(dataset);
		written.bindParameters(n5, reverse);
		return written;
	}

}

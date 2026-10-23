package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class RotationCoordinateTransform extends AbstractParametrizedTransform<AffineGet,double[][]> implements InvertibleCoordinateTransform<AffineGet> {

	public static final String TYPE = "rotation";

	public double[][] rotation;

	public transient AffineGet transform;

	private static transient double EPSILON = 1e-3;

	protected RotationCoordinateTransform() {
		super(TYPE);
	}

	public RotationCoordinateTransform( RotationCoordinateTransform ct ) {
		super( ct );
	}

	protected RotationCoordinateTransform( RotationCoordinateTransform other, TransformOptions copyParams ) {
		super( other, copyParams );
		if (other.rotation != null)
			buildTransform(deepCopy(other.rotation));
	}

	@Override
	public RotationCoordinateTransform copy() {

		return copy(new TransformOptions());
	}

	@Override
	public RotationCoordinateTransform copy(final TransformOptions copyParams) {

		return new RotationCoordinateTransform(this, copyParams);
	}

	public RotationCoordinateTransform(final double[] affineFlat) {
		super(TYPE);
		this.rotation = TransformUtils.toRotationMatrix(affineFlat);
		buildTransform(affineFlat);
	}

	public RotationCoordinateTransform(final String name, final String inputSpace, final String outputSpace,
			final double[] affineFlat) {
		super(TYPE, name, inputSpace, outputSpace);
		this.rotation = TransformUtils.toRotationMatrix(affineFlat);
		buildTransform(affineFlat);
	}

	public RotationCoordinateTransform(final String name, final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final double[] affineFlat) {
		super(TYPE, name, inputRef, outputRef);
		this.rotation = TransformUtils.toRotationMatrix(affineFlat);
		buildTransform(affineFlat);
	}

	public RotationCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace );
		bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
		buildTransform(getParameters());
	}

	public RotationCoordinateTransform(final double[][] affine) {
		super(TYPE);
		buildTransform(affine);
	}

	public RotationCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final double[][] affine) {
		super(TYPE, name, inputSpace, outputSpace);
		buildTransform(affine);
	}

	public RotationCoordinateTransform(final String name, final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final double[][] affine) {
		super(TYPE, name, inputRef, outputRef);
		buildTransform(affine);
	}

	public RotationCoordinateTransform(final String name,
			final String inputSpace, final String outputSpace, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace);
	}

	@Override
	public AffineGet getTransform() {

		if (transform != null)
			return transform;
		else if (rotation != null)
			return buildTransform(rotation);
		else if (path != null)
			return buildTransform(requireParameters());

		return null;
	}

	public void write(final N5Writer n5, final String dataset) {

		final double[][] stored = OmeNgffMetadataParser.storesDimensionsReversed(n5)
				? TransformUtils.reverseCoordinatesRotation(rotation)
				: rotation;
		CoordinateTransformAdapter.writeDoubleArray2d(n5, dataset, stored);
	}

	@Override
	protected double[][] readParameters(final N5Reader n5, final String path, final boolean reverse) {

		return CoordinateTransformAdapter.rotationMatrixSupplier(n5, path, reverse);
	}

	public AffineGet buildTransform(double[][] params) {

		this.rotation = params;
		if (params == null)
			return null;

		return buildTransform(TransformUtils.flattenRotation(params));
	}

	/**
	 * Creates an imglib2 AffineGet from a flattened representation.
	 * <p>
	 * This flat array must be compatible with the imglib2 classes, and
	 * is more general than a rotation. For example, the transform
	 * that is returned has a translation component that
	 * is not allowed by a rotation. It's there so that we can 
	 * use the imglib2 classes.
	 * 
	 * @param affineFlat 
	 *		flat affine in homogeneous coordinates 
	 * @return
	 * 	an imglib2 AffineGet
	 */
	public AffineGet buildTransform( final double[] affineFlat) {

		if (affineFlat == null) {
			return null;
		}

		if (affineFlat.length == 6) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set(affineFlat);
			transform = tmp;
		} else if (affineFlat.length == 12) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set(affineFlat);
			transform = tmp;
		} else {
			int nd = (int)Math.floor(Math.sqrt(affineFlat.length));
			AffineTransform tmp = new AffineTransform(nd);
			tmp.set(affineFlat);
			transform = tmp;
		}
		return transform;
	}

	private static void validate(AffineGet affine, double eps) {

		// imglib2 AffineGet always ave numSourceDimensions == numTargetDimensions
		int nd = affine.numSourceDimensions();
		final DMatrixRMaj mtx = new DMatrixRMaj( nd, nd );
		for( int i = 0; i < nd; i++)
			for( int j = 0; j < nd; j++)
				mtx.set(i, j, affine.get(i, j));

		final double determinant = CommonOps_DDRM.det(mtx);
		System.out.println(determinant);
		if (Math.abs(1 - determinant) > EPSILON)
			throw new IllegalArgumentException(
					String.format("Rotation matrix must have determinant = 1, but is: %f ", determinant));

	}

}

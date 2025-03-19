package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

@NameConfig.Name("rotation")
public class RotationCoordinateTransform extends BaseLinearCoordinateTransform<AffineGet> {

	public static final String TYPE = "rotation";

	@NameConfig.Parameter(optional = true)
	protected JsonElement rotation;

	private static final double EPSILON = 1e-6;

	protected RotationCoordinateTransform() {
		super(TYPE);
	}

	public RotationCoordinateTransform( BaseLinearCoordinateTransform<AffineGet> ct ) {
		super( ct );
		validate(ct.transform, EPSILON);
	}

	/**
	 * Create a coordinate transform from a rotation matrix stored as a 2D array.
	 *
	 * @param matrix the matrix stored as double[rows][columns]
	 */
	public RotationCoordinateTransform( final double[][] matrix) {
		super(TYPE, TransformUtils.flattenRotation(matrix));
		buildTransform(affineFlat);
		validate(transform, EPSILON);
	}

	public RotationCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final double[][] matrix) {
		super(TYPE, name, inputSpace, outputSpace, TransformUtils.flattenRotation(matrix));
		buildTransform(affineFlat);
		validate(transform, EPSILON);
	}

	public RotationCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.affineFlat = getParameters(n5);
		buildTransform(affineFlat);
		validate(transform, EPSILON);
	}

	public RotationCoordinateTransform( final String name, 
			final String[] inputAxes, final String[] outputAxes,
			final double[][] matrix ) {
		super(TYPE, name, inputAxes, outputAxes, TransformUtils.flattenRotation(matrix));
		validate(transform, EPSILON);
	}

	public RotationCoordinateTransform( final String name, 
			final String inputSpace, final String outputSpace, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace);
	}

	public JsonElement getJsonParameter() {
		return rotation;
	}
	
	@Override
	protected void buildJsonParameter() {

		if (affineFlat != null) {
			final double[][] rotationFOrder = TransformUtils.toRotationMatrix(affineFlat);
			final double[][] rotationCOrder = TransformUtils.reverseCoordinatesRotation(rotationFOrder);
			affineJson = (new Gson()).toJsonTree(rotationCOrder);
		}
		this.rotation = affineJson;
	}
	
	public void interpretParameters() {
		final double[][] mtxCOrder = (new Gson()).fromJson(rotation, double[][].class);
		final double[][] mtxFOrder = TransformUtils.reverseCoordinatesRotation(mtxCOrder);
		affineFlat = TransformUtils.flattenRotation(mtxFOrder);
	}


	@Override
	public double[] getParameters(N5Reader n5) {

		System.out.println("rot getParameters");
		if (n5 == null)
			return null;

		final double[][] rotationCOrder = getDoubleArray2(n5, getParameterPath());
		final double[][] rotationFOrder = TransformUtils.reverseCoordinatesRotation(rotationCOrder);
		return TransformUtils.flattenRotation(rotationFOrder);
	}

	@Override
	public AffineGet buildTransform(double[] parameters) {

		if (parameters == null)
			return null;

		if (parameters.length == 6) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set(parameters);
			transform = tmp;
		} else if (parameters.length == 12) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set(parameters);
			transform = tmp;
		} else {
			int nd = (int)Math.floor(Math.sqrt(parameters.length));
			AffineTransform tmp = new AffineTransform(nd);
			tmp.set(parameters);
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

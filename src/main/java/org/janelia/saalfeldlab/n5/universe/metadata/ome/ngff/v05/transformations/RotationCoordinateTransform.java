package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import com.google.gson.JsonElement;

import net.imglib2.realtransform.AffineGet;

@NameConfig.Name("rotation")
public class RotationCoordinateTransform extends BaseLinearCoordinateTransform<AffineGet> {

	public static String TYPE = "rotation";

	@NameConfig.Parameter()
	public JsonElement rotation;

	private static final double EPSILON = 1e-6;

	protected RotationCoordinateTransform() {
		super(TYPE);
	}

	public RotationCoordinateTransform( BaseLinearCoordinateTransform<AffineGet> ct ) {
		super( ct );
		validate(ct.transform, EPSILON);
	}

	public RotationCoordinateTransform( final double[][] matrix) {
		super(TYPE, flattenRotationMatrix(matrix));
		buildTransform(affineFlat);
		validate(transform, EPSILON);
	}

	public RotationCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final double[][] matrix) {
		super(TYPE, name, inputSpace, outputSpace, flattenRotationMatrix(matrix));
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
		super(TYPE, name, inputAxes, outputAxes, flattenRotationMatrix(matrix));
		validate(transform, EPSILON);
	}

	public RotationCoordinateTransform( final String name, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace  );
	}

	public JsonElement getJsonParameter() {
		return rotation;
	}
	
	@Override
	protected void buildJsonParameter() {

		super.buildJsonParameter();
		this.rotation = affineJson;
	}
	
	/**
	 * Turn an N x N matrix into a flattened (N+1) x (N+1) matrix in homogeneous coordinates.
	 * 
	 * 
	 * @param matrix a rotation matrix
	 * @return the corresponding flattened (row-major) matrix in homogeneous coordinates.
	 */
	private static double[] flattenRotationMatrix( double[][] matrix ) {

		final int nd = matrix.length;
		final double[] out = new double[nd * (nd+1)];
		int pos = 0;
		for( int r = 0; r < nd; r++) {
			System.arraycopy(matrix[r], 0, out, pos, nd);
			pos += (nd + 1);
		}

		return out;
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

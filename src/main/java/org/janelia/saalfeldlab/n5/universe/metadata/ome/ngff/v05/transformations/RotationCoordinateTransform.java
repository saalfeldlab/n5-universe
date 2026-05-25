package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import com.google.gson.JsonElement;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.DoubleType;

public class RotationCoordinateTransform extends AbstractParametrizedTransform<AffineGet,double[][]> implements InvertibleCoordinateTransform<AffineGet> {

	public static final String TYPE = "rotation";

	public JsonElement rotation;
	
	public transient double[] affineFlat;

	public transient AffineGet transform;

	private static transient double EPSILON = 1e-3;

	protected RotationCoordinateTransform() {
		super(TYPE);
	}

	public RotationCoordinateTransform( RotationCoordinateTransform ct ) {
		super( ct );
	}

	public RotationCoordinateTransform( final double[] affine) {
		super(TYPE);
		this.affineFlat = affine;
		buildTransform();
	}

	public RotationCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final double[] affine) {
		super(TYPE, name, inputSpace, outputSpace);
		this.affineFlat = affine;
		buildTransform();
	}

	public RotationCoordinateTransform( final String name, final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final double[] affine) {
		super(TYPE, name, inputRef, outputRef);
		this.affineFlat = affine;
		buildTransform();
	}

	public RotationCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {

		super(TYPE, name, path, inputSpace, outputSpace );
		buildTransform(getParameters(n5));
	}

	public RotationCoordinateTransform( final String name, 
			final int[] inputAxes, final int[] outputAxes,
			final double[] affine ) {
		super(TYPE, name, null, inputAxes, outputAxes);
		this.affineFlat = affine;
	}

	public RotationCoordinateTransform( final String name, 
			final String inputSpace, final String outputSpace, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace );
	}

	public JsonElement getJsonParameter() {
		return rotation;
	}

	@Override
	public AffineGet getTransform() {
		return transform;
	}

//	protected void buildJsonParameter() {
//
//		if (affineFlat != null) {
//			final double[][] affineFOrder = TransformUtils.toAffineMatrix(affineFlat);
//			final double[][] affineCOrder = TransformUtils.reverseCoordinates(affineFOrder);
//			affine = (new Gson()).toJsonTree(affineCOrder);
//		}
//	}

	public void write(final N5Writer n5, final String dataset) {

		final double[][] affineMtx = TransformUtils.toAffineMatrix(affineFlat);
		final double[][] affineCorder = TransformUtils.reverseCoordinates(affineMtx);
		ArrayImg<DoubleType, DoubleArray> img = ArrayImgs.doubles(
				TransformUtils.flatten(affineCorder),
				 affineCorder[0].length, affineCorder.length);
		
		final int nd = img.numDimensions();
		final int[] blkSize = new int[nd];
		for (int i = 0; i < nd; i++)
			blkSize[i] = (int) img.dimension(i);

		N5Utils.save(img, n5, dataset, blkSize, new RawCompression());
	}

	public AffineGet buildTransform(double[][] params) {

		affineFlat = TransformUtils.flattenRotation(params);
		return buildTransform();
	}

	public AffineGet buildTransform() {

		if (affineFlat == null) {
			if (rotation == null)
				return null;
			else
				return buildTransform(MetadataUtils.toMatrix(rotation));
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

	@Override
	public double[][] getParameters(N5Reader n5) {

		if (n5 == null)
			return null;

		final double[][] rotCOrder = getDoubleArray2(n5, getParameterPath());
		final double[][] rotCOrderT = TransformUtils.transpose(rotCOrder);
		final double[][] rotFOrder = TransformUtils.reverseCoordinatesRotation(rotCOrderT);
		return rotFOrder;
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

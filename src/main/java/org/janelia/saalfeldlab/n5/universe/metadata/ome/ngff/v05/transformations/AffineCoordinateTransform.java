package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import com.google.gson.JsonElement;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.type.numeric.real.DoubleType;

@NameConfig.Name("affine")
public class AffineCoordinateTransform extends BaseLinearCoordinateTransform<AffineGet> {

	public static final String TYPE = "affine";

	@NameConfig.Parameter(optional = true)
	public JsonElement affine;

	protected AffineCoordinateTransform() {
		super(TYPE);
	}

	public AffineCoordinateTransform( BaseLinearCoordinateTransform<AffineGet> ct ) {
		super( ct );
	}

	public AffineCoordinateTransform( final double[] affine) {
		super(TYPE, affine);
		buildTransform(affine);
	}

	public AffineCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final double[] affine) {
		super(TYPE, name, inputSpace, outputSpace, affine );
		this.affineFlat = affine;
		buildTransform( affine );
	}

	public AffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.affineFlat = getParameters(n5);
		buildTransform(affineFlat);
	}

	public AffineCoordinateTransform( final String name, 
			final String[] inputAxes, final String[] outputAxes,
			final double[] affine ) {
		super(TYPE, name, inputAxes, outputAxes, affine );
	}

	public AffineCoordinateTransform( final String name, 
			final String inputSpace, final String outputSpace, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace );
	}

	public JsonElement getJsonParameter() {
		return affine;
	}

	@Override
	protected void buildJsonParameter() {

		super.buildJsonParameter();
		this.affine = affineJson;
	}

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

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import com.google.gson.JsonElement;

import net.imglib2.realtransform.AffineGet;

@NameConfig.Name("affine")
public class AffineCoordinateTransform extends BaseLinearCoordinateTransform<AffineGet> {

	public static String TYPE = "affine";

	@NameConfig.Parameter()
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

	public AffineCoordinateTransform( final String name, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace  );
	}

	public JsonElement getJsonParameter() {
		return affine;
	}

	@Override
	protected void buildJsonParameter() {

		super.buildJsonParameter();
		this.affine = affineJson;
	}

}

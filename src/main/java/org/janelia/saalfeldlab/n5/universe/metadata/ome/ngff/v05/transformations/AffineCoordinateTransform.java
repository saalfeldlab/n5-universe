package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import com.google.gson.JsonElement;

import net.imglib2.realtransform.AffineGet;

public class AffineCoordinateTransform extends BaseLinearCoordinateTransform<AffineGet> {

	public AffineCoordinateTransform( BaseLinearCoordinateTransform<AffineGet> ct )
	{
		super( ct );
	}

	public AffineCoordinateTransform( final double[] affine) {
		super("affine");
		this.affineFlat = affine;
		buildTransform(affine );
	}

	public AffineCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final double[] affine) {
		super("affine", name, inputSpace, outputSpace );
		this.affineFlat = affine;
		buildTransform( affine );
	}

	public AffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super("affine", name, path, inputSpace, outputSpace );
		this.affineFlat = getParameters(n5);
		buildTransform(affineFlat);
	}

	public AffineCoordinateTransform( final String name, 
			final String[] inputAxes, final String[] outputAxes,
			final double[] affine ) {
		super("affine", name, inputAxes, outputAxes  );
		this.affineFlat = affine;
	}

	public AffineCoordinateTransform( final String name, final String path,
			final String inputSpace, final String outputSpace) {
		super(name, path, inputSpace, outputSpace  );
	}

	public JsonElement getJsonParameter() {
		return affine;
	}

}

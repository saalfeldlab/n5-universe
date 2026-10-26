package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractCoordinateTransform.TransformOptions;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ScaleCoordinateTransform.ScaleOptions;

import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ScaleGet;

public class ScaleCoordinateTransformation extends AbstractCoordinateTransformation<ScaleGet>
{
	public static final String TYPE = "scale";
	
	protected double[] scale;

	public ScaleCoordinateTransformation(final double[] scale) {
		super(TYPE);
		this.scale = scale;
	}
	
	public ScaleCoordinateTransformation(ScaleOptions options) {
		super(TYPE);
		this.scale = options.getScale();
	}

	protected  ScaleCoordinateTransformation(ScaleCoordinateTransformation other) {
		this(other.scale);
	}

	protected ScaleCoordinateTransformation(ScaleCoordinateTransformation other, TransformOptions<?> copyParams) {
		super(TYPE);
		this.scale = other.scale == null ? null : other.scale.clone();
	}

	public double[] getScale() {
		return scale;
	}

	@Override
	public ScaleGet getTransform()
	{
		int nd = getScale().length;
		if( nd == 2 )
			return new Scale2D( getScale() );
		else if( nd == 3 )
			return new Scale3D( getScale() );
		else
			return new Scale( getScale() );
	}

	@Override
	public ScaleCoordinateTransformation copy() {

		return new ScaleCoordinateTransformation(this.scale);
	}

	@Override
	public ScaleCoordinateTransformation copy(final TransformOptions<?> copyParams) {

		return copy();
	}
}

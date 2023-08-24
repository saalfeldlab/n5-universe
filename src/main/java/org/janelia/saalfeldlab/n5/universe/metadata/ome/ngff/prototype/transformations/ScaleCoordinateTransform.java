package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ScaleGet;

public class ScaleCoordinateTransform extends AbstractLinearCoordinateTransform<ScaleGet,double[]> implements InvertibleCoordinateTransform<ScaleGet> {

	public double[] scale;

	public transient ScaleGet transform;

	public ScaleCoordinateTransform( final double[] scale ) {
		super("scale", null, null, null );
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( String name, 
			final double[] scale) {
		super("scale", name, null, null );
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( String name, 
			final String[] inputAxes, String[] outputAxes,
			final double[] scale) {
		super("scale", name, null, inputAxes, outputAxes );
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final double[] scale) {
		super("scale", name, inputSpace, outputSpace );
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
		 final N5Reader n5, final String path ) {
		super("scale", name, path, inputSpace, outputSpace );
		this.scale = getParameters(n5);
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final String path ) {
		super("scale", name, path, inputSpace, outputSpace );
		this.scale = null;
	}

	@Override
	public ScaleGet buildTransform( double[] parameters )
	{
		this.scale = parameters;
		if( parameters.length == 2 )
			transform = new Scale2D(parameters);
		else if( parameters.length == 3 )
			transform = new Scale3D(parameters);
		else
			transform = new Scale(parameters);

		return (ScaleGet) transform;
	}

	@Override
	public ScaleGet getTransform() {
		if( transform == null && scale != null )
			buildTransform(scale);

		return (ScaleGet) transform;
	}

	@Override
	public double[] getParameters(N5Reader n5) {
		return getDoubleArray( n5 , getParameterPath() );
	}
	
}

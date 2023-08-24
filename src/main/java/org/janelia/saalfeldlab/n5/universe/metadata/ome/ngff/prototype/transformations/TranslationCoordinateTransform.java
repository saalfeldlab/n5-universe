package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.realtransform.TranslationGet;

public class TranslationCoordinateTransform extends AbstractLinearCoordinateTransform<TranslationGet,double[]> 
	implements InvertibleCoordinateTransform<TranslationGet> {

	public double[] translation;

	public transient AffineGet transform;

	public TranslationCoordinateTransform( String name, 
			final double[] translation) {
		super("translation", name, null, null );
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, 
			final String[] inputAxes, String[] outputAxes,
			final double[] translation) {
		super("translation", name, null, inputAxes, outputAxes );
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final double[] translation) {
		super("translation", name, inputSpace, outputSpace );
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final N5Reader n5, final String path) {
		super("translation", name, path, inputSpace, outputSpace );
		this.translation = getParameters( n5 );
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final String path) {
		super("translation", name, path, inputSpace, outputSpace );
		this.translation = null;
	}

	@Override
	public TranslationGet buildTransform( double[] parameters )
	{
		this.translation = parameters;
		if( parameters.length == 2 )
			transform = new Translation2D(parameters);
		else if( parameters.length == 3 )
			transform = new Translation3D(parameters);
		else
			transform = new Translation(parameters);

		return (TranslationGet) transform;
	}

	@Override
	public TranslationGet getTransform() {
		if( transform == null && translation != null )
			buildTransform(translation);

		return (TranslationGet) transform;
	}

	@Override
	public double[] getParameters(N5Reader n5) {
		return getDoubleArray( n5 , getParameterPath() );
	}

	
}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.serialization.N5Annotations;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.realtransform.TranslationGet;

@NameConfig.Name("translation")
public class TranslationCoordinateTransform extends AbstractLinearCoordinateTransform<TranslationGet,double[]>
	implements InvertibleCoordinateTransform<TranslationGet> {

	public static final String TYPE = "translation";

	@N5Annotations.ReverseArray
	@NameConfig.Parameter()
	public double[] translation;

	public transient AffineGet transform;

	private TranslationCoordinateTransform() {
		// for serialization
		super(TYPE);
	}

	public TranslationCoordinateTransform( final double[] translation) {
		this("", translation);
	}

	public TranslationCoordinateTransform( String name,
			final double[] translation) {
		super(TYPE, name, null, null );
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name,
			final String[] inputAxes, String[] outputAxes,
			final double[] translation) {
		super(TYPE, name, null, inputAxes, outputAxes );
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final double[] translation) {
		super(TYPE, name, inputSpace, outputSpace );
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final N5Reader n5, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.translation = getParameters( n5 );
		buildTransform( translation );
	}

	public TranslationCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final String path) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.translation = null;
	}

	public TranslationCoordinateTransform(TranslationCoordinateTransform other) {
		super(other);
		this.translation = other.translation;
	}

	public TranslationCoordinateTransform(TranslationCoordinateTransform other, String[] inputAxes, String[] outputAxes) {
		super(other, inputAxes, outputAxes);
		this.translation = other.translation;
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

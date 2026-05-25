package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ScaleGet;

public class ScaleCoordinateTransform extends AbstractParametrizedTransform<ScaleGet,double[]> implements InvertibleCoordinateTransform<ScaleGet> {

	public static final String TYPE = "scale";

	public double[] scale;

	public transient ScaleGet transform;

	private ScaleCoordinateTransform() {
		// for serialization
		super(TYPE);
	}

	public ScaleCoordinateTransform( final double[] scale ) {
		super(TYPE);
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform(String name,
			final double[] scale) {
		super(TYPE, name);
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform(String name,
			final int[] inputAxes, int[] outputAxes,
			final double[] scale) {
		super(TYPE, name, null, inputAxes, outputAxes );
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( String name, final String inputSpace, final String outputSpace,
			final double[] scale) {
		super(TYPE, name, inputSpace, outputSpace );
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( String name, final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final double[] scale) {
		super(TYPE, name, inputRef, outputRef);
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
		 final N5Reader n5, final String path ) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.scale = getParameters(n5);
		buildTransform( scale );
	}

	public ScaleCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final String path ) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.scale = null;
	}

	public ScaleCoordinateTransform( final String name, final String inputSpace, final String outputSpace, final String path,
			final double[] scale) {
		super(TYPE, name, path, inputSpace, outputSpace );
		this.scale = scale;
	}

	public ScaleCoordinateTransform(ScaleCoordinateTransform other, final double[] scale) {

		super(other);
		this.scale = scale;
	}

	public ScaleCoordinateTransform(ScaleCoordinateTransform other) {

		super(other);
		this.scale = other.scale;
	}

	public ScaleCoordinateTransform(ScaleCoordinateTransform other, int[] inputAxes, int[] outputAxes) {

		super(other, inputAxes, outputAxes);
		this.scale = other.scale;
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

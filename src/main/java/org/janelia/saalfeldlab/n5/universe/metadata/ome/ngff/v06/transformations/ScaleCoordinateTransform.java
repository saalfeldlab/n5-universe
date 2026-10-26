package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

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

	public ScaleCoordinateTransform(final double[] scale ) {
		this(new ScaleOptions().withScale(scale));
	}

	public ScaleCoordinateTransform(ScaleOptions options) {
		super(TYPE, options);
		this.scale = options.scale;
		if (this.scale != null)
			buildTransform(this.scale.clone());
	}

	protected ScaleCoordinateTransform(ScaleCoordinateTransform other, TransformOptions<?> copyParams) {

		super(other, copyParams);
		final double[] newScale = (copyParams instanceof ScaleOptions && ((ScaleOptions) copyParams).isScaleSet())
				? ((ScaleOptions) copyParams).getScale()
				: other.scale;

		if (newScale != null)
			buildTransform(newScale.clone());
	}

	public static class ScaleOptions extends TransformOptions<ScaleOptions> {

		private boolean scaleSet = false;
		private double[] scale;
		
		public ScaleOptions withScale(double[] scale){ 
			this.scale = scale;
			this.scaleSet = true;
			return this;
		}
		
		public boolean isScaleSet() {
			return scaleSet;
		}

		public double[] getScale() {
			return scale;
		}
	}

	public static ScaleOptions options() {
		return new ScaleOptions();
	}

	@Override
	public ScaleCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public ScaleCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new ScaleCoordinateTransform(this, copyParams);
	}

	@Override
	protected double[] readParameters(final N5Reader n5, final String path, final boolean reverse) {

		return CoordinateTransformAdapter.doubleArraySupplier(n5, path, reverse);
	}

	@Override
	public ScaleGet buildTransform( double[] parameters )
	{
		this.scale = parameters;
		if( parameters == null )
			return null;
		else if( parameters.length == 2 )
			transform = new Scale2D(parameters);
		else if( parameters.length == 3 )
			transform = new Scale3D(parameters);
		else
			transform = new Scale(parameters);

		return (ScaleGet) transform;
	}

	@Override
	public ScaleGet getTransform() {

		if (transform != null)
			return (ScaleGet) transform;
		else if (scale != null)
			return buildTransform(scale);
		else if (path != null)
			return buildTransform(requireParameters());

		return null;
	}

}

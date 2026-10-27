package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.realtransform.TranslationGet;

public class TranslationCoordinateTransform extends AbstractParametrizedTransform<TranslationGet,double[]>
	implements InvertibleCoordinateTransform<TranslationGet> {

	public static final String TYPE = "translation";

	public double[] translation;

	public transient AffineGet transform;

	private TranslationCoordinateTransform() {
		// for serialization
		super(TYPE);
	}

	public TranslationCoordinateTransform(final double[] translation) {
		this(new Options().withTranslation(translation));
	}

	private TranslationCoordinateTransform(Options options) {
		super(TYPE, options);
		this.translation = options.translation;
		if (this.translation != null)
			buildTransform(this.translation.clone());
	}

	protected TranslationCoordinateTransform(TranslationCoordinateTransform other, TransformOptions<?> copyParams) {

		super(other, copyParams);
		final double[] newTranslation = (copyParams instanceof Options
				&& ((Options) copyParams).isTranslationSet())
						? ((Options) copyParams).getTranslation()
						: other.translation;

		if (newTranslation != null)
			buildTransform(newTranslation.clone());
	}

	public static class Options extends TransformOptions<Options> {

		private boolean translationSet = false;
		private double[] translation;

		public Options withTranslation(double[] translation) {
			this.translation = translation;
			this.translationSet = true;
			return this;
		}

		public boolean isTranslationSet() {
			return translationSet;
		}

		public double[] getTranslation() {
			return translation;
		}

		public TranslationCoordinateTransform build() {
			return new TranslationCoordinateTransform(this);
		}
	}

	public static Options options() {
		return new Options();
	}

	@Override
	public TranslationCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public TranslationCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new TranslationCoordinateTransform(this, copyParams);
	}

	@Override
	protected double[] readParameters(final N5Reader n5, final String path, final boolean reverse) {

		return CoordinateTransformAdapter.doubleArraySupplier(n5, path, reverse);
	}

	@Override
	public TranslationGet buildTransform( double[] parameters )
	{
		this.translation = parameters;
		if( parameters == null )
			return null;
		else if( parameters.length == 2 )
			transform = new Translation2D(parameters);
		else if( parameters.length == 3 )
			transform = new Translation3D(parameters);
		else
			transform = new Translation(parameters);

		return (TranslationGet) transform;
	}

	@Override
	public TranslationGet getTransform() {

		if (transform != null)
			return (TranslationGet) transform;
		else if (translation != null)
			return buildTransform(translation);
		else if (path != null)
			return buildTransform(requireParameters());

		return null;
	}

}

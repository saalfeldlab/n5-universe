package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractCoordinateTransform.TransformOptions;

import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.realtransform.TranslationGet;

public class TranslationCoordinateTransformation extends AbstractCoordinateTransformation<TranslationGet>
{
	public static final String TYPE = "translation";
	
	protected double[] translation;

	public TranslationCoordinateTransformation(final double[] translation) {
		super(TYPE);
		this.translation = translation;
	}

	protected TranslationCoordinateTransformation(TranslationCoordinateTransformation other) {
		this(other.translation);
	}

	protected TranslationCoordinateTransformation(TranslationCoordinateTransformation other, TransformOptions<?> copyParams) {
		super(TYPE);
		this.translation = other.translation == null ? null : other.translation.clone();
	}

	public double[] getTranslation() {
		return translation;
	}

	@Override
	public TranslationGet getTransform()
	{
		int nd = getTranslation().length;
		if( nd == 2 )
			return new Translation2D( getTranslation() );
		else if( nd == 3 )
			return new Translation3D( getTranslation() );
		else
			return new Translation( getTranslation() );
	}

	@Override
	public TranslationCoordinateTransformation copy() {
		return new TranslationCoordinateTransformation(this.translation);
	}

	@Override
	public TranslationCoordinateTransformation copy(final TransformOptions<?> copyParams) {
		return copy();
	}
}

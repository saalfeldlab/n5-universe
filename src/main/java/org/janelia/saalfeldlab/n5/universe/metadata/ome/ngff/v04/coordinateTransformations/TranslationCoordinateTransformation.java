package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

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

	public TranslationCoordinateTransformation(TranslationCoordinateTransformation other) {
		this(other.translation);
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
}

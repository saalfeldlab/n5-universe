package org.janelia.saalfeldlab.n5.universe.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.ScaleAndTranslation;

public interface SpatialModifiable<T extends SpatialMetadata> {

	public T modifySpatialTransform(final AffineGet relativeTransformation);

	public default T modifySpatialTransform(final double[] relativeScale, final double[] relativeTranslation) {

		return modifySpatialTransform(new ScaleAndTranslation(relativeScale, relativeTranslation));
	}

}

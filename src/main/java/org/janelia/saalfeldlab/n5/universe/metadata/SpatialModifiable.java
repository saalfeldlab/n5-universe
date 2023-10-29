package org.janelia.saalfeldlab.n5.universe.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.ScaleAndTranslation;

public interface SpatialModifiable<T extends SpatialMetadata> {

	public T modifySpatialTransform(final String newPath, final AffineGet relativeTransformation);

	public default T modifySpatialTransform(final String newPath, final double[] relativeScale, final double[] relativeTranslation) {

		return modifySpatialTransform(newPath, new ScaleAndTranslation(relativeScale, relativeTranslation));
	}

}

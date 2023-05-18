package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import net.imglib2.realtransform.AffineGet;

/**
 * An OME-NGFF coordinate transformation.
 * <p>
 * In Version 0.4, every transform type returns an {@link AffineGet}.
 *
 * @param <T> The transform type 
 */
public interface CoordinateTransformation<T extends AffineGet>
{
	public T getTransform();

	public String getType();

	public static CoordinateTransformation<?> create(CoordinateTransformation<?> ct) {
		if (ct instanceof CoordinateTransformation) {
			return (CoordinateTransformation<?>) ct;
		}

		switch (ct.getType()) {
		case IdentityCoordinateTransformation.TYPE:
			return new IdentityCoordinateTransformation();
		case ScaleCoordinateTransformation.TYPE:
			return new ScaleCoordinateTransformation((ScaleCoordinateTransformation) ct);
		case TranslationCoordinateTransformation.TYPE:
			return new TranslationCoordinateTransformation((TranslationCoordinateTransformation) ct);

		}
		return null;
	}
}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.InvertibleRealTransform;

public interface InvertibleCoordinateTransform<T extends InvertibleRealTransform> extends CoordinateTransform<T> {

	/**
	 * @param n5 the reader
	 * @return the transform
	 * @deprecated use {@link #getInvertibleTransform()}, see {@link CoordinateTransform#getTransform(N5Reader)}
	 */
	@Deprecated
	public default InvertibleRealTransform getInvertibleTransform(N5Reader n5) {
		ParameterBinding.bindUnbound(this, n5);
		return getInvertibleTransform();
	}

	public default InvertibleRealTransform getInvertibleTransform() {
		return getTransform();
	}

}

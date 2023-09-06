package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.InvertibleRealTransform;

public interface InvertibleCoordinateTransform<T extends InvertibleRealTransform> extends RealCoordinateTransform<T> {

	public default InvertibleRealTransform getInvertibleTransform(N5Reader n5) {
		return getTransform(n5).inverse();
	}

	public default InvertibleRealTransform getInvertibleTransform() {
		return getTransform().inverse();
	}

}

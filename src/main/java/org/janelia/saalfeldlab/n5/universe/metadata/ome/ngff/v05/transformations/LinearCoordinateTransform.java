package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import net.imglib2.realtransform.AffineGet;

public interface LinearCoordinateTransform<T extends AffineGet> extends RealCoordinateTransform<T>, InvertibleCoordinateTransform<T> {

	@Override
	public T getTransform();
}

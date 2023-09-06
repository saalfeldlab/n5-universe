package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import net.imglib2.realtransform.AffineGet;

public interface LinearCoordinateTransform<T extends AffineGet> extends RealCoordinateTransform<T> {

	@Override
	public T getTransform();
}

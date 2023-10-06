package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import net.imglib2.realtransform.RealTransform;

public interface RealCoordinateTransform<T extends RealTransform> extends CoordinateTransform<T> {

	@Override
	public T getTransform();
	
//	public default RealCoordinate apply( final RealCoordinate pt ) {
//
//		T t = getTransform();
//
//		RealCoordinate out = new RealCoordinate(t.numTargetDimensions());
//		// this needs to work on subspaces correctly
//
//		return out;
//	}
}

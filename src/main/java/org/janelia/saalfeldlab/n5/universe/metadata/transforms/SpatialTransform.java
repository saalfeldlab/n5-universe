package org.janelia.saalfeldlab.n5.universe.metadata.transforms;

import net.imglib2.realtransform.RealTransform;

public interface SpatialTransform {

	public RealTransform getTransform();
}

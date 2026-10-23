package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import net.imglib2.realtransform.RealTransform;

public interface ParametrizedTransform<T extends RealTransform,P> extends CoordinateTransform<T> {

	public String getParameterPath();

	public T buildTransform( P parameters );

	public P getParameters();

	public void resolveAbsolutePath(final String groupPath);
}

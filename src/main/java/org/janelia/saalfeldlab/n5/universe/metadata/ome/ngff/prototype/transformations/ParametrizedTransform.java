package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.RealTransform;

public interface ParametrizedTransform<T extends RealTransform,P> extends CoordinateTransform<T> {

	public String getParameterPath();
	
	@Override
	public default T getTransform( final N5Reader n5 )
	{
		T t = getTransform();
		if( t != null )
			return t;
		else
		{
			return buildTransform(getParameters(n5));
		}
	}

	public T buildTransform( P parameters );

	public P getParameters( final N5Reader n5 );
}

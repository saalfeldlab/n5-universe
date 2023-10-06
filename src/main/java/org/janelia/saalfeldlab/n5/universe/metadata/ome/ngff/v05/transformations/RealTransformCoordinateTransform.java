package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

public class RealTransformCoordinateTransform<T extends RealTransform> extends AbstractCoordinateTransform<T> {

	public transient T transform;

	public RealTransformCoordinateTransform( String name, final String[] inputAxes, String[] outputAxes, final T transform ) {
		super("realtransform", name, inputAxes, outputAxes );
		this.transform = transform;
	}

	public RealTransformCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
			final T transform ) {
		super("realtransform", name, inputSpace, outputSpace );
		this.transform = transform;
	}

	@SuppressWarnings( "unchecked" )
	public RealTransformCoordinateTransform(final String name, final String inputSpace, final String outputSpace) {
		super("realtransform", name, inputSpace, outputSpace );
		this.transform = ( T ) new RealTransformSequence(); // identity
	}

	@Override
	public T getTransform() {
		return transform;
	}
	
}

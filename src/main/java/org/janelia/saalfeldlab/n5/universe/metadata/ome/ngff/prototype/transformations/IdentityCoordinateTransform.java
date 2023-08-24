package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;

public class IdentityCoordinateTransform extends AbstractCoordinateTransform<InvertibleRealTransform> 
	implements RealCoordinateTransform<InvertibleRealTransform> , InvertibleCoordinateTransform<InvertibleRealTransform> {

	public IdentityCoordinateTransform( final String name, final String inputSpace, final String outputSpace ) {
		super("identity", name, inputSpace, outputSpace );
	}

	public IdentityCoordinateTransform( final String name, final String[] inputAxes, final String[] outputAxes ) {
		super("identity", name, inputAxes, outputAxes );
	}

	@Override
	public InvertibleRealTransform getTransform()
	{
		// an empty RealTransformSequence is the identity
		return new InvertibleRealTransformSequence();
	}
	
}

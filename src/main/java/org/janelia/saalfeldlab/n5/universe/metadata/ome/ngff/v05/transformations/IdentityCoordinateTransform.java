package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;

public class IdentityCoordinateTransform extends AbstractCoordinateTransform<InvertibleRealTransform>
	implements InvertibleCoordinateTransform<InvertibleRealTransform> {

	public static final String TYPE = "identity";

	public IdentityCoordinateTransform() {
		super(TYPE);
	}

	public IdentityCoordinateTransform( final String name, final String input, final String output ) {
		super(TYPE, name, input, output );
	}

	public IdentityCoordinateTransform( final String name, final int[] inputAxes, final int[] outputAxes ) {
		super(TYPE, name, inputAxes, outputAxes );
	}

	public IdentityCoordinateTransform(IdentityCoordinateTransform other) {

		super(other);
	}

	public IdentityCoordinateTransform(IdentityCoordinateTransform other, int[] inputAxes, int[] outputAxes) {

		super(other, inputAxes, outputAxes);
	}

	@Override
	public InvertibleRealTransform getTransform()
	{
		// an empty RealTransformSequence is the identity
		return new InvertibleRealTransformSequence();
	}

}

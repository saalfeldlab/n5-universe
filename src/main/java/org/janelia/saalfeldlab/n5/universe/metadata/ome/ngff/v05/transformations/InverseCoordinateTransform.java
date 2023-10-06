package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import net.imglib2.realtransform.RealTransform;

public class InverseCoordinateTransform<T extends RealTransform,C extends CoordinateTransform<T>> extends AbstractCoordinateTransform<T> {

	protected C transform;

	public InverseCoordinateTransform( final String name, final C ct ) {
		// input and output spaces / axes must be swapped
		super( "inverse_of", name, ct.getOutput(), ct.getInput());
		super.inputAxes = ct.getOutputAxes();
		super.outputAxes = ct.getInputAxes();
		this.transform = ct;
	}

	public InverseCoordinateTransform( final C ct ) {
		// input and output spaces must be swapped
		this( "inverse_of-" + ct.getName(), ct );
	}

	@Override
	public T getTransform() {
		return transform.getTransform();
	}
}

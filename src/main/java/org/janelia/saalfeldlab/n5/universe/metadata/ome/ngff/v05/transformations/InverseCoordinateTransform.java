package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.realtransform.RealTransform;

@NameConfig.Name("inverseOf")
public class InverseCoordinateTransform<T extends RealTransform, C extends CoordinateTransform<T>> extends AbstractCoordinateTransform<T> {
	
	public static final String TYPE = "inverseOf";

	@NameConfig.Parameter
	protected C transform;

	protected InverseCoordinateTransform() {
		// for serialization
	}

	public InverseCoordinateTransform(
			final String name,
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final C transform ) {
		super(TYPE, name, inputSpace, outputSpace);
		this.transform = transform;
	}

	public InverseCoordinateTransform(
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final C transform ) {
		this(null, inputSpace, outputSpace, transform);
	}

	public InverseCoordinateTransform( final String name, final C ct ) {
		// input and output spaces / axes must be swapped
		super(TYPE, name, ct.getOutput(), ct.getInput());
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
	
	@Override
	public T getTransform(final N5Reader n5) {
		return transform.getTransform(n5);
	}
}

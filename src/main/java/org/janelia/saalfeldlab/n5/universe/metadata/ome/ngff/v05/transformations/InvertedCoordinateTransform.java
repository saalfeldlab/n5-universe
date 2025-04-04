package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;

import net.imglib2.realtransform.InvertibleRealTransform;

public class InvertedCoordinateTransform<T extends InvertibleRealTransform, C extends InvertibleCoordinateTransform<T>> extends AbstractCoordinateTransform<T> 
	implements InvertibleCoordinateTransform<T> {

	public static final String TYPE = "inverted";

	protected C transform;

	protected InvertedCoordinateTransform() {
		// for serialization
	}

	public InvertedCoordinateTransform(
			final String name,
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final C transform ) {
		super(TYPE, name, inputSpace, outputSpace);
		this.transform = transform;
	}

	public InvertedCoordinateTransform(
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final C transform ) {
		this(null, inputSpace, outputSpace, transform);
	}

	public InvertedCoordinateTransform( final String name, final C ct ) {
		// input and output spaces / axes must be swapped
		super(TYPE, name, ct.getOutput(), ct.getInput());
		super.inputAxes = ct.getOutputAxes();
		super.outputAxes = ct.getInputAxes();
		this.transform = ct;
	}

	public InvertedCoordinateTransform( final C ct ) {
		// input and output spaces must be swapped
		this( "inverted-" + ct.getName(), ct );
	}

	@Override
	public T getTransform() {
		return (T)transform.getTransform().inverse();
	}

	@Override
	public T getTransform(final N5Reader n5) {
		return (T)transform.getTransform(n5).inverse();
	}

	@Override
	public InvertibleRealTransform getInvertibleTransform(N5Reader n5) {
		return getTransform(n5);
	}

	@Override
	public InvertibleRealTransform getInvertibleTransform() {
		return getTransform();
	}
}

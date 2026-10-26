package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;

import net.imglib2.realtransform.InvertibleRealTransform;

public class InverseCoordinateTransform<T extends InvertibleRealTransform, C extends InvertibleCoordinateTransform<T>> extends AbstractCoordinateTransform<T> 
	implements InvertibleCoordinateTransform<T> {

	public static final String TYPE = "inverted";

	protected C transform;

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
		this.transform = ct;
	}

	public InverseCoordinateTransform( final C ct ) {
		// input and output spaces must be swapped
		this( "inverted-" + ct.getName(), ct );
	}
	
	@SuppressWarnings("unchecked")
	protected InverseCoordinateTransform(final InverseCoordinateTransform<T, C> other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		// the copy of an invertible transform is invertible, see the covariant copy methods
		this.transform = (C)other.transform.copy();
	}

	@Override
	public InverseCoordinateTransform<T, C> copy() {

		return copy(new BasicOptions());
	}

	@Override
	public InverseCoordinateTransform<T, C> copy(final TransformOptions<?> copyParams) {

		return new InverseCoordinateTransform<>(this, copyParams);
	}

	public C getWrappedCoordinateTransform() {
		return transform;
	}

	@Override
	public T getTransform() {
		return (T)getWrappedCoordinateTransform().getTransform().inverse();
	}

	@Override
	public InvertibleRealTransform getInvertibleTransform() {
		return getTransform();
	}
}

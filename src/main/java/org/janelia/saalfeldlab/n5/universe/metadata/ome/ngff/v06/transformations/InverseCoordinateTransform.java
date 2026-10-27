package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.InvertibleRealTransform;

public class InverseCoordinateTransform<T extends InvertibleRealTransform, C extends InvertibleCoordinateTransform<T>> extends AbstractCoordinateTransform<T> 
	implements InvertibleCoordinateTransform<T> {

	public static final String TYPE = "inverted";

	protected C transform;

	private InverseCoordinateTransform( final InverseOptions<T, C> options ) {
		super(TYPE, options.withDefaults());
		this.transform = options.transform;
	}

	/**
	 * The inverse of {@code ct}, which has the input and output of {@code ct} swapped,
	 * and a name that says it is inverted.
	 *
	 * @param ct the transform to invert
	 */
	public InverseCoordinateTransform( final C ct ) {
		this( new InverseOptions<T, C>().withTransform(ct) );
	}

	/**
	 * Options for an inverse transform. If the name, input, or output are not set, they are those of
	 * the inverse of the {@link #withTransform transform}: its input and output swapped, and the name
	 * of the transform with "inverted-" in front.
	 *
	 * @param <T> the type of the transform
	 * @param <C> the type of the coordinate transform
	 */
	public static class InverseOptions<T extends InvertibleRealTransform, C extends InvertibleCoordinateTransform<T>>
			extends TransformOptions<InverseOptions<T, C>> {

		private C transform;

		public InverseOptions<T, C> withTransform( final C transform ) {
			this.transform = transform;
			return this;
		}

		private InverseOptions<T, C> withDefaults() {
			if (transform != null) {
				if (!isNameSet())
					withName("inverted-" + transform.getName());
				if (!isInputSet())
					withInput(transform.getOutput());
				if (!isOutputSet())
					withOutput(transform.getInput());
			}
			return this;
		}

		public InverseCoordinateTransform<T, C> build() {
			return new InverseCoordinateTransform<>(this);
		}
	}

	public static <T extends InvertibleRealTransform, C extends InvertibleCoordinateTransform<T>> InverseOptions<T, C> options() {
		return new InverseOptions<>();
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

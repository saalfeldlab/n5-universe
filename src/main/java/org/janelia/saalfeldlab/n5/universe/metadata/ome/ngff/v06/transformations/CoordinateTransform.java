package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.Objects;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractCoordinateTransform.BasicOptions;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractCoordinateTransform.TransformOptions;

import net.imglib2.realtransform.RealTransform;

public interface CoordinateTransform<T extends RealTransform> {

	public final static String KEY = "coordinateTransformations";

	public final static String TYPE_KEY = "type";
	public final static String NAME_KEY = "name";
	public final static String INPUT_KEY = "input";
	public final static String OUTPUT_KEY = "output";
	public final static String INPUT_AXES_KEY = "inputAxes";
	public final static String OUTPUT_AXES_KEY = "outputAxes";

	/**
	 * Builds this transform. Transforms whose parameters are stored in arrays read them from the
	 * reader they are bound to, see {@link CoordinateTransforms}.
	 *
	 * @return the transform
	 * @throws org.janelia.saalfeldlab.n5.N5Exception if parameters are stored in an array, but this
	 *             transform is not bound to a reader
	 */
	public T getTransform();

	/**
	 * Binds any parametrized transform in this one that is not bound yet to {@code n5}, then builds
	 * this transform.
	 *
	 * @param n5 the reader
	 * @return the transform
	 * @deprecated read transforms with {@link CoordinateTransforms} or the ome-ngff parsers, which
	 *             bind them, and use {@link #getTransform()}
	 */
	@Deprecated
	public default T getTransform( final N5Reader n5 ) {
		ParameterBinding.bindUnbound(this, n5);
		return getTransform();
	}

	/**
	 * Copies this transform, replacing the fields set in {@code copyParams}.
	 * <p>
	 * The copy does not share mutable state with this transform, except for
	 * data that can not be copied (e.g. a displacement field's values).
	 *
	 * @param copyParams the fields to replace
	 * @return the copy
	 * @throws UnsupportedOperationException if this transform does not support replacing one of the set fields
	 */
	public CoordinateTransform<T> copy(final TransformOptions<?> copyParams);

	/**
	 * @return a copy of this transform
	 * @see #copy(TransformOptions)
	 */
	public default CoordinateTransform<T> copy() {
		return copy(new BasicOptions());
	}

	public String getName();

	public String getType();

	public OmeNgffReference getInput();

	public OmeNgffReference getOutput();

	public void setInput(final CoordinateSystem inputCoordinateSystem);

	public void setOutput(final CoordinateSystem outputCoordinateSystem);

	/**
	 * Is the other {@code other} the same graph edge as this transform.
	 * <p>
	 * Two transform are the same if they have identical same {@code type},
	 * {@code name}, and (qualified) {@code input}/{@code output}. This
	 * method returns true even if they have different parameters.
	 *
	 * @param other
	 *            the transform to compare against
	 * @return whether {@code other} declares the same edge as this transform
	 */
	public default boolean sameEdge(final CoordinateTransform<?> other) {

		if (other == null)
			return false;
		if (!Objects.equals(getType(), other.getType()))
			return false;
		if (!Objects.equals(getName(), other.getName()))
			return false;

		final String in = getInput() == null ? null : getInput().getQualifiedName();
		final String otherIn = other.getInput() == null ? null : other.getInput().getQualifiedName();
		if (!Objects.equals(in, otherIn))
			return false;

		final String out = getOutput() == null ? null : getOutput().getQualifiedName();
		final String otherOut = other.getOutput() == null ? null : other.getOutput().getQualifiedName();
		return Objects.equals(out, otherOut);
	}

	/**
	 * Returns the inverse of {@code ct}, or null if it is not invertible. A
	 * {@link GeneralAffineCoordinateTransform} is invertible if its matrix is
	 * square.
	 *
	 * @param ct the transform
	 * @return the inverse, or null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static CoordinateTransform<?> inverseOf(final CoordinateTransform<?> ct) {

		try {
			if (ct instanceof InvertibleCoordinateTransform)
				return new InverseCoordinateTransform((InvertibleCoordinateTransform)ct);
			else if (ct instanceof GeneralAffineCoordinateTransform)
				return ((GeneralAffineCoordinateTransform)ct).tryInverse();
			else if (ct instanceof SequenceCoordinateTransform)
				return ((SequenceCoordinateTransform)ct).inverse();
			else if (ct instanceof ByDimensionCoordinateTransform)
				return ((ByDimensionCoordinateTransform)ct).inverse();
		} catch (final Exception e) {}

		return null;
	}

}

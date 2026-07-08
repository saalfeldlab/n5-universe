package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.Objects;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.RealTransform;

public interface CoordinateTransform<T extends RealTransform> {

	public final static String KEY = "coordinateTransformations";

	public final static String TYPE_KEY = "type";
	public final static String NAME_KEY = "name";
	public final static String INPUT_KEY = "input";
	public final static String OUTPUT_KEY = "output";
	public final static String INPUT_AXES_KEY = "inputAxes";
	public final static String OUTPUT_AXES_KEY = "outputAxes";

	public T getTransform();

	public default T getTransform( final N5Reader n5 ) {
		return getTransform();
	}

	public String getName();

	public String getType();

	public OmeNgffReference getInput();

	public OmeNgffReference getOutput();

	public void setInput(final CoordinateSystem inputCoordinateSystem);

	public void setOutput(final CoordinateSystem outputCoordinateSystem);

	public int[] getInputAxes();

	public int[] getOutputAxes();

	public void setInputAxes(final int[] inputAxes);

	public void setOutputAxes(final int[] outputAxes);

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

	public static CoordinateTransform<?> create(CoordinateTransform<?> ct) {
		if (ct instanceof CoordinateTransform) {
			return (CoordinateTransform<?>) ct;
		}

		// TODO finish for the rest of the types
		switch (ct.getType()) {
		case IdentityCoordinateTransform.TYPE:
			return new IdentityCoordinateTransform();
		case ScaleCoordinateTransform.TYPE:
			return new ScaleCoordinateTransform((ScaleCoordinateTransform) ct);
		case TranslationCoordinateTransform.TYPE:
			return new TranslationCoordinateTransform((TranslationCoordinateTransform) ct);

		}
		return null;
	}

}

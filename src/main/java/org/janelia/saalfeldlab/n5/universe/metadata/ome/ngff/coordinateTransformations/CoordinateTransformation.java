package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import net.imglib2.realtransform.AffineGet;

/**
 * An OME-NGFF coordinate transformation.
 * <p>
 * In Version 0.4, every transform type returns an {@link AffineGet}.
 *
 * @param <T> The transform type 
 */
public interface CoordinateTransformation<T extends AffineGet> extends CoordinateTransform<T>
{
	public T getTransform();

	public String getType();

	public static CoordinateTransformation<?> create(CoordinateTransformation<?> ct) {
		if (ct instanceof CoordinateTransformation) {
			return (CoordinateTransformation<?>) ct;
		}

		switch (ct.getType()) {
		case IdentityCoordinateTransformation.TYPE:
			return new IdentityCoordinateTransformation();
		case ScaleCoordinateTransformation.TYPE:
			return new ScaleCoordinateTransformation((ScaleCoordinateTransformation) ct);
		case TranslationCoordinateTransformation.TYPE:
			return new TranslationCoordinateTransformation((TranslationCoordinateTransformation) ct);

		}
		return null;
	}

	default String getName() {
		return "";
	}

	@Override
	default OmeNgffReference getInput() {
		return null;
	}

	@Override
	default OmeNgffReference getOutput() {
		return null;
	}

	@Override
	default int[] getInputAxes() {
		return null;
	}

	@Override
	default int[] getOutputAxes() {
		return null;
	}

	@Override
	default void setInput(CoordinateSystem inputCoordinateSystem) {
		// no op
	}

	@Override
	default void setOutput(CoordinateSystem outputCoordinateSystem) {
		// no op
	}

	@Override
	default void setInputAxes(int[] inputAxes) {
		// no op
	}

	@Override
	default void setOutputAxes(int[] outputAxes) {
		// no op
	}

}

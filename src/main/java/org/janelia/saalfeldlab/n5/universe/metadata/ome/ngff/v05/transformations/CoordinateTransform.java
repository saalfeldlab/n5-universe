package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.realtransform.RealTransform;

@NameConfig.Prefix("coordinate-transformation")
public interface CoordinateTransform<T extends RealTransform> {

	public final static String KEY = "coordinateTransformations";

	public final static String TYPE_KEY = "type";
	public final static String NAME_KEY = "name";
	public final static String INPUT_KEY = "input";
	public final static String OUTPUT_KEY = "output";

	public T getTransform();

	public default T getTransform( final N5Reader n5 ) {
		return getTransform();
	}

	public String getName();

	public String getType();

	public String getInput();

	public String getOutput();

	public String[] getInputAxes();

	public String[] getOutputAxes();

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

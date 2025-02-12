package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.realtransform.ExplicitInvertibleRealTransform;

@NameConfig.Name("bijection")
public class BijectionCoordinateTransform extends AbstractCoordinateTransform<ExplicitInvertibleRealTransform> 
	implements InvertibleCoordinateTransform<ExplicitInvertibleRealTransform> {

	public static final String TYPE = "bijection";

	@NameConfig.Parameter
	private final CoordinateTransform<?> forward;

	@NameConfig.Parameter
	private final CoordinateTransform<?> inverse;

	protected BijectionCoordinateTransform() {
		// for serialization
		super(TYPE);
		forward = null;
		inverse = null;
	}

	public BijectionCoordinateTransform(final String name, final String inputSpace, final String outputSpace,
			final CoordinateTransform<?> forward, final CoordinateTransform<?> inverse) {
		super(TYPE, name, inputSpace, outputSpace);
		this.forward = forward;
		this.inverse = inverse;
	}

	public CoordinateTransform<?> getForward() {
		return forward;
	}

	public CoordinateTransform<?> getInverse() {
		return inverse;
	}
	
	@Override
	public ExplicitInvertibleRealTransform getTransform(final N5Reader n5) {
		return new ExplicitInvertibleRealTransform(forward.getTransform(n5), inverse.getTransform(n5));
	}

	@Override
	public ExplicitInvertibleRealTransform getTransform() {
		return new ExplicitInvertibleRealTransform(forward.getTransform(), inverse.getTransform());
	}

}

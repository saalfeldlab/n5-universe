package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.ExplicitInvertibleRealTransform;

public class BijectionCoordinateTransform extends AbstractCoordinateTransform<ExplicitInvertibleRealTransform> 
	implements InvertibleCoordinateTransform<ExplicitInvertibleRealTransform> {

	public static final String TYPE = "bijection";

	private final CoordinateTransform<?> forward;

	private final CoordinateTransform<?> inverse;

	protected BijectionCoordinateTransform() {
		// for serialization
		super(TYPE);
		forward = null;
		inverse = null;
	}

	public BijectionCoordinateTransform(final String name,
			final CoordinateSystem input, final CoordinateSystem output,
			final CoordinateTransform<?> forward, final CoordinateTransform<?> inverse) {
		super(TYPE, name, input, output);
		this.forward = forward;
		this.inverse = inverse;
	}

	public BijectionCoordinateTransform(final String name, final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final CoordinateTransform<?> forward, final CoordinateTransform<?> inverse) {
		super(TYPE, name, inputRef, outputRef);
		this.forward = forward;
		this.inverse = inverse;
	}

	public BijectionCoordinateTransform(final String name, final String inputSpace, final String outputSpace,
			final CoordinateTransform<?> forward, final CoordinateTransform<?> inverse) {
		super(TYPE, name, inputSpace, outputSpace);
		this.forward = forward;
		this.inverse = inverse;
	}

	protected BijectionCoordinateTransform(final BijectionCoordinateTransform other, final TransformOptions copyParams) {
		super(other, copyParams);
		this.forward = other.forward == null ? null : other.forward.copy();
		this.inverse = other.inverse == null ? null : other.inverse.copy();
	}

	@Override
	public BijectionCoordinateTransform copy() {

		return copy(new TransformOptions());
	}

	@Override
	public BijectionCoordinateTransform copy(final TransformOptions copyParams) {

		return new BijectionCoordinateTransform(this, copyParams);
	}

	public CoordinateTransform<?> getForward() {
		return forward;
	}

	public CoordinateTransform<?> getInverse() {
		return inverse;
	}

	@Override
	public ExplicitInvertibleRealTransform getTransform() {
		return new ExplicitInvertibleRealTransform(forward.getTransform(), inverse.getTransform());
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

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

	private BijectionCoordinateTransform(final BijectionOptions options) {
		super(TYPE, options);
		this.forward = options.forward;
		this.inverse = options.inverse;
	}

	protected BijectionCoordinateTransform(final BijectionCoordinateTransform other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		this.forward = other.forward == null ? null : other.forward.copy();
		this.inverse = other.inverse == null ? null : other.inverse.copy();
	}

	public static class BijectionOptions extends TransformOptions<BijectionOptions> {

		private CoordinateTransform<?> forward;
		private CoordinateTransform<?> inverse;

		public BijectionOptions withForward(final CoordinateTransform<?> forward) {
			this.forward = forward;
			return this;
		}

		public BijectionOptions withInverse(final CoordinateTransform<?> inverse) {
			this.inverse = inverse;
			return this;
		}

		public BijectionCoordinateTransform build() {
			return new BijectionCoordinateTransform(this);
		}
	}

	public static BijectionOptions options() {
		return new BijectionOptions();
	}

	@Override
	public BijectionCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public BijectionCoordinateTransform copy(final TransformOptions<?> copyParams) {

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

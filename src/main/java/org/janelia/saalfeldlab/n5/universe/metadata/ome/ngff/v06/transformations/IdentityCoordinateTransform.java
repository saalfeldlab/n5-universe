package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;

public class IdentityCoordinateTransform extends AbstractCoordinateTransform<InvertibleRealTransform>
	implements InvertibleCoordinateTransform<InvertibleRealTransform> {

	public static final String TYPE = "identity";

	public IdentityCoordinateTransform() {
		super(TYPE);
	}

	private IdentityCoordinateTransform(final Options options) {

		super(TYPE, options);
	}

	protected IdentityCoordinateTransform(IdentityCoordinateTransform other, TransformOptions<?> copyParams) {

		super(other, copyParams);
	}

	public static class Options extends TransformOptions<Options> {

		public IdentityCoordinateTransform build() {
			return new IdentityCoordinateTransform(this);
		}
	}

	public static Options options() {
		return new Options();
	}

	@Override
	public IdentityCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public IdentityCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new IdentityCoordinateTransform(this, copyParams);
	}

	@Override
	public InvertibleRealTransform getTransform() {

		final int nd = inputCoordinateSystem != null ? inputCoordinateSystem.numDimensions() : 0;
		return getTransform(nd);
	}

	/**
	 * @param nd the number of dimensions
	 * @return an identity transform with {@code nd} source and target dimensions
	 */
	public InvertibleRealTransform getTransform(final int nd) {

		return new IdentityRealTransform(nd);
	}

	private static class IdentityRealTransform implements InvertibleRealTransform {

		private int nDims;

		public IdentityRealTransform(int nDims) {

			this.nDims = nDims;
		}

		@Override
		public int numSourceDimensions() {

			return nDims;
		}

		@Override
		public int numTargetDimensions() {

			return nDims;
		}

		@Override
		public void apply(double[] source, double[] target) {

			// use target.length instead of nDims
			// in case nDims is not specified
			if (source == target)
				return;
			else
				System.arraycopy(source, 0, target, 0, target.length);
		}

		@Override
		public void apply(RealLocalizable source, RealPositionable target) {

			if (source == target)
				return;
			else
				target.setPosition(source);
		}

		@Override
		public void applyInverse(double[] source, double[] target) {

			apply(source, target);
		}

		@Override
		public void applyInverse(RealPositionable source, RealLocalizable target) {

			apply(target, source);
		}

		@Override
		public InvertibleRealTransform inverse() {

			return this;
		}

		@Override
		public InvertibleRealTransform copy() {

			return new IdentityRealTransform(nDims);
		}

	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

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

	public IdentityCoordinateTransform( final String name, final String input, final String output ) {
		super(TYPE, name, input, output );
	}

	public IdentityCoordinateTransform( final String name, final int[] inputAxes, final int[] outputAxes ) {
		super(TYPE, name, inputAxes, outputAxes );
	}

	public IdentityCoordinateTransform(IdentityCoordinateTransform other) {

		super(other);
	}

	public IdentityCoordinateTransform(IdentityCoordinateTransform other, int[] inputAxes, int[] outputAxes) {

		super(other, inputAxes, outputAxes);
	}

	@Override
	public InvertibleRealTransform getTransform() {

		if (getInputAxes() != null)
			return new IdentityRealTransform(getInputAxes().length);
		else
			return new IdentityRealTransform(0);
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

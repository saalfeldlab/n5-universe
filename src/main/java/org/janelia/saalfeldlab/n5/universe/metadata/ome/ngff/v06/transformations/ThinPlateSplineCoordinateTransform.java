package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;

public class ThinPlateSplineCoordinateTransform extends AbstractCoordinateTransform<ThinplateSplineTransform> {

	public static final String TYPE = "thin-plate-spline";

	protected int numDimensions;

	protected double[][] movingPoints;

	protected double[][] targetPoints;

	protected double[][] affine;

	protected double[] translation;

	protected double[] weights;

	protected transient ThinplateSplineTransform tps;

	public ThinPlateSplineCoordinateTransform(
			final double[][] movingPoints,
			final double[][] targetPoints) {
		this(options().name("").input(OmeNgffReference.DUMMY).output(OmeNgffReference.DUMMY)
				.points(movingPoints, targetPoints));
	}

	private ThinPlateSplineCoordinateTransform(final Options options) {
		super(TYPE, options);

		if (options.kernelTransform != null) {
			final ThinPlateR2LogRSplineKernelTransform kernel = options.kernelTransform;
			this.movingPoints = kernel.getSourceLandmarks();
			this.affine = kernel.getAffine();
			this.translation = kernel.getTranslation();
			this.weights = kernel.getKnotWeights();

			this.tps = new ThinplateSplineTransform(kernel);
			numDimensions = this.tps.numSourceDimensions();
		} else if (options.movingPoints != null && options.targetPoints != null) {
			this.movingPoints = options.movingPoints;
			this.targetPoints = options.targetPoints;
			this.tps = new ThinplateSplineTransform(movingPoints, targetPoints);
			numDimensions = movingPoints.length;
		}
	}

	/**
	 * Options for a thin plate spline transform. Its parameters are given either as the
	 * {@link #points moving and target points}, or as a {@link #transform transform}
	 * that has already been built.
	 */
	public static class Options extends TransformOptions<Options> {

		private double[][] movingPoints;
		private double[][] targetPoints;
		private ThinPlateR2LogRSplineKernelTransform kernelTransform;

		public Options points(final double[][] movingPoints, final double[][] targetPoints) {
			this.movingPoints = movingPoints;
			this.targetPoints = targetPoints;
			this.kernelTransform = null;
			return this;
		}

		public Options transform(final ThinplateSplineTransform tps) {
			return transform(tps.getKernelTransform());
		}

		public Options transform(final ThinPlateR2LogRSplineKernelTransform kernelTransform) {
			this.kernelTransform = kernelTransform;
			this.movingPoints = null;
			this.targetPoints = null;
			return this;
		}

		public ThinPlateSplineCoordinateTransform build() {
			return new ThinPlateSplineCoordinateTransform(this);
		}
	}

	public static Options options() {
		return new Options();
	}

	protected ThinPlateSplineCoordinateTransform(final ThinPlateSplineCoordinateTransform other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		this.numDimensions = other.numDimensions;
		this.movingPoints = deepCopy(other.movingPoints);
		this.targetPoints = deepCopy(other.targetPoints);
		this.affine = deepCopy(other.affine);
		this.translation = other.translation == null ? null : other.translation.clone();
		this.weights = other.weights == null ? null : other.weights.clone();
		// tps is built lazily from the copied parameters
	}

	@Override
	public ThinPlateSplineCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public ThinPlateSplineCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new ThinPlateSplineCoordinateTransform(this, copyParams);
	}

	@Override
	public ThinplateSplineTransform getTransform() {

		if (tps == null)
			if (targetPoints != null)
				tps = new ThinplateSplineTransform(movingPoints, targetPoints);
			else if (weights != null)
				tps = new ThinplateSplineTransform(
						new ThinPlateR2LogRSplineKernelTransform(movingPoints, affine, translation, weights));

		return tps;
	}
}

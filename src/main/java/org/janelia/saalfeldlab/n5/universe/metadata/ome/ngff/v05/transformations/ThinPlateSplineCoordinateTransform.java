package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

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
		this("", null, null, movingPoints, targetPoints );
	}

	public ThinPlateSplineCoordinateTransform(
			final String input, final String output,
			final double[][] movingPoints,
			final double[][] targetPoints) {
		this("", input, output, movingPoints, targetPoints );
	}

	public ThinPlateSplineCoordinateTransform( final String name,
			final String input, final String output,
			final double[][] movingPoints,
			final double[][] targetPoints) {
		super(TYPE, name, input, output);
		this.movingPoints = movingPoints;
		this.targetPoints = targetPoints;
		this.tps = new ThinplateSplineTransform(movingPoints, targetPoints);
		numDimensions = movingPoints.length;
	}

	public ThinPlateSplineCoordinateTransform(
			ThinplateSplineTransform tps) {
		this("", null, null, tps );
	}

	public ThinPlateSplineCoordinateTransform(
			final String input, final String output,
			ThinplateSplineTransform tps) {
		this("", input, output, tps);
	}

	public ThinPlateSplineCoordinateTransform(final String name,
			final String input, final String output,
			ThinplateSplineTransform tps) {

		this(name, input, output, tps.getKernelTransform());
	}

	public ThinPlateSplineCoordinateTransform(
			ThinPlateR2LogRSplineKernelTransform tps) {
		this("", null, null, tps );
	}

	public ThinPlateSplineCoordinateTransform(
			final String input, final String output,
			ThinPlateR2LogRSplineKernelTransform tps) {
		this("", input, output, tps);
	}

	public ThinPlateSplineCoordinateTransform(final String name,
			final String input, final String output,
			ThinPlateR2LogRSplineKernelTransform tps) {

		super(TYPE, name, input, output);
		this.movingPoints = tps.getSourceLandmarks();
		this.affine = tps.getAffine();
		this.translation = tps.getTranslation();
		this.weights = tps.getKnotWeights();

		this.tps = new ThinplateSplineTransform(tps);
		numDimensions = this.tps.numSourceDimensions();
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

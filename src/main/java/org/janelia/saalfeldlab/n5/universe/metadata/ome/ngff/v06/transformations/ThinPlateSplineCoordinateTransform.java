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
		this("", OmeNgffReference.DUMMY, OmeNgffReference.DUMMY, 
				movingPoints, targetPoints );
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

	public ThinPlateSplineCoordinateTransform( final String name,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final double[][] movingPoints,
			final double[][] targetPoints) {
		super(TYPE, name, inputRef, outputRef);
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

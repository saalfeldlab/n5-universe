package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.Objects;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

/**
 * Metadata representing an N5Dataset that implements {@link SpatialMetadata} and {@link IntensityMetadata}.
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class N5SingleScaleMetadata extends AbstractN5SpatialDatasetMetadata implements SpatialModifiable<N5SingleScaleMetadata> {

	private final AffineTransform3D transform;
	private final String unit;
	private final double[] downsamplingFactors;
	private final double[] pixelResolution;
	private final double[] offset;
	private final Double minIntensity;
	private final Double maxIntensity;
	private final boolean isLabelMultiset;

	public N5SingleScaleMetadata(final String path, final AffineTransform3D transform,
			final double[] downsamplingFactors,
			final double[] pixelResolution,
			final double[] offset,
			final String unit,
			final DatasetAttributes attributes) {

		this(path, transform, downsamplingFactors, pixelResolution, offset, unit, attributes, null, null, false);
	}

	public N5SingleScaleMetadata(final String path, final AffineTransform3D transform,
			final double[] downsamplingFactors,
			final double[] pixelResolution,
			final double[] offset,
			final String unit,
			final DatasetAttributes attributes,
			final boolean isLabelMultiset) {

		this(path, transform, downsamplingFactors, pixelResolution, offset, unit, attributes, null, null, isLabelMultiset);
	}

	public N5SingleScaleMetadata(final String path, final AffineTransform3D transform,
			final double[] downsamplingFactors,
			final double[] pixelResolution,
			final double[] offset,
			final String unit,
			final DatasetAttributes attributes,
			final Double minIntensity,
			final Double maxIntensity,
			boolean isLabelMultiset) {

		super(path, attributes);

		Objects.requireNonNull(path);
		Objects.requireNonNull(transform);
		Objects.requireNonNull(downsamplingFactors);
		Objects.requireNonNull(pixelResolution);
		Objects.requireNonNull(offset);
		this.transform = transform;
		this.downsamplingFactors = downsamplingFactors;
		this.pixelResolution = pixelResolution;
		this.offset = offset;
		/*
		 * These are allowed to be null, if we wish to use implementation
		 * defaults
		 */

		final Double defaultMaxIntensity = attributes != null ? IntensityMetadata.maxForDataType(attributes.getDataType()) : new Double(255);
		this.minIntensity = minIntensity != null ? minIntensity : 0;
		this.maxIntensity = maxIntensity != null ? maxIntensity : defaultMaxIntensity;
		this.isLabelMultiset = isLabelMultiset;

		if (unit == null)
			this.unit = "pixel";
		else
			this.unit = unit;
	}

	@Override
	public AffineGet spatialTransform() {

		return transform.copy();
	}

	@Override
	public String unit() {

		return unit;
	}

	@Override
	public double minIntensity() {

		return minIntensity;
	}

	@Override
	public double maxIntensity() {

		return maxIntensity;
	}

	public double[] getPixelResolution() {

		return pixelResolution;
	}

	public double[] getOffset() {

		return offset;
	}

	public double[] getDownsamplingFactors() {

		return downsamplingFactors;
	}

	public boolean isLabelMultiset() {

		return isLabelMultiset;
	}

	@Override
	public N5SingleScaleMetadata modifySpatialTransform(final String newPath, final AffineGet relativeTransformation) {

		final int nd = relativeTransformation.numDimensions();

		// if relative transformation is 4d, it means the last dimension
		// contains time
		final AffineGet transform;
		if (nd == 4)
			transform = TransformUtils.subAffine(relativeTransformation, new int[]{0, 1, 2});
		else if (nd == 2)
			transform = TransformUtils.superAffine(relativeTransformation, 3, new int[]{0, 1});
		else
			transform = relativeTransformation;

		final AffineTransform3D newTransform = new AffineTransform3D();
		newTransform.preConcatenate(spatialTransform());
		newTransform.preConcatenate(transform);

		final double[] newScale = new double[nd];
		final double[] newTranslation = new double[nd];
		int j = 0;
		for (int i = 0; i < nd; i++) {
			if (nd <= 3) {
				newScale[i] = newTransform.get(j, j);
				newTranslation[i] = newTransform.get(j, nd);
			} else {
				newScale[i] = 1.0;
				newTranslation[i] = 0.0;
			}
			j++;
		}

		return new N5SingleScaleMetadata(newPath,
				newTransform, downsamplingFactors, newScale, newTranslation, unit,
				getAttributes(), minIntensity, maxIntensity, isLabelMultiset);
	}

}

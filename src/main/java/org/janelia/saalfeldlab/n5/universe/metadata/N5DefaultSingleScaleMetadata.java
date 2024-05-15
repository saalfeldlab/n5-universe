package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * This class merely serves as a marker that all its values are default values. See
 * {@link N5GenericSingleScaleMetadataParser}.
 */
public class N5DefaultSingleScaleMetadata extends N5SingleScaleMetadata {

	public N5DefaultSingleScaleMetadata(String path, AffineTransform3D transform, double[] downsamplingFactors, double[] pixelResolution, double[] offset,
			String unit, DatasetAttributes attributes, Double minIntensity, Double maxIntensity, boolean isLabelMultiset) {

		super(path, transform, downsamplingFactors, pixelResolution, offset, unit, attributes, minIntensity, maxIntensity, isLabelMultiset);
	}

	public N5DefaultSingleScaleMetadata(String path, AffineTransform3D transform, double[] downsamplingFactors, double[] pixelResolution, double[] offset,
			String unit, DatasetAttributes attributes, boolean isLabelMultiset) {

		super(path, transform, downsamplingFactors, pixelResolution, offset, unit, attributes, isLabelMultiset);
	}

	public N5DefaultSingleScaleMetadata(String path, AffineTransform3D transform, double[] downsamplingFactors, double[] pixelResolution, double[] offset,
			String unit, DatasetAttributes attributes) {

		super(path, transform, downsamplingFactors, pixelResolution, offset, unit, attributes);
	}

}

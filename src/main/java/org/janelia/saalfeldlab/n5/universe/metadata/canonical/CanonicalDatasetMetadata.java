package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import net.imglib2.type.numeric.ARGBType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.IntensityMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;

public class CanonicalDatasetMetadata extends AbstractN5DatasetMetadata implements CanonicalMetadata, N5DatasetMetadata, IntensityMetadata, ColorMetadata {

	private IntensityLimits intensityLimits;

	private ColorMetadata colorMetadata;

	private static final ARGBType defaultColor = new ARGBType( ARGBType.rgba(255, 255, 255, 255));

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes,
			final double min, final double max,
			final ColorMetadata colorMetadata ) {
		super( path, attributes);
		intensityLimits = new IntensityLimits( min, max );
		this.colorMetadata = colorMetadata;
	}

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes,
			final IntensityLimits limits,
			final ColorMetadata colorMetadata ) {
		super( path, attributes);
		intensityLimits = limits;
		this.colorMetadata = colorMetadata;
	}

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes,
			final ColorMetadata colorMetadata ) {
		super( path, attributes);
		intensityLimits = null;
		this.colorMetadata = colorMetadata;
	}

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes) {
		super( path, attributes );
		intensityLimits = null;
		colorMetadata = null;
	}

	/**
	 * @return the minimum intensity value of the data
	 */
	@Override
	public double minIntensity() {
		return intensityLimits == null ? 0 : intensityLimits.min;
	}

	/**
	 * @return the maximum intensity value of the data
	 */
	@Override
	public double maxIntensity() {
		return intensityLimits == null ? 
				IntensityMetadata.maxForDataType(getAttributes().getDataType()) :
					intensityLimits.max;
	}

	@Override
	public ARGBType getColor() {
		return colorMetadata == null ? defaultColor : colorMetadata.getColor();
	}

	public ColorMetadata getColorMetadata() {
		return colorMetadata;
	}

	protected static class IntensityLimits {
		public final double min;
		public final double max;
		public IntensityLimits( double min, double max ) {
			this.min = min;
			this.max = max;
		}
	}

}

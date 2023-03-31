package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import java.util.Arrays;
import net.imglib2.realtransform.AffineGet;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.CalibratedSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.LinearSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;

/**
 * Interface for metadata describing how spatial data are oriented in physical
 * space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataCanonical implements SpatialMetadata, AxisMetadata {

	private final SpatialTransform transform;
	private final String unit; // redundant, also in axis list
	private final String path;
	private final Axis[] axes;

	public SpatialMetadataCanonical(final String path, final SpatialTransform transform, final String unit,
			final Axis[] axes) {

		this.path = path;
		this.unit = unit;
		this.transform = transform;
		this.axes = axes;
	}

	public SpatialMetadataCanonical(final String path, CalibratedSpatialTransform calTransform, final Axis[] axes) {

		this.path = path;
		this.unit = calTransform.getUnit();
		this.transform = calTransform.getSpatialTransform();
		this.axes = axes;
	}

	public SpatialTransform transform() {
		return transform;
	}

	@Override
	public AffineGet spatialTransform() {
		if (transform instanceof LinearSpatialTransform) {
			return ((LinearSpatialTransform)transform).getTransform().copy();
		} else
			return null;
	}

	@Override
	public String unit() {

		return unit;
	}

	@Override
	public String getPath() {

		return path;
	}

	@Override public Axis[] getAxes() {

		return axes;
	}

	@Override
	public String[] getAxisLabels() {

		return Arrays.stream(axes).map(Axis::getLabel).toArray(String[]::new);
	}

	@Override
	public String[] getAxisTypes() {

		return Arrays.stream(axes).map(Axis::getType).toArray(String[]::new);
	}

	@Override
	public String[] getUnits() {
		return Arrays.stream( axes ).map( Axis::getUnit ).toArray( String[]::new );
	}

}

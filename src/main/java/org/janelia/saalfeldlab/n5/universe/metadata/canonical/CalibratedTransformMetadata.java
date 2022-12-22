package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import java.util.Arrays;
import net.imglib2.realtransform.AffineGet;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.CalibratedSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.LinearSpatialTransform;

public class CalibratedTransformMetadata implements SpatialMetadata, AxisMetadata {

	private final String path;
	private final CalibratedSpatialTransform spatialTransform;
	private final Axis[] axes;

	public CalibratedTransformMetadata(final String path, final CalibratedSpatialTransform spatialTransform, final Axis[] axes)
	{
		this.path = path;
		this.spatialTransform = spatialTransform;
		this.axes = axes;
	}

	@Override
	public String getPath() {
		return path;
	}

	public CalibratedSpatialTransform getTransform() {
		return spatialTransform;
	}

	@Override
	public AffineGet spatialTransform() {
		if( spatialTransform.getTransform() instanceof LinearSpatialTransform ) {
			return ((LinearSpatialTransform)spatialTransform.getTransform()).getTransform().copy();
		}
		else
			return null;
	}

	@Override
	public String unit() {
		return spatialTransform.getUnit();
	}

	@Override
	public String[] getAxisLabels() {
		return Arrays.stream( axes ).map( Axis::getLabel).toArray( String[]::new );
	}

	@Override
	public String[] getAxisTypes() {
		return Arrays.stream( axes ).map( Axis::getType ).toArray( String[]::new );
	}

	@Override
	public String[] getUnits() {
		return Arrays.stream( axes ).map( Axis::getUnit ).toArray( String[]::new );
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialModifiable;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import net.imglib2.realtransform.AffineGet;

public class NgffV06SingleScaleAxesMetadata implements AxisMetadata, N5SpatialDatasetMetadata, SpatialModifiable<NgffV06SingleScaleAxesMetadata> {

	public static final String AXIS_KEY = "axes";

	public static final String COORDINATETRANSFORMATIONS_KEY = "coordinateTransformations";

	private final String path;

	private final CoordinateSystem[] coordinateSystems;

	private final CoordinateTransform<?>[] coordinateTransformations;

	private transient final DatasetAttributes datasetAttributes;

	public NgffV06SingleScaleAxesMetadata(final String path,
			final CoordinateTransform<?>[] coordinateTransformations,
			final CoordinateSystem[] coordinateSystems,
			final DatasetAttributes datasetAttributes) {

		this.path = MetadataUtils.normalizeGroupPath(path);
		this.coordinateTransformations = coordinateTransformations;
		this.coordinateSystems = coordinateSystems;
		this.datasetAttributes = datasetAttributes;
	}

	@Override
	public Axis[] getAxes() {

		return coordinateSystems[0].getAxes() ;
	}

	public CoordinateTransform<?>[] getCoordinateTransformations() {

		return coordinateTransformations;
	}

	@Override
	public DatasetAttributes getAttributes() {

		return datasetAttributes;
	}

	@Override
	public String getPath() {

		return path;
	}

	@Override
	public AffineGet spatialTransform() {

		// TODO don't just pick the first one, and better checking
		return (AffineGet)coordinateTransformations[0].getTransform();
	}

	@Override
	public String unit() {

		final Optional<Axis> firstSpace = Arrays.stream(getAxes()).filter(x -> x.getType().equals(Axis.SPACE)).findFirst();
		if( firstSpace.isPresent())
			return firstSpace.get().getUnit();

		final Optional<String> firstUnit = Arrays.stream(getAxes()).filter(x -> {
			final String unit = x.getUnit();
			return unit != null && unit.isEmpty();
		}).findFirst().map(x -> x.getUnit());

		return firstUnit.orElse("");
	}

	@Override
	public NgffV06SingleScaleAxesMetadata modifySpatialTransform(final String newPath, final AffineGet relativeTransformation) {

		// TODO
		return this;
	}

}

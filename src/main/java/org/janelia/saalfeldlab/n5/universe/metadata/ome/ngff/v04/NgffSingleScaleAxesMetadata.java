package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

public class NgffSingleScaleAxesMetadata implements AxisMetadata, N5SpatialDatasetMetadata {

	public static final String AXIS_KEY = "axes";

	public static final String COORDINATETRANSFORMATIONS_KEY = "coordinateTransformations";

	private final String path;

	private final Axis[] axes;

	private final CoordinateTransformation<?>[] coordinateTransformations;

	private transient final DatasetAttributes datasetAttributes;

	private transient final double[] scale;

	private transient final double[] translation;

	private transient final AffineGet transform;

	public NgffSingleScaleAxesMetadata(final String path, final double[] scale, final double[] translation,
			final DatasetAttributes datasetAttributes) {

		this(path, scale, translation,
				AxisUtils.defaultAxes(scale != null ? scale.length : translation.length),
				datasetAttributes);
	}

	public NgffSingleScaleAxesMetadata(final String path, final double[] scale, final double[] translation, final Axis[] axes,
			final DatasetAttributes datasetAttributes) {

		this.path = path;

		this.scale = scale;
		this.translation = translation;
		this.axes = axes;

		this.datasetAttributes = datasetAttributes;

		// TODO generalize for translation first
		coordinateTransformations = MetadataUtils.buildScaleTranslationTransformList(scale, translation);
		this.transform = MetadataUtils.scaleTranslationTransforms(scale, translation);
	}

	@Override
	public Axis[] getAxes() {

		// reverse final the axes if necessary
//		if (datasetAttributes == null)
			return axes;
//		else
//			return OmeNgffMultiScaleMetadata.reverseIfCorder(datasetAttributes, axes);
	}

	public CoordinateTransformation<?>[] getCoordinateTransformations() {

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

	public double[] getScale() {

		return scale;
	}

	public double[] getTranslation() {

		return translation;
	}

	@Override
	public AffineGet spatialTransform() {

		return transform;
	}

	@Override
	public String unit() {

		final Optional<Axis> firstSpace = Arrays.stream(axes).filter(x -> x.getType().equals(Axis.SPACE)).findFirst();
		if( firstSpace.isPresent())
			return firstSpace.get().getUnit();

		final Optional<String> firstUnit = Arrays.stream(axes).filter(x -> {
			final String unit = x.getUnit();
			return unit != null && unit.isEmpty();
		}).findFirst().map(x -> x.getUnit());

		return firstUnit.orElse("");
	}

	@Override
	public AffineTransform3D spatialTransform3d() {

		int nd = 0;
		final int[] spatialIndexes = new int[3];
		for (int i = 0; i < axes.length; i++) {
			if (axes[i].getType().equals(Axis.SPACE))
				spatialIndexes[nd++] = i;
		}

		final AffineTransform3D transform3d = new AffineTransform3D();
		// return identity if null
		if (transform == null)
			return transform3d;
		else if (transform instanceof AffineTransform3D)
			return (AffineTransform3D)transform;
		else {

			if (scale != null) {
				for (int i = 0; i < nd; i++) {
					transform3d.set(scale[spatialIndexes[i]], i, i);
				}
			}

			if (translation != null) {
				for (int i = 0; i < nd; i++) {
					transform3d.set(translation[spatialIndexes[i]], i, 3);
				}
			}
		}

		return transform3d;
	}

}

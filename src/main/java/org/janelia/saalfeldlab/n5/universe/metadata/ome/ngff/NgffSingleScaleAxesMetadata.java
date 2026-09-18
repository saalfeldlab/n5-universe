package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialModifiable;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;

public class NgffSingleScaleAxesMetadata implements AxisMetadata, N5SpatialDatasetMetadata,
	SpatialModifiable<NgffSingleScaleAxesMetadata> {

	public static final String AXIS_KEY = "axes";

	public static final String COORDINATETRANSFORMATIONS_KEY = "coordinateTransformations";

	private final String path;

	private final Axis[] axes;

	private final CoordinateTransform<?>[] coordinateTransformations;

	private transient final DatasetAttributes datasetAttributes;

	private transient final double[] scale;

	private transient final double[] translation;

	private transient final AffineGet transform;

	/**
	 * Permutation of this dataset's axis parameters relative to its parent multiscales,
	 * such that {@code childValue[i] = parentValue[permutationFromParent[i]]}.
	 * Null if not permuted, or if there is no parent.
	 */
	private transient final int[] permutationFromParent;

	public NgffSingleScaleAxesMetadata(final String path,
			final double[] scale, final double[] translation,
			final DatasetAttributes datasetAttributes) {

		this(path, scale, translation,
				AxisUtils.defaultAxes(scale != null ? scale.length : translation.length),
				datasetAttributes);
	}

	public NgffSingleScaleAxesMetadata(final String path,
			final double[] scale,
			final double[] translation,
			final Axis[] axes,
			final DatasetAttributes datasetAttributes) {

		this(path, scale, translation, axes, datasetAttributes, null);
	}

	/**
	 * @param permutationFromParent
	 *            the permutation of this dataset's axis parameters relative to its parent multiscales
	 *            (see {@link #getPermutationFromParent()}), or null if not permuted or there is no parent
	 */
	public NgffSingleScaleAxesMetadata(final String path,
			final double[] scale,
			final double[] translation,
			final Axis[] axes,
			final DatasetAttributes datasetAttributes,
			final int[] permutationFromParent) {

		this.path = MetadataUtils.normalizeGroupPath(path);
		this.permutationFromParent = permutationFromParent == null ? null : permutationFromParent.clone();

		this.scale = scale != null ? scale : ones(axes.length);
		this.translation = translation != null ? translation : new double[axes.length];
		this.axes = axes;

		this.datasetAttributes = datasetAttributes;

		coordinateTransformations = MetadataUtils.buildScaleTranslationTransformList(this.scale, this.translation);
		if (Arrays.stream(axes).allMatch(x -> x.getType().equals(Axis.SPACE))) {
			this.transform = MetadataUtils.scaleTranslationTransforms(this.scale, this.translation);
		} else {
			final int[] spaceIndexes = AxisUtils.indexes(axes, x -> x.getType().equals(Axis.SPACE));
			final double[] spaceScale = AxisUtils.permute(this.scale, spaceIndexes);
			final double[] spaceTranslation = AxisUtils.permute(this.translation, spaceIndexes);
			this.transform = MetadataUtils.scaleTranslationTransforms(spaceScale, spaceTranslation);
		}
	}

	private static double[] ones(final int N) {

		final double[] ones = new double[N];
		Arrays.fill(ones, 1);
		return ones;
	}

	@Override
	public Axis[] getAxes() {

		return axes;
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

	public double[] getScale() {

		return scale;
	}

	public double[] getTranslation() {

		return translation;
	}

	/**
	 * Returns the permutation of this dataset's axis parameters (axes, scale, translation)
	 * relative to those of its parent multiscales, such that
	 * {@code childValue[i] = parentValue[p[i]]}, the convention of the array overloads of
	 * {@link AxisUtils#permute(double[], int[])}.
	 * <p>
	 * When parsed from a zarr2 f-order array this is a reversal, because n5-zarr does not
	 * reverse the dimensions of f-order arrays. To permute an image with this, pass
	 * {@link AxisUtils#invertPermutation(int[])} of it to
	 * {@link AxisUtils#permute(net.imglib2.RandomAccessibleInterval, int[])}.
	 *
	 * @return a copy of the permutation, or null if not permuted or there is no parent
	 */
	public int[] getPermutationFromParent() {

		return permutationFromParent == null ? null : permutationFromParent.clone();
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

	@Override
	public NgffSingleScaleAxesMetadata modifySpatialTransform(final String newPath, final AffineGet relativeTransformation) {

		final int nd = this.axes.length;
		final AffineTransform newTransform = new AffineTransform(nd);
		newTransform.preConcatenate(spatialTransform());
		newTransform.preConcatenate(relativeTransformation);

		final double[] newScale = new double[nd];
		final double[] newTranslation = new double[nd];
		int j = 0;
		for (int i = 0; i < nd; i++) {
			newScale[i] = newTransform.get(j, j);
			newTranslation[i] = newTransform.get(j, nd);
			j++;
		}

		return new NgffSingleScaleAxesMetadata( newPath,
				newScale, newTranslation,
				axes, datasetAttributes, permutationFromParent);
	}

}

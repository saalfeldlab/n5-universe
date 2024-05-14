package org.janelia.saalfeldlab.n5.universe.metadata;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

public class MetadataUtils {

	// duplicate variables in N5ScalePyramidExporter in n5-ij
	public static final String DOWN_SAMPLE = "Sample";
	public static final String DOWN_AVERAGE = "Average";

	public static double[] mul(final double[] a, final double[] b) {

		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++)
			out[i] = a[i] * b[i];

		return out;
	}

	public static double[] mul(final double[] a, final long[] b) {

		final double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++)
			out[i] = a[i] * b[i];

		return out;
	}

	public static double[][] scalesAndTranslations(final double[] baseScale,
			final double[] downsamplingFactors, final int numLevels) {

		final int numDimensions = baseScale.length;
		final double[][] out = new double[numLevels][numDimensions];
		for( int i = 0; i < numLevels; i++ )
			for( int d = 0; d < numDimensions; d++ )
				if( i == 0 )
					out[i][d] = baseScale[d];
				else
					out[i][d] = out[i-1][d] * downsamplingFactors[d];

		return out;
	}

	public static long[] downsamplingFactors(final long factor, final long[] dimensions, final String[] types) {

		final int nd = dimensions.length;
		final long[] factors = new long[nd];
		for (int i = 0; i < nd; i++) {

			if (dimensions[i] > factor && !types[i].equals(Axis.CHANNEL))
				factors[i] = factor;
			else
				factors[i] = 1;

		}
		return factors;
	}

	public static long[] updateDownsamplingFactors(final long factor, final long[] baseFactors, final long[] dimensions, final String[] types) {

		final int nd = dimensions.length;
		final long[] factors = new long[nd];
		for (int i = 0; i < nd; i++) {

			if (dimensions[i] > factor && !types[i].equals(Axis.CHANNEL))
				factors[i] = factor * baseFactors[i];
			else
				factors[i] = baseFactors[i];

		}
		return factors;
	}

	public static CoordinateTransformation<?>[] buildScaleTranslationTransformList(final double[] scale, final double[] translation) {

		int nTforms = 0;
		if (scale != null)
			nTforms++;

		if (translation != null)
			nTforms++;

		final CoordinateTransformation<?>[] coordinateTransformations = new CoordinateTransformation<?>[nTforms];

		int i = 0;
		if (scale != null)
			coordinateTransformations[i++] = new ScaleCoordinateTransformation(scale);

		if (translation != null)
			coordinateTransformations[i++] = new TranslationCoordinateTransformation(translation);

		return coordinateTransformations;
	}

	public static ScaleAndTranslation scaleTranslationFromCoordinateTransformations(final CoordinateTransformation<?>[] cts) {

		if (cts == null || cts.length == 0)
			return null;

		final ScaleAndTranslation out = coordinateTransformToScaleAndTranslation(cts[0]);
		for (int i = 1; i < cts.length; i++) {
			out.preConcatenate(coordinateTransformToScaleAndTranslation(cts[i]));
		}
		return out;
	}

	public static ScaleAndTranslation coordinateTransformToScaleAndTranslation(CoordinateTransformation<?> ct) {

		if (ct.getType().equals(ScaleCoordinateTransformation.TYPE)) {
			final double[] s = ((ScaleCoordinateTransformation)ct).getScale();
			return new ScaleAndTranslation(s, new double[s.length]);
		} else if (ct.getType().equals(TranslationCoordinateTransformation.TYPE)) {
			final double[] t = ((TranslationCoordinateTransformation)ct).getTranslation();
			final double[] s = new double[t.length];
			Arrays.fill(s, 1.0);
			return new ScaleAndTranslation(s, t);
		}
		return null;
	}

	/**
	 * Returns a new {@link N5SingleScaleMetadata} equal to the baseMetadata, but with
	 * {@link DatasetAttributes} coming from datasetMetadata.ew
	 * <p>
	 *
	 * @param baseMetadata metadata
	 * @param datasetMetadata dataset metadata
	 * @return the single scale metadata
	 */
	public static N5SingleScaleMetadata setDatasetAttributes(final N5SingleScaleMetadata baseMetadata, final N5DatasetMetadata datasetMetadata) {

		if (baseMetadata.getPath().equals(datasetMetadata.getPath()))
			return new N5SingleScaleMetadata(baseMetadata.getPath(), baseMetadata.spatialTransform3d(),
					baseMetadata.getDownsamplingFactors(), baseMetadata.getPixelResolution(), baseMetadata.getOffset(),
					baseMetadata.unit(), datasetMetadata.getAttributes());
		else
			return null;
	}

	public static N5SingleScaleMetadata[] updateChildrenDatasetAttributes(final N5SingleScaleMetadata[] baseMetadata,
			final N5DatasetMetadata[] datasetMetadata) {

		final HashMap<String, N5SingleScaleMetadata> bases = new HashMap<>();
		Arrays.stream(baseMetadata).forEach(x -> {
			bases.put(x.getPath(), x);
		});

		return (N5SingleScaleMetadata[])Arrays.stream(datasetMetadata).map(x -> {
			final N5SingleScaleMetadata b = bases.get(x.getPath());
			if (b == null)
				return null;
			else
				return setDatasetAttributes(b, x);
		}).filter(x -> x != null).toArray();
	}

	public static void updateChildrenMetadata(final N5TreeNode parent, final N5Metadata[] childrenMetadata,
			final boolean relative) {

		final HashMap<String, N5Metadata> children = new HashMap<>();
		Arrays.stream(childrenMetadata).forEach(x -> {
			final String absolutePath;
			if (relative) {
				absolutePath = normalizeGroupPath(parent.getPath() + "/" + x.getPath());
			} else {
				absolutePath = x.getPath();
			}
			children.put(absolutePath, x);
		});
		parent.childrenList().forEach(c -> {
			final N5Metadata m = children.get(MetadataUtils.normalizeGroupPath(c.getPath()));
			if (m != null)
				c.setMetadata(m);
		});
	}

	public static String canonicalPath(final N5TreeNode parent, final String child) {

		return canonicalPath(parent.getPath(), child);
	}

	public static String canonicalPath(final String parent, final String child) {

		try {
			final N5URI url = new N5URI("?/" + parent + "/" + child);
			return url.normalizeGroupPath();
		} catch (final URISyntaxException e) {}
		return null;
	}

	public static String normalizeGroupPath(final String path) {

		try {
			return new N5URI("?" + path).normalizeGroupPath();
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}
		return path;
	}

	/**
	 * Returns a relative group path from the child absolute path group path child
	 * the parent absolute group path.
	 *
	 * If the child path is not a descendent of parent, child will be returned.
	 *
	 * @param parent an absolute path
	 * @param child an absolute path
	 * @return relative path from child to parent, if it exists.
	 */
	public static String relativePath(final String parent, final String child) {

		try {
			final String purl = new N5URI("?" + parent).normalizeGroupPath();
			final String curl = new N5URI("?" + child).normalizeGroupPath();
			return new N5URI("?" + curl.replaceFirst("^"+purl, "")).normalizeGroupPath();
		} catch (final URISyntaxException e) {}
		return child;
	}

	/**
	 * Element-wise power. Returns an array y such that y[i] = x[i] ^ d
	 *
	 * @param x array
	 * @param d exponent
	 * @return result
	 */
	public static double[] pow(final double[] x, final int d) {

		final double[] y = new double[x.length];
		Arrays.fill(y, 1);
		for (int i = 0; i < d; i++)
			for (int j = 0; j < x.length; j++)
				y[j] *= x[j];

		return y;
	}

	/**
	 * Gets a String from a {@link JsonElement} if possible, returning null if the
	 * element is {@link JsonNull}.
	 *
	 * @param element the json element
	 * @return a string
	 */
	public static String getStringNullable(final JsonElement element) {

		if (element == null || element.isJsonNull())
			return null;
		else
			return element.getAsString();
	}

	/**
	 * Returns the most efficient transform given the input scale and translation parameters.
	 * If both are null, this method will return null;
	 *
	 * @param scale the scale parameters
	 * @param translation the translation parameters
	 * @return an appropriate AffineGet
	 */
	public static AffineGet scaleTranslationTransforms(final double[] scale, final double[] translation) {

		if (translation != null) {

			if (scale != null) {
				return new ScaleAndTranslation(scale, translation);
			} else {
				// scale null, translation not null
				if (translation.length == 2)
					return new Translation2D(translation);
				else if (translation.length == 3)
					return new Translation3D(translation);
				else
					return new Translation(translation);
			}

		} else if (scale != null) {
			// scale not null, translation null
			if (scale.length == 2)
				return new Scale2D(scale);
			else if (scale.length == 3)
				return new Scale3D(scale);
			else
				return new Scale(scale);

		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <M extends N5DatasetMetadata> M metadataForThisScale(final String newPath,
			final M baseMetadata,
			final String downsampleMethod,
			final double[] baseResolution,
			final double[] absoluteDownsamplingFactors,
			final double[] absoluteScale,
			final double[] absoluteTranslation) {

		if (baseMetadata == null)
			return null;

		/**
		 * if metadata is N5SingleScaleMetadata and not a subclass of it then this is using N5Viewer
		 * metadata which does not have an offset
		 */
		if (baseMetadata.getClass().equals(N5SingleScaleMetadata.class)) {
			return (M)buildN5VMetadata(newPath, (N5SingleScaleMetadata)baseMetadata, downsampleMethod, baseResolution, absoluteDownsamplingFactors);
		} else if (baseMetadata instanceof N5CosemMetadata) {
			return (M)buildCosemMetadata(newPath, (N5CosemMetadata)baseMetadata, absoluteScale, absoluteTranslation);

		} else if (baseMetadata instanceof NgffSingleScaleAxesMetadata) {
			return (M)buildNgffMetadata(newPath, (NgffSingleScaleAxesMetadata)baseMetadata, absoluteScale, absoluteTranslation);
		} else
			return baseMetadata;
	}

	public static N5SingleScaleMetadata buildN5VMetadata(
			final String path,
			final N5SingleScaleMetadata baseMetadata,
			final String downsampleMethod,
			final double[] baseResolution,
			final double[] downsamplingFactors) {

		/**
		 * N5Viewer metadata doesn't have a way to directly represent offset. Rather, the half-pixel
		 * offsets that averaging downsampling introduces are assumed when downsampling factors are
		 * not equal to ones.
		 *
		 * As a result, we use downsampling factors with average downsampling, but set the factors
		 * to one otherwise.
		 */
		final int nd = baseResolution.length > 3 ? 3 : baseResolution.length;
		final double[] resolution = new double[nd];
		final double[] factors = new double[nd];

		if (downsampleMethod.equals(DOWN_AVERAGE)) {
			System.arraycopy(baseResolution, 0, resolution, 0, nd);
			System.arraycopy(downsamplingFactors, 0, factors, 0, nd);
		} else {
			for (int i = 0; i < nd; i++)
				resolution[i] = baseResolution[i] * downsamplingFactors[i];

			Arrays.fill(factors, 1);
		}

		final AffineTransform3D transform = new AffineTransform3D();
		for (int i = 0; i < nd; i++)
			transform.set(resolution[i], i, i);

		return new N5SingleScaleMetadata(
				path,
				transform,
				factors,
				resolution,
				baseMetadata.getOffset(),
				baseMetadata.unit(),
				baseMetadata.getAttributes(),
				baseMetadata.minIntensity(),
				baseMetadata.maxIntensity(),
				baseMetadata.isLabelMultiset());

	}

	public static N5CosemMetadata buildCosemMetadata(
			final String path,
			final N5CosemMetadata baseMetadata,
			final double[] absoluteResolution,
			final double[] absoluteTranslation) {

		final double[] resolution = new double[absoluteResolution.length];
		System.arraycopy(absoluteResolution, 0, resolution, 0, absoluteResolution.length);

		final double[] translation = new double[absoluteTranslation.length];
		System.arraycopy(absoluteTranslation, 0, translation, 0, absoluteTranslation.length);

		return new N5CosemMetadata(
				path,
				new CosemTransform(
						baseMetadata.getCosemTransform().axes,
						resolution,
						translation,
						baseMetadata.getCosemTransform().units),
				baseMetadata.getAttributes());
	}

	public static NgffSingleScaleAxesMetadata buildNgffMetadata(
			final String path,
			final NgffSingleScaleAxesMetadata baseMetadata,
			final double[] absoluteResolution,
			final double[] absoluteTranslation) {

		final double[] resolution = new double[absoluteResolution.length];
		System.arraycopy(absoluteResolution, 0, resolution, 0, absoluteResolution.length);

		final double[] translation = new double[absoluteTranslation.length];
		System.arraycopy(absoluteTranslation, 0, translation, 0, absoluteTranslation.length);

		return new NgffSingleScaleAxesMetadata(
				path,
				resolution,
				translation,
				baseMetadata.getAxes(),
				baseMetadata.getAttributes());
	}

	@SuppressWarnings("unchecked")
	public static <M extends N5Metadata> M permuteSpatialMetadata(final M metadata, final int[] axisPermutation) {

		if (metadata == null)
			return null;

		/**
		 * if metadata is N5SingleScaleMetadata and not a subclass of it then this is using N5Viewer
		 * metadata which does not have an offset
		 */
		if (metadata.getClass().equals(N5SingleScaleMetadata.class)) {
			return (M)permuteN5vMetadata((N5SingleScaleMetadata)metadata, axisPermutation);
		} else if (metadata instanceof N5CosemMetadata) {
			return (M)permuteCosemMetadata((N5CosemMetadata)metadata, axisPermutation);
		} else if (metadata instanceof NgffSingleScaleAxesMetadata) {
			return (M)permuteNgffMetadata((NgffSingleScaleAxesMetadata)metadata, axisPermutation);
		} else
			return metadata;
	}

	public static NgffSingleScaleAxesMetadata permuteNgffMetadata(final NgffSingleScaleAxesMetadata metadata, int[] axisPermutation) {

		final Axis[] axes = metadata.getAxes();
		final Axis[] axesPermuted = new Axis[axes.length];
		for (int i = 0; i < axes.length; i++)
			axesPermuted[i] = axes[i];

		AxisUtils.permute(axesPermuted, axesPermuted, axisPermutation);

		return new NgffSingleScaleAxesMetadata(
				metadata.getPath(),
				AxisUtils.permute(metadata.getScale(), axisPermutation),
				AxisUtils.permute(metadata.getTranslation(), axisPermutation),
				axesPermuted,
				metadata.getAttributes());
	}

	public static N5CosemMetadata permuteCosemMetadata(final N5CosemMetadata metadata, int[] axisPermutation) {

		final double[] oldScales = ArrayUtils.clone(metadata.getCosemTransform().scale);
		ArrayUtils.reverse(oldScales);
		final double[] newScales = AxisUtils.permute(oldScales, axisPermutation);
		ArrayUtils.reverse(newScales);

		final double[] oldTranslation = ArrayUtils.clone(metadata.getCosemTransform().translate);
		ArrayUtils.reverse(oldTranslation);
		final double[] newTranslation = AxisUtils.permute(oldTranslation, axisPermutation);
		ArrayUtils.reverse(newTranslation);

		final String[] newAxes = ArrayUtils.clone(metadata.getCosemTransform().axes);
		ArrayUtils.reverse(newAxes);
		AxisUtils.permute(newAxes, newAxes, axisPermutation);
		ArrayUtils.reverse(newAxes);

		final String[] newUnits = ArrayUtils.clone(metadata.getCosemTransform().units);
		ArrayUtils.reverse(newUnits);
		AxisUtils.permute(newUnits, newUnits, axisPermutation);
		ArrayUtils.reverse(newUnits);

		return new N5CosemMetadata(
				metadata.getPath(),
				new CosemTransform(newAxes, newScales, newTranslation, newUnits),
				metadata.getAttributes());
	}


	public static N5SingleScaleMetadata permuteN5vMetadata(final N5SingleScaleMetadata metadata, int[] axisPermutation) {

		final double[] newScales = AxisUtils.permute(metadata.getPixelResolution(), axisPermutation);
		final double[] newOffset = AxisUtils.permute(metadata.getOffset(), axisPermutation);
		final double[] newFactors = AxisUtils.permute(metadata.getDownsamplingFactors(), axisPermutation);

		final AffineTransform3D offsetTform = new AffineTransform3D();
		offsetTform.translate(newOffset);

		final AffineTransform3D transform = N5SingleScaleMetadataParser.buildTransform(newFactors, newScales, Optional.of(offsetTform));

		return new N5SingleScaleMetadata(
				metadata.getPath(),
				transform,
				newFactors,
				newScales,
				newOffset,
				metadata.unit(),
				metadata.getAttributes());

	}

}

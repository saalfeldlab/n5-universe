package org.janelia.saalfeldlab.n5.universe.metadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

public class MetadataUtils {

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
			return new URI(N5URI.normalizeGroupPath(parent)).relativize(
					new URI(N5URI.normalizeGroupPath(child)))
					.toASCIIString();
		} catch (URISyntaxException e) {}
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

}

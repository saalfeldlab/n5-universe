package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffV04MultiScaleMetadata.OmeNgffV04Dataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

public class OmeNgffMetadata extends SpatialMultiscaleMetadata<NgffSingleScaleAxesMetadata>
{
	public final OmeNgffV04MultiScaleMetadata[] multiscales;

	public OmeNgffMetadata( final String path, final OmeNgffV04MultiScaleMetadata[] multiscales)
	{
		// assumes children metadata are the same for all multiscales, which should be true
		super(path, multiscales[0].getChildrenMetadata());
		this.multiscales = multiscales;
	}

	/**
	 * Creates an OmeNgffMetadata object for writing.
	 * See {@link AxisUtils#defaultAxes(String...)} for convenient creation of axes.
	 *
	 * @param numDimensions number of dimensions
	 * @param name a name for this dataset
	 * @param axes an array of axes (length numDimensions)
	 * @param scalePaths relative paths to children containing scale level arrays
	 * @param scales array of absolute resolutions. size: [numScales][numDimensions]
	 * @param translations array of translations. size: [numScales][numDimensions]. May be null.
	 * @return OmeNgffMetadata
	 */
	public static OmeNgffMetadata buildForWriting( final int numDimensions,
			final String name,
			final Axis[] axes,
			final String[] scalePaths,
			final double[][] scales,
			final double[][] translations) {

		// TODO make this a constructor? (yes, says Caleb, and John)

		assert scalePaths.length == scales.length;

		if( translations != null )
			assert scalePaths.length == translations.length;

		final int numScales = scalePaths.length;
		final String version = "0.4";
		final String type = "";
		final OmeNgffV04Dataset[] datasets = new OmeNgffV04Dataset[numScales];
		for( int i = 0; i < numScales; i++ ) {

			final ScaleCoordinateTransformation s = new ScaleCoordinateTransformation(scales[i]);
			TranslationCoordinateTransformation t = null;
			if( translations != null && translations[i] != null )
				t = new TranslationCoordinateTransformation(translations[i]);

			datasets[i] = new OmeNgffV04Dataset();
			datasets[i].path = scalePaths[i];
			datasets[i].coordinateTransformations = t == null ?
					new CoordinateTransformation[]{ s } :
					new CoordinateTransformation[]{ s, t };
		}

		final CoordinateTransformation<?>[] cts = null;
		final OmeNgffV04MultiScaleMetadata ms = new OmeNgffV04MultiScaleMetadata(
				numDimensions, "", name, type, axes,
				datasets, null, cts, null);


		return new OmeNgffMetadata("", new OmeNgffV04MultiScaleMetadata[]{ ms });
	}

	public static <T, M extends AxisMetadata & N5Metadata> RandomAccessibleInterval<T> permuteForNgff(
			final RandomAccessibleInterval<T> img,
			final M meta) {

		final int[] p = findNgffPermutation(meta.getAxisLabels());
		AxisUtils.fillPermutation(p);

		// TODO under what conditions can I return the image directly?
		RandomAccessibleInterval<T> imgTmp = img;
		while (imgTmp.numDimensions() < 5)
			imgTmp = Views.addDimension(imgTmp, 0, 0);

		if (AxisUtils.isIdentityPermutation(p))
			return imgTmp;

		return AxisUtils.permute(imgTmp, AxisUtils.invertPermutation(p));
	}

	/**
	 * Finds and returns a permutation p such that source[p[i]] equals xyczt
	 *
	 * @param axisLabels
	 *            the axis labels
	 * @return the permutation array
	 */
	public static int[] findNgffPermutation(final String[] axisLabels) {

		final int[] p = new int[5];
		p[0] = indexOf(axisLabels, "x");
		p[1] = indexOf(axisLabels, "y");
		p[2] = indexOf(axisLabels, "z");
		p[3] = indexOf(axisLabels, "c");
		p[4] = indexOf(axisLabels, "t");
		return p;
	}

	private static final <T> int indexOf(final T[] arr, final T tgt) {

		for (int i = 0; i < arr.length; i++) {
			if (arr[i].equals(tgt))
				return i;
		}
		return -1;
	}

}

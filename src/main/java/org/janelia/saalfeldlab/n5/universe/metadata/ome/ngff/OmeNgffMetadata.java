package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

public class OmeNgffMetadata extends SpatialMultiscaleMetadata<NgffSingleScaleAxesMetadata>
{
	public final OmeNgffMultiScaleMetadata[] multiscales;

	public final transient String version;

	public OmeNgffMetadata( final String path, final OmeNgffMultiScaleMetadata[] multiscales)
	{
		// assumes children metadata are the same for all multiscales, which should be true
		super(path, multiscales[0].getChildrenMetadata());
		this.multiscales = multiscales;
		this.version = multiscales[0].version;
	}

	/**
	 * Creates an OmeNgffMetadata object for writing.
	 * See {@link AxisUtils#defaultAxes(String...)} for convenient creation of axes.
	 *
	 * @param numDimensions number of dimensions
	 * @param name a name for this dataset
	 * @param version the OME-Zarr version
	 * @param axes an array of axes (length numDimensions)
	 * @param scalePaths relative paths to children containing scale level arrays
	 * @param scales array of absolute resolutions. size: [numScales][numDimensions]
	 * @param translations array of translations. size: [numScales][numDimensions]. May be null.
	 * @return OmeNgffMetadata
	 */
	public static OmeNgffMetadata buildForWriting( final int numDimensions,
			final String name,
			final String version,
			final Axis[] axes,
			final String[] scalePaths,
			final double[][] scales,
			final double[][] translations) {

		// TODO make this a constructor? (yes, says Caleb, and John)
		
		assert scalePaths.length == scales.length;

		if( translations != null )
			assert scalePaths.length == translations.length;

		final int numScales = scalePaths.length;
		final String type = "";

		final OmeNgffMultiScaleMetadataMutable mut = new OmeNgffMultiScaleMetadataMutable();
		for( int i = 0; i < numScales; i++ ) {
			
			final NgffSingleScaleAxesMetadata singleScaleMeta = new NgffSingleScaleAxesMetadata(scalePaths[i], 
					scales[i], 
					translations != null && translations[i] != null ? translations[i] : null, 
					null);
			
			mut.addChild(singleScaleMeta);
		}

		final CoordinateTransform<?>[] cts = null;
		final OmeNgffMultiScaleMetadata ms = new OmeNgffMultiScaleMetadata(numDimensions,
				"", name, type, version,
				axes, mut.getDatasets(), cts, null, null);

		return new OmeNgffMetadata("", new OmeNgffMultiScaleMetadata[]{ ms });
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
		return AxisUtils.findPermutationByName(axisLabels, "x", "y", "z", "c", "t");
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;

public class OmeNgffV05Metadata extends SpatialMultiscaleMetadata<NgffSingleScaleAxesMetadata>
{
	public final OmeNgffMultiScaleMetadata[] multiscales;

	public OmeNgffV05Metadata(final String path, final OmeNgffMultiScaleMetadata[] multiscales)
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
	public static OmeNgffV05Metadata buildForWriting( final int numDimensions,
			final String name,
			final Axis[] axes,
			final String[] scalePaths,
			final double[][] scales,
			final double[][] translations) {

		// TODO make this a constructor? (yes, says Caleb, and John)

		assert scalePaths.length == scales.length;
		assert translations == null || scalePaths.length == translations.length;

		final int numScales = scalePaths.length;
		final String version = "0.5";
		final String type = "";
		final OmeNgffDataset[] datasets = new OmeNgffDataset[numScales];
		for( int i = 0; i < numScales; i++ ) {

			final ScaleCoordinateTransformation s = new ScaleCoordinateTransformation(scales[i]);
			TranslationCoordinateTransformation t = null;
			if( translations != null && translations[i] != null )
				t = new TranslationCoordinateTransformation(translations[i]);

			datasets[i] = new OmeNgffDataset();
			datasets[i].path = scalePaths[i];
			datasets[i].coordinateTransformations = t == null ?
					new CoordinateTransformation[]{ s } :
					new CoordinateTransformation[]{ s, t };
		}

		final CoordinateTransformation<?>[] cts = null;
		final OmeNgffMultiScaleMetadata ms = new OmeNgffMultiScaleMetadata(
				numDimensions, "", name,
				type, version, axes,
				datasets, null, cts, null);

		return new OmeNgffV05Metadata("", new OmeNgffMultiScaleMetadata[]{ ms });
	}

}

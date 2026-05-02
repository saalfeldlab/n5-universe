package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.ScaleCoordinateTransformation;

public class OmeNgffV03MetadataProcessor {

	/**
	 * Estimates implied coordinate transformations from the sizes of the array for each scale.
	 * Adds these transformations to the given {@code OmeNgffDataset}s in place.
	 * 
	 * @param datasets
	 * 	A collection of NGFF v0.3 datasets
	 */
	public static void readProcess(final OmeNgffDataset[] datasets, final DatasetAttributes[] attributes) {

		final int nd = attributes[0].getNumDimensions();
		final double[] unitRes = new double[nd];
		Arrays.fill(unitRes, 1);
		datasets[0].coordinateTransformations = new CoordinateTransformation[]{new ScaleCoordinateTransformation(unitRes)};

		for (int i = 1; i < datasets.length; i++) {
			datasets[i].coordinateTransformations = inferRelativeResolution(attributes[0], attributes[i]);
		}
	}

	private static CoordinateTransformation[] inferRelativeResolution(final DatasetAttributes base, final DatasetAttributes downsampled) {
		final int nd = base.getDimensions().length;
		final double[] res = new double[nd];
		for (int i = 0; i < nd; i++) {
			res[i] = (double)base.getDimensions()[i] / downsampled.getDimensions()[i];
		}
		return new CoordinateTransformation[]{new ScaleCoordinateTransformation(res)};
	}

}

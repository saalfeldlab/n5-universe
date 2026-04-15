package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.ScaleCoordinateTransformation;

public class OmeNgffV03MetadataProcessor
{
	private static ScaleCoordinateTransformation getImpliedScale( final int nd, final int i ) {
		final double[] res = new double[nd];
		Arrays.fill(res, Math.pow(2, i));
		return new ScaleCoordinateTransformation(res);
	}

	private static ScaleCoordinateTransformation[] getScales(final int nd, final int numScales) {
		final ScaleCoordinateTransformation[] cts = new ScaleCoordinateTransformation[numScales];
		for (int i = 0; i < cts.length; i++) {
			cts[i] = getImpliedScale(nd, i);
		}
		return cts;
	}
	
	/**
	 * Adds implied coordinate transformations to the given datasets.
	 * Modifies elements of the given array in place.
	 * 
	 * @param datasets
	 * 	A collection of NGFF v0.3 datasets
	 */
	public static void readProcess(int nd, OmeNgffDataset[] datasets) {
		
		ScaleCoordinateTransformation[] cts = getScales(nd, datasets.length);
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].coordinateTransformations = new CoordinateTransformation[]{cts[i]};
		}
	}
}

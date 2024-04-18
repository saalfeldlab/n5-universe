package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.junit.Test;

public class BuildMultiscaleTest {

	@Test
	public void buildMultiscaleRoot() {

		final String path = "";
		final String downsampleMethod = "sampling";

		final Axis[] axes = AxisUtils.buildAxes("x", "y", "z");
		final OmeNgffMultiScaleMetadataMutable ms = new OmeNgffMultiScaleMetadataMutable("");

		final String[] paths = new String[]{"s0", "s1", "s2", "s3"};
		ms.addChild(buildScaleLevelMetadata(paths[0], new double[]{1, 1, 1}, axes));
		ms.addChild(buildScaleLevelMetadata(paths[1], new double[]{2, 2, 2}, axes));
		ms.addChild(buildScaleLevelMetadata(paths[2], new double[]{4, 4, 4}, axes));
		ms.addChild(buildScaleLevelMetadata(paths[3], new double[]{8, 8, 8}, axes));

		final OmeNgffMultiScaleMetadata meta = new OmeNgffMultiScaleMetadata(ms.getAxes().length,
				path, path, downsampleMethod, "0.4",
				ms.getAxes(),
				ms.getDatasets(), null,
				ms.coordinateTransformations, ms.metadata, true);

		for( int i = 0; i < 4; i++ ) {
			assertEquals( paths[i], meta.getDatasets()[i].path );
		}
	}

	private static NgffSingleScaleAxesMetadata buildScaleLevelMetadata(final String path, final double[] res,
			final Axis[] axes)	{

		return new NgffSingleScaleAxesMetadata(path, res, null, axes, null);
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;

import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.junit.Test;

public class BuildMultiscaleTest {

	@Test
	public void buildNgffMultiscale() {

		testHelper("", new String[]{"s0", "s1", "s2", "s3"});

		testHelper("a", new String[]{"s0", "s1", "s2", "s3"});
		testHelper("a/b", new String[]{"s0", "s1", "s2", "s3"});
		testHelper("a/b/c", new String[]{"s0", "s1", "s2", "s3"});

		testHelper("a", new String[]{"0/s0", "0/s1", "0/s2", "0/s3"});
	}

	private static void testHelper(final String path, final String[] childPaths) {

		final String downsampleMethod = "sampling";
		final Axis[] axes = AxisUtils.buildAxes("x", "y", "z");
		final OmeNgffMultiScaleMetadataMutable ms = new OmeNgffMultiScaleMetadataMutable("");

		for (int i = 0; i < childPaths.length; i++) {
			final double s = Math.pow(2, i);
			ms.addChild(buildScaleLevelMetadata(childPaths[i], new double[]{s, s, s}, axes));
		}

		final OmeNgffMultiScaleMetadata meta = new OmeNgffMultiScaleMetadata(ms.getAxes().length,
				path, path, downsampleMethod, "0.4",
				ms.getAxes(),
				ms.getDatasets(), null,
				ms.coordinateTransformations, ms.metadata, true);

		for (int i = 0; i < childPaths.length; i++) {
			assertEquals(
					String.format("multiscale path incorrect for root: %s, child: %s", path, childPaths[i]),
					childPaths[i], meta.getDatasets()[i].path);
		}

		// test building children from multiscales
		// these metadata's path variables must be relative to the root
		final NgffSingleScaleAxesMetadata[] children = OmeNgffMultiScaleMetadata.buildMetadata(3, path, null, meta);
		for (int i = 0; i < childPaths.length; i++) {
			// ensure the paths are equal up to normalization
			assertEquals(
				String.format("single scale path incorrect for root: %s, child: %s", path, childPaths[i]),
				N5URI.normalizeGroupPath(path + "/" + childPaths[i]),
				N5URI.normalizeGroupPath(children[i].getPath()));
		}
	}

	private static NgffSingleScaleAxesMetadata buildScaleLevelMetadata(final String path, final double[] res,
			final Axis[] axes) {

		return new NgffSingleScaleAxesMetadata(path, res, null, axes, null);
	}

}

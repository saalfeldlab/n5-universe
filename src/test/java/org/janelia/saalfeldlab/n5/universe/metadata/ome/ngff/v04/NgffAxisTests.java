package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;
import org.junit.Test;

import net.imglib2.realtransform.AffineTransform3D;

public class NgffAxisTests {

	private static double EPS = 1e-6;

	@Test
	public void testSpatial3D() {

		// 1D
		final double[] scale1d = new double[] { 2.0 };
		final double[] translation1d = new double[] { 10.0 };

		final AffineTransform3D st1 = new NgffSingleScaleAxesMetadata("", scale1d, translation1d, null).spatialTransform3d();
		assertArrayEquals("1d scale translation", new double[] { 2, 0, 0, 10, 0, 1, 0, 0, 0, 0, 1, 0 },
				st1.getRowPackedCopy(), 1e-9);

		final AffineTransform3D s1 = new NgffSingleScaleAxesMetadata("", scale1d, null, null).spatialTransform3d();
		assertArrayEquals("1d scale", new double[] { 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0 },
				s1.getRowPackedCopy(), 1e-9);

		final AffineTransform3D t1 = new NgffSingleScaleAxesMetadata("", null, translation1d, null).spatialTransform3d();
		assertArrayEquals("1d translation", new double[] { 1, 0, 0, 10, 0, 1, 0, 0, 0, 0, 1, 0 },
				t1.getRowPackedCopy(), 1e-9);


		// 2D
		final double[] scale2d = new double[] { 2.0, 3.0 };
		final double[] translation2d = new double[] { 10.0, 100.0 };

		final AffineTransform3D st2 = new NgffSingleScaleAxesMetadata("", scale2d, translation2d, null).spatialTransform3d();
		assertArrayEquals("2d scale translation", new double[] { 2, 0, 0, 10, 0, 3, 0, 100, 0, 0, 1, 0 },
				st2.getRowPackedCopy(), 1e-9);

		final AffineTransform3D s2 = new NgffSingleScaleAxesMetadata("", scale2d, null, null).spatialTransform3d();
		assertArrayEquals("2d scale", new double[] { 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 1, 0 },
				s2.getRowPackedCopy(), 1e-9);

		final AffineTransform3D t2 = new NgffSingleScaleAxesMetadata("", null, translation2d, null).spatialTransform3d();
		assertArrayEquals("2d translation", new double[] { 1, 0, 0, 10, 0, 1, 0, 100, 0, 0, 1, 0 },
				t2.getRowPackedCopy(), 1e-9);


		// 3D
		final double[] scale3d = new double[] { 2.0, 3.0, 4.0 };
		final double[] translation3d = new double[] { 10.0, 100.0, 1000.0 };
		final Axis[] spaceAxes = AxisUtils.defaultAxes("x", "y", "z");

		final AffineTransform3D st3 = new NgffSingleScaleAxesMetadata("", scale3d, translation3d, spaceAxes, null).spatialTransform3d();
		assertArrayEquals("3d scale translation", new double[] { 2, 0, 0, 10, 0, 3, 0, 100, 0, 0, 4, 1000 },
				st3.getRowPackedCopy(), 1e-9);

		final AffineTransform3D s3 = new NgffSingleScaleAxesMetadata("", scale3d, null, spaceAxes, null).spatialTransform3d();
		assertArrayEquals("3d scale", new double[] { 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 4, 0 },
				s3.getRowPackedCopy(), 1e-9);

		final AffineTransform3D t3 = new NgffSingleScaleAxesMetadata("", null, translation3d, spaceAxes, null).spatialTransform3d();
		assertArrayEquals("3d scale translation", new double[] { 1, 0, 0, 10, 0, 1, 0, 100, 0, 0, 1, 1000 },
				t3.getRowPackedCopy(), 1e-9);

		// 5D
		final double[] scale5d = new double[] { 2.0, 3.0, 4.0, 5.0, 6.0 };
		final double[] translation5d = new double[] { 10.0, 100.0, 1000.0, -1.0, -10.0 };
		Axis[] axes = AxisUtils.defaultAxes("x", "y", "z", "c", "t");

		AffineTransform3D st5 = new NgffSingleScaleAxesMetadata("", scale5d, translation5d, axes, null).spatialTransform3d();
		assertArrayEquals("5d scale translation xyzct", new double[] { 2, 0, 0, 10, 0, 3, 0, 100, 0, 0, 4, 1000 },
				st5.getRowPackedCopy(), 1e-9);

		axes = AxisUtils.defaultAxes("c", "x", "y", "z", "t");
		st5 = new NgffSingleScaleAxesMetadata("", scale5d, translation5d, axes, null).spatialTransform3d();
		assertArrayEquals("5d scale translation cxyzt", new double[] { 3, 0, 0, 100, 0, 4, 0, 1000, 0, 0, 5, -1 },
				st5.getRowPackedCopy(), 1e-9);

		axes = AxisUtils.defaultAxes("c", "x", "t", "y", "z" );
		st5 = new NgffSingleScaleAxesMetadata("", scale5d, translation5d, axes, null).spatialTransform3d();
		assertArrayEquals("5d scale translation cxtyz", new double[] { 3, 0, 0, 100, 0, 5, 0, -1, 0, 0, 6, -10 },
				st5.getRowPackedCopy(), 1e-9);
	}

	@Test
	public void testAxisOrderStorageOrder() {

		final URI rootF = Paths.get("src", "test", "resources", "metadata.zarr").toUri();
		final N5Reader zarr = new N5Factory().openReader(rootF.toString());

		final String[] names = new String[]{"c", "x", "y", "z"};
		ArrayUtils.reverse(names);

		// the expected scales and translations are reversed versions
		// of the arrays appearing in the JSON
		final double[] expectedScales = new double[]{13, 12, 11, 1};
		final double[] expectedTranslations = new double[]{3, 2, 1, 0};

		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		// flip when f-Order
		final N5TreeNode fOrderNode = CoordinateTransformParsingTest.setupNode(zarr, "fOrder", "1");
		axisOrderTest(parser.parseMetadata(zarr, fOrderNode), names, expectedScales, expectedTranslations);

		// and flip when c-Order
		final N5TreeNode cOrderNode = CoordinateTransformParsingTest.setupNode(zarr, "cOrder", "1");
		axisOrderTest(parser.parseMetadata(zarr, cOrderNode), names, expectedScales, expectedTranslations);
	}

	private void axisOrderTest(final Optional<OmeNgffMetadata> metaOpt, final String[] expectedNames,
			final double[] expectedScales, final double[] expectedTranslations) {

		assertTrue("ss not parsable", metaOpt.isPresent());

		final OmeNgffMetadata meta = metaOpt.get();
		assertTrue("no multiscales found", meta.multiscales.length > 0);

		final Axis[] axes = meta.multiscales[0].axes;
		final String[] names = Arrays.stream(axes).map(a -> a.getName()).toArray(N -> new String[N]);
		assertArrayEquals("names don't match", expectedNames, names);

		final CoordinateTransformation<?>[] cts = meta.multiscales[0].datasets[0].coordinateTransformations;
		assertTrue("first coordinate transform not scale", cts[0] instanceof ScaleCoordinateTransformation);
		final ScaleCoordinateTransformation ct0 = (ScaleCoordinateTransformation)cts[0];
		assertArrayEquals("scales don't match", expectedScales, ct0.getScale(), EPS);

		assertTrue("second coordinate transform not translation", cts[1] instanceof TranslationCoordinateTransformation);
		final TranslationCoordinateTransformation ct1 = (TranslationCoordinateTransformation)cts[1];
		assertArrayEquals("translations don't match", expectedTranslations, ct1.getTranslation(), EPS);
	}

}

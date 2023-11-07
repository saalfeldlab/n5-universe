package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertArrayEquals;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.junit.Test;

import net.imglib2.realtransform.AffineTransform3D;

public class NgffAxisTests {

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

}

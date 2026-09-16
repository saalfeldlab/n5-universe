package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.TranslationCoordinateTransform;
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

		// f- and c-order multiscales metadata should have axes, scales, and translations
		// in the same order after parsing (reversed relative to the JSON).
		// The children's axis parameters are aligned with their array's dimensions,
		// so they are reversed again for f-order arrays.
		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();
		final String[] expectedNames = new String[]{"z", "y", "x", "c"};
		final double[] expectedScales = new double[] { 13, 12, 11, 1 };
		final double[] expectedTranslations = new double[] { 3, 2, 1, 0 };

		// f-Order
		// n5-zarr does not reverse dimensions of f-order arrays,
		// so the dimensions are in the same order as the JSON shape
		final long[] expectedDimensionsF = new long[] { 1, 4096, 4096, 1536 };
		final N5TreeNode fOrderNode = CoordinateTransformParsingTest.setupNode(zarr, "fOrder", "1");
		axisOrderTest(parser.parseMetadata(zarr, fOrderNode), expectedNames, expectedScales, expectedTranslations,
				expectedDimensionsF, new int[] { 3, 2, 1, 0 });

		// c-Order
		// n5-zarr reverses dimensions of c-order arrays,
		// so the dimensions are reversed relative to the JSON shape
		final long[] expectedDimensionsC = new long[] { 1536, 4096, 4096, 1 };
		final N5TreeNode cOrderNode = CoordinateTransformParsingTest.setupNode(zarr, "cOrder", "1");
		axisOrderTest(parser.parseMetadata(zarr, cOrderNode), expectedNames, expectedScales, expectedTranslations,
				expectedDimensionsC, null);
	}

	@Test
	public void testPermutationFromParentPropagates() {

		final int[] reversal = new int[] { 2, 1, 0 };
		final NgffSingleScaleAxesMetadata child = new NgffSingleScaleAxesMetadata("s0",
				new double[] { 1, 2, 3 }, new double[] { 4, 5, 6 },
				AxisUtils.defaultAxes("x", "y", "z"), null, reversal);

		assertArrayEquals("permutation from parent", reversal, child.getPermutationFromParent());
		assertNull("no parent", new NgffSingleScaleAxesMetadata("s0",
				new double[] { 1, 2, 3 }, null, AxisUtils.defaultAxes("x", "y", "z"), null)
				.getPermutationFromParent());

		// the getter returns a copy
		child.getPermutationFromParent()[0] = 99;
		assertArrayEquals("getter returns a copy", reversal, child.getPermutationFromParent());

		assertArrayEquals("modifySpatialTransform keeps permutation", reversal,
				child.modifySpatialTransform("s1", new AffineTransform3D()).getPermutationFromParent());

		assertArrayEquals("buildNgffMetadata keeps permutation", reversal,
				MetadataUtils.buildNgffMetadata("s1", child, new double[] { 2, 4, 6 }, new double[3])
						.getPermutationFromParent());

		// permuting again composes: new[i] = parent[reversal[q[i]]]
		final int[] q = new int[] { 1, 0, 2 };
		final NgffSingleScaleAxesMetadata permuted = MetadataUtils.permuteNgffMetadata(child, q);
		assertArrayEquals("permuteNgffMetadata composes permutation", new int[] { 1, 2, 0 },
				permuted.getPermutationFromParent());
		final double[] parentScale = new double[] { 3, 2, 1 };
		assertArrayEquals("composed permutation maps parent scale to child scale",
				permuted.getScale(), AxisUtils.permute(parentScale, permuted.getPermutationFromParent()), EPS);
	}

	// The expected names, scales, and translations are those of the multiscales metadata.
	// The expected child values are derived from these by applying expectedPermutation.
	// The expected dimensions are those of the child array.
	private void axisOrderTest(final Optional<OmeNgffMetadata> metaOpt,
			final String[] expectedNames,
			final double[] expectedScales,
			final double[] expectedTranslations,
			final long[] expectedDimension,
			final int[] expectedPermutation) {

		assertTrue("not parsable", metaOpt.isPresent());

		final OmeNgffMetadata meta = metaOpt.get();
		assertTrue("no multiscales found", meta.multiscales.length > 0);

		final Axis[] axes = meta.multiscales[0].axes;
		final String[] names = Arrays.stream(axes).map(a -> a.getName()).toArray(N -> new String[N]);
		assertArrayEquals("names do not match", expectedNames, names);

		final CoordinateTransform<?>[] cts = meta.multiscales[0].datasets[0].coordinateTransformations;

		assertTrue("first coordinate transform not scale", cts[0] instanceof ScaleCoordinateTransform);
		final ScaleCoordinateTransform ct0 = (ScaleCoordinateTransform)cts[0];
		assertArrayEquals("scales do not match", expectedScales, ct0.scale, EPS);

		assertTrue("second coordinate transform not translation", cts[1] instanceof TranslationCoordinateTransform);
		final TranslationCoordinateTransform ct1 = (TranslationCoordinateTransform)cts[1];
		assertArrayEquals("translations do not match", expectedTranslations, ct1.translation, EPS);

		// test child
		final NgffSingleScaleAxesMetadata child = meta.getChildrenMetadata()[0];
		final DatasetAttributes attrs = child.getAttributes();

		assertArrayEquals("permutation from parent does not match", expectedPermutation,
				child.getPermutationFromParent());

		String[] childNames = Arrays.copyOf(expectedNames, expectedNames.length);
		double[] childScales = Arrays.copyOf(expectedScales, expectedScales.length);
		double[] childTranslations = Arrays.copyOf(expectedTranslations, expectedTranslations.length);
		if (expectedPermutation != null) {
			AxisUtils.permute(childNames, childNames, expectedPermutation);
			childScales = AxisUtils.permute(childScales, expectedPermutation);
			childTranslations = AxisUtils.permute(childTranslations, expectedPermutation);
		}

		assertArrayEquals("dimensions do not match", expectedDimension, attrs.getDimensions());
		assertArrayEquals("child names do not match", childNames, child.getAxisLabels());
		assertArrayEquals("child scales do not match", childScales, child.getScale(), EPS);
		assertArrayEquals("child translations do not match", childTranslations, child.getTranslation(), EPS);
	}

}

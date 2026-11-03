package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.BijectionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ByDimensionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.GeneralAffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.InvertibleAffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.MapAxisCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.RotationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.StackedCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.SubAxisTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ThinPlateSplineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.TranslationCoordinateTransform;
import org.junit.Test;

import net.imglib2.type.numeric.real.FloatType;

/**
 * Uses the options of each transform from outside of the package of the transforms, as callers do.
 * The options of a transform are created by its static {@code options()} method, and the transform
 * with {@code build()}.
 */
public class TransformOptionsTest {

	private static final double[][] AFFINE = { { 2, 0, 1 }, { 0, 3, 2 } };

	@Test
	public void testSetterOrderDoesNotMatter() {

		final ScaleCoordinateTransform nameFirst = ScaleCoordinateTransform.options()
				.name("s").input("in").output("out").scale(new double[] { 2, 3 }).build();

		final ScaleCoordinateTransform scaleFirst = ScaleCoordinateTransform.options()
				.scale(new double[] { 2, 3 }).output("out").input("in").name("s").build();

		for (final ScaleCoordinateTransform ct : new ScaleCoordinateTransform[] { nameFirst, scaleFirst }) {
			assertEquals("s", ct.getName());
			assertEquals("in", ct.getInput().getName());
			assertEquals("out", ct.getOutput().getName());
			assertArrayEquals(new double[] { 2, 3 }, ct.scale, 0.0);
			assertNotNull(ct.getTransform());
		}
	}

	@Test
	public void testUnsetFieldsAreNull() {

		final ScaleCoordinateTransform ct = ScaleCoordinateTransform.options().scale(new double[] { 2 }).build();
		assertNull(ct.getName());
		assertNull(ct.getInput());
		assertNull(ct.getOutput());
		assertNull(ct.getParameterPath());

		assertEquals(0, SequenceCoordinateTransform.options().build().getTransformations().length);
	}

	@Test
	public void testOptionsDoNotShareArrays() {

		final double[] scale = { 2, 3 };
		final ScaleCoordinateTransform ct = ScaleCoordinateTransform.options().scale(scale).build();
		scale[0] = 100;
		assertEquals(2, ct.scale[0], 0.0);
		assertEquals(2, ct.getTransform().getScale(0), 0.0);
	}

	@Test
	public void testBuildEveryType() {

		final double[] v = { 1, 2 };

		assertEquals(TranslationCoordinateTransform.TYPE, TranslationCoordinateTransform.options()
				.name("t").translation(v).build().getType());

		assertEquals(RotationCoordinateTransform.TYPE, RotationCoordinateTransform.options()
				.name("r").rotation(new double[][] { { 1, 0 }, { 0, 1 } }).build().getType());

		assertEquals(InvertibleAffineCoordinateTransform.TYPE, InvertibleAffineCoordinateTransform.options()
				.name("a").affine(AFFINE).build().getType());

		assertEquals(GeneralAffineCoordinateTransform.TYPE, GeneralAffineCoordinateTransform.options()
				.name("g").affine(AFFINE).build().getType());

		assertEquals(IdentityCoordinateTransform.TYPE, IdentityCoordinateTransform.options()
				.name("i").build().getType());

		final IdentityCoordinateTransform id = IdentityCoordinateTransform.options().build();
		final BijectionCoordinateTransform bijection = BijectionCoordinateTransform.options()
				.name("b").forward(id).inverse(id).build();
		assertSame(id, bijection.getForward());

		final ByDimensionCoordinateTransform byDimension = ByDimensionCoordinateTransform.options()
				.name("bd").input("a").output("b")
				.transformations(new SubAxisTransformation(
						ScaleCoordinateTransform.options().scale(v).build(), new int[] { 0, 1 }, new int[] { 0, 1 }))
				.build();
		assertEquals(1, byDimension.getTransformations().length);

		assertNotNull(StackedCoordinateTransform.options().name("st").transforms().build());

		assertArrayEquals(new int[] { 1, 0 }, MapAxisCoordinateTransform.options()
				.name("m").axisMapping(new int[] { 1, 0 }).build().getAxisMapping());

		final ThinPlateSplineCoordinateTransform tps = ThinPlateSplineCoordinateTransform.options()
				.name("tps")
				.points(new double[][] { { 0, 1, 0, 1 }, { 0, 0, 1, 1 } },
						new double[][] { { 0, 1, 0, 2 }, { 0, 0, 1, 2 } })
				.build();
		assertNotNull(tps.getTransform());

		assertEquals("p", DisplacementFieldCoordinateTransform.<FloatType>options()
				.name("d").path("p").build().getParameterPath());

		assertEquals("p", CoordinateFieldCoordinateTransform.<FloatType>options()
				.name("c").path("p").interpolation("nearest").build().getParameterPath());

		assertEquals("nearest", CoordinateFieldCoordinateTransform.<FloatType>options()
				.interpolation("nearest").path("p").build().getInterpolation());
	}

	@Test
	public void testCopyReplacesParametersWithTheTypesOptions() {

		final ScaleCoordinateTransform scale = ScaleCoordinateTransform.options()
				.name("s").scale(new double[] { 2, 3 }).build();

		final ScaleCoordinateTransform rescaled = scale.copy(ScaleCoordinateTransform.options().scale(new double[] { 5, 6 }));
		assertArrayEquals(new double[] { 5, 6 }, rescaled.scale, 0.0);
		assertEquals("s", rescaled.getName());
		assertArrayEquals(new double[] { 2, 3 }, scale.scale, 0.0);

		final TranslationCoordinateTransform translation = TranslationCoordinateTransform.options()
				.translation(new double[] { 1, 2 }).build();
		assertArrayEquals(new double[] { 7, 8 },
				translation.copy(TranslationCoordinateTransform.options().translation(new double[] { 7, 8 })).translation, 0.0);

		final InvertibleAffineCoordinateTransform affine = InvertibleAffineCoordinateTransform.options()
				.affine(AFFINE).build();
		final InvertibleAffineCoordinateTransform movedAffine = affine.copy(
				InvertibleAffineCoordinateTransform.options().affine(new double[][] { { 1, 0, 5 }, { 0, 1, 6 } }));
		assertEquals(5, movedAffine.affine[0][2], 0.0);
		assertEquals(1, affine.affine[0][2], 0.0);

		final SequenceCoordinateTransform sequence = SequenceCoordinateTransform.options().name("seq")
				.transformations(scale).build();
		final SequenceCoordinateTransform replaced = sequence.copy(SequenceCoordinateTransform.options()
				.transformations(scale, translation));
		assertEquals(2, replaced.getTransformations().length);
		assertEquals(1, sequence.getTransformations().length);
		assertEquals("seq", replaced.getName());
	}

	@Test
	public void testCopyKeepsParametersThatAreNotSet() {

		final ScaleCoordinateTransform scale = ScaleCoordinateTransform.options()
				.name("s").scale(new double[] { 2, 3 }).build();

		final ScaleCoordinateTransform renamed = scale.copy(ScaleCoordinateTransform.options().name("s2"));
		assertEquals("s2", renamed.getName());
		assertArrayEquals(new double[] { 2, 3 }, renamed.scale, 0.0);
		assertNotSame(scale.scale, renamed.scale);
	}

	@Test
	public void testFieldCopyCanNotChangeInterpolation() {

		final DisplacementFieldCoordinateTransform<FloatType> df = DisplacementFieldCoordinateTransform.<FloatType>options()
				.path("p").build();

		assertEquals("linear", df.copy().getInterpolation());

		try {
			df.copy(DisplacementFieldCoordinateTransform.<FloatType>options().interpolation("nearest"));
			fail("the interpolation of a field can not be changed");
		} catch (final UnsupportedOperationException e) {}

		// the same interpolation is fine
		assertEquals("linear", df.copy(DisplacementFieldCoordinateTransform.<FloatType>options().interpolation("linear"))
				.getInterpolation());
	}

	@Test
	public void testPathOnlyForParametrizedTransforms() {

		final CoordinateTransform<?> identity = IdentityCoordinateTransform.options().build();
		try {
			identity.copy(IdentityCoordinateTransform.options().path("p"));
			fail("identity has no parameter path");
		} catch (final UnsupportedOperationException e) {}

		assertEquals("p", ScaleCoordinateTransform.options().path("p").build()
				.copy(ScaleCoordinateTransform.options().name("n")).getParameterPath());
	}
}

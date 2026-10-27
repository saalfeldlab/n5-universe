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
				.withName("s").withInput("in").withOutput("out").withScale(new double[] { 2, 3 }).build();

		final ScaleCoordinateTransform scaleFirst = ScaleCoordinateTransform.options()
				.withScale(new double[] { 2, 3 }).withOutput("out").withInput("in").withName("s").build();

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

		final ScaleCoordinateTransform ct = ScaleCoordinateTransform.options().withScale(new double[] { 2 }).build();
		assertNull(ct.getName());
		assertNull(ct.getInput());
		assertNull(ct.getOutput());
		assertNull(ct.getParameterPath());

		assertEquals(0, SequenceCoordinateTransform.options().build().getTransformations().length);
	}

	@Test
	public void testOptionsDoNotShareArrays() {

		final double[] scale = { 2, 3 };
		final ScaleCoordinateTransform ct = ScaleCoordinateTransform.options().withScale(scale).build();
		scale[0] = 100;
		assertEquals(2, ct.scale[0], 0.0);
		assertEquals(2, ct.getTransform().getScale(0), 0.0);
	}

	@Test
	public void testBuildEveryType() {

		final double[] v = { 1, 2 };

		assertEquals(TranslationCoordinateTransform.TYPE, TranslationCoordinateTransform.options()
				.withName("t").withTranslation(v).build().getType());

		assertEquals(RotationCoordinateTransform.TYPE, RotationCoordinateTransform.options()
				.withName("r").withRotation(new double[][] { { 1, 0 }, { 0, 1 } }).build().getType());

		assertEquals(InvertibleAffineCoordinateTransform.TYPE, InvertibleAffineCoordinateTransform.options()
				.withName("a").withAffine(AFFINE).build().getType());

		assertEquals(GeneralAffineCoordinateTransform.TYPE, GeneralAffineCoordinateTransform.options()
				.withName("g").withAffine(AFFINE).build().getType());

		assertEquals(IdentityCoordinateTransform.TYPE, IdentityCoordinateTransform.options()
				.withName("i").build().getType());

		final IdentityCoordinateTransform id = IdentityCoordinateTransform.options().build();
		final BijectionCoordinateTransform bijection = BijectionCoordinateTransform.options()
				.withName("b").withForward(id).withInverse(id).build();
		assertSame(id, bijection.getForward());

		final ByDimensionCoordinateTransform byDimension = ByDimensionCoordinateTransform.options()
				.withName("bd").withInput("a").withOutput("b")
				.withTransformations(new SubAxisTransformation(
						ScaleCoordinateTransform.options().withScale(v).build(), new int[] { 0, 1 }, new int[] { 0, 1 }))
				.build();
		assertEquals(1, byDimension.getTransformations().length);

		assertNotNull(StackedCoordinateTransform.options().withName("st").withTransforms().build());

		assertArrayEquals(new int[] { 1, 0 }, MapAxisCoordinateTransform.options()
				.withName("m").withAxisMapping(new int[] { 1, 0 }).build().getAxisMapping());

		final ThinPlateSplineCoordinateTransform tps = ThinPlateSplineCoordinateTransform.options()
				.withName("tps")
				.withPoints(new double[][] { { 0, 1, 0, 1 }, { 0, 0, 1, 1 } },
						new double[][] { { 0, 1, 0, 2 }, { 0, 0, 1, 2 } })
				.build();
		assertNotNull(tps.getTransform());

		assertEquals("p", DisplacementFieldCoordinateTransform.<FloatType>options()
				.withName("d").withPath("p").build().getParameterPath());

		assertEquals("p", CoordinateFieldCoordinateTransform.<FloatType>options()
				.withName("c").withPath("p").withInterpolation("nearest").build().getParameterPath());

		assertEquals("nearest", CoordinateFieldCoordinateTransform.<FloatType>options()
				.withInterpolation("nearest").withPath("p").build().getInterpolation());
	}

	@Test
	public void testCopyReplacesParametersWithTheTypesOptions() {

		final ScaleCoordinateTransform scale = ScaleCoordinateTransform.options()
				.withName("s").withScale(new double[] { 2, 3 }).build();

		final ScaleCoordinateTransform rescaled = scale.copy(ScaleCoordinateTransform.options().withScale(new double[] { 5, 6 }));
		assertArrayEquals(new double[] { 5, 6 }, rescaled.scale, 0.0);
		assertEquals("s", rescaled.getName());
		assertArrayEquals(new double[] { 2, 3 }, scale.scale, 0.0);

		final TranslationCoordinateTransform translation = TranslationCoordinateTransform.options()
				.withTranslation(new double[] { 1, 2 }).build();
		assertArrayEquals(new double[] { 7, 8 },
				translation.copy(TranslationCoordinateTransform.options().withTranslation(new double[] { 7, 8 })).translation, 0.0);

		final InvertibleAffineCoordinateTransform affine = InvertibleAffineCoordinateTransform.options()
				.withAffine(AFFINE).build();
		final InvertibleAffineCoordinateTransform movedAffine = affine.copy(
				InvertibleAffineCoordinateTransform.options().withAffine(new double[][] { { 1, 0, 5 }, { 0, 1, 6 } }));
		assertEquals(5, movedAffine.affine[0][2], 0.0);
		assertEquals(1, affine.affine[0][2], 0.0);

		final SequenceCoordinateTransform sequence = SequenceCoordinateTransform.options().withName("seq")
				.withTransformations(scale).build();
		final SequenceCoordinateTransform replaced = sequence.copy(SequenceCoordinateTransform.options()
				.withTransformations(scale, translation));
		assertEquals(2, replaced.getTransformations().length);
		assertEquals(1, sequence.getTransformations().length);
		assertEquals("seq", replaced.getName());
	}

	@Test
	public void testCopyKeepsParametersThatAreNotSet() {

		final ScaleCoordinateTransform scale = ScaleCoordinateTransform.options()
				.withName("s").withScale(new double[] { 2, 3 }).build();

		final ScaleCoordinateTransform renamed = scale.copy(ScaleCoordinateTransform.options().withName("s2"));
		assertEquals("s2", renamed.getName());
		assertArrayEquals(new double[] { 2, 3 }, renamed.scale, 0.0);
		assertNotSame(scale.scale, renamed.scale);
	}

	@Test
	public void testFieldCopyCanNotChangeInterpolation() {

		final DisplacementFieldCoordinateTransform<FloatType> df = DisplacementFieldCoordinateTransform.<FloatType>options()
				.withPath("p").build();

		assertEquals("linear", df.copy().getInterpolation());

		try {
			df.copy(DisplacementFieldCoordinateTransform.<FloatType>options().withInterpolation("nearest"));
			fail("the interpolation of a field can not be changed");
		} catch (final UnsupportedOperationException e) {}

		// the same interpolation is fine
		assertEquals("linear", df.copy(DisplacementFieldCoordinateTransform.<FloatType>options().withInterpolation("linear"))
				.getInterpolation());
	}

	@Test
	public void testPathOnlyForParametrizedTransforms() {

		final CoordinateTransform<?> identity = IdentityCoordinateTransform.options().build();
		try {
			identity.copy(IdentityCoordinateTransform.options().withPath("p"));
			fail("identity has no parameter path");
		} catch (final UnsupportedOperationException e) {}

		assertEquals("p", ScaleCoordinateTransform.options().withPath("p").build()
				.copy(ScaleCoordinateTransform.options().withName("n")).getParameterPath());
	}
}

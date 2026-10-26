package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.AbstractCoordinateTransform.BasicOptions;
import org.junit.Test;

public class CoordinateTransformCopyTest {

	private static ScaleCoordinateTransform scale(final String name, final String input, final String output,
			final double... scale) {

		return new ScaleCoordinateTransform(ScaleCoordinateTransform.options()
				.withName(name).withInput(input).withOutput(output).withScale(scale));
	}

	private static TranslationCoordinateTransform translation(final String name, final String input, final String output,
			final double... translation) {

		return new TranslationCoordinateTransform(TranslationCoordinateTransform.options()
				.withName(name).withInput(input).withOutput(output).withTranslation(translation));
	}

	private static SequenceCoordinateTransform sequence(final String name, final String input, final String output,
			final CoordinateTransform<?>... transformations) {

		return new SequenceCoordinateTransform(SequenceCoordinateTransform.options()
				.withName(name).withInput(input).withOutput(output).withTransformations(transformations));
	}

	@Test
	public void testCopyKeepsFields() {

		final ScaleCoordinateTransform ct = scale("s", "in", "out", 2, 3);
		final ScaleCoordinateTransform copy = ct.copy();

		assertNotSame(ct, copy);
		assertEquals(ct.getType(), copy.getType());
		assertEquals("s", copy.getName());
		assertEquals(ct.getInput().getName(), copy.getInput().getName());
		assertEquals(ct.getOutput().getName(), copy.getOutput().getName());
		assertArrayEquals(ct.scale, copy.scale, 0.0);
		assertNotSame(ct.scale, copy.scale);
		assertNotNull(copy.getTransform());
		assertNotSame(ct.getTransform(), copy.getTransform());
	}

	@Test
	public void testCopyReplacesSetFields() {

		final TranslationCoordinateTransform ct = translation("t", "in", "out", 1, 2);

		final TranslationCoordinateTransform renamed = ct.copy(new BasicOptions().withName("t2"));
		assertEquals("t2", renamed.getName());
		assertEquals("in", renamed.getInput().getName());
		assertEquals("out", renamed.getOutput().getName());

		final TranslationCoordinateTransform moved = ct.copy(new BasicOptions().withInput("a").withOutput("b"));
		assertEquals("t", moved.getName());
		assertEquals("a", moved.getInput().getName());
		assertEquals("b", moved.getOutput().getName());

		// the original is unchanged
		assertEquals("t", ct.getName());
		assertEquals("in", ct.getInput().getName());
	}

	@Test
	public void testCopySetToNull() {

		final IdentityCoordinateTransform ct = new IdentityCoordinateTransform("id", "in", "out");
		final IdentityCoordinateTransform copy = ct.copy(new BasicOptions().withName(null).withInput((String)null));

		assertNull(copy.getName());
		assertNull(copy.getInput());
		assertEquals("out", copy.getOutput().getName());
	}

	@Test
	public void testAffineCopyIsDeep() {

		final double[][] matrix = new double[][]{{1, 0, 5}, {0, 2, 6}};
		final InvertibleAffineCoordinateTransform ct = new InvertibleAffineCoordinateTransform("a", "in", "out", matrix);
		final InvertibleAffineCoordinateTransform copy = ct.copy();

		assertArrayEquals(matrix[1], copy.affine[1], 0.0);
		assertNotSame(ct.affine, copy.affine);
		assertNotSame(ct.affine[0], copy.affine[0]);
		assertNotNull(copy.getTransform());
	}

	@Test
	public void testSequenceCopiesChildren() {

		final ScaleCoordinateTransform scale = scale("s", "a", "b", 2, 2);
		final TranslationCoordinateTransform translation = translation("t", "b", "c", 1, 1);
		final SequenceCoordinateTransform seq = sequence("seq", "a", "c", scale, translation);

		final SequenceCoordinateTransform copy = seq.copy(new BasicOptions().withName("seq2"));

		assertEquals("seq2", copy.getName());
		assertEquals(2, copy.getTransformations().length);
		assertNotSame(scale, copy.getTransformations()[0]);
		assertEquals("s", copy.getTransformations()[0].getName());

		final double[] p = new double[]{1, 1};
		final double[] expected = new double[2];
		final double[] actual = new double[2];
		seq.getTransform().apply(p, expected);
		copy.getTransform().apply(p, actual);
		assertArrayEquals(expected, actual, 1e-9);
	}

	@Test
	public void testInverseCopiesWrapped() {

		final ScaleCoordinateTransform scale = scale("s", "a", "b", 2, 4);
		final InverseCoordinateTransform<?, ?> inv = new InverseCoordinateTransform<>(scale);
		final InverseCoordinateTransform<?, ?> copy = inv.copy();

		assertNotSame(inv.getWrappedCoordinateTransform(), copy.getWrappedCoordinateTransform());

		final double[] p = new double[]{2, 4};
		final double[] expected = new double[2];
		final double[] actual = new double[2];
		inv.getTransform().apply(p, expected);
		copy.getTransform().apply(p, actual);
		assertArrayEquals(expected, actual, 1e-9);
	}

	@Test
	public void testWithPath() {

		final ScaleCoordinateTransform ct = new ScaleCoordinateTransform(ScaleCoordinateTransform.options()
				.withName("s").withInput("in").withOutput("out").withPath("a/scale"));
		assertEquals("a/scale", ct.getParameterPath());

		final ScaleCoordinateTransform copy = ct.copy(new BasicOptions().withPath("b/scale"));
		assertEquals("b/scale", copy.getParameterPath());
		assertEquals("a/scale", ct.getParameterPath());
		assertEquals("s", copy.getName());

		// unset keeps the path, null clears it
		assertEquals("a/scale", ct.copy().getParameterPath());
		assertNull(ct.copy(new BasicOptions().withPath(null)).getParameterPath());
	}

	@Test
	public void testWithPathResetsAbsolutePath() {

		final InvertibleAffineCoordinateTransform ct = new InvertibleAffineCoordinateTransform("a", "in", "out", "rel/affine");
		ct.resolveAbsolutePath("group");
		assertEquals("group/rel/affine", ct.getParameterPath());
		assertEquals("group/rel/affine", ct.copy().getParameterPath());

		final InvertibleAffineCoordinateTransform copy = ct.copy(new BasicOptions().withPath("other/affine"));
		assertEquals("other/affine", copy.getParameterPath());
	}

	@Test
	public void testWithPathKeepsParameters() {

		final InvertibleAffineCoordinateTransform ct = new InvertibleAffineCoordinateTransform(
				"a", "in", "out", new double[][]{{1, 0, 5}, {0, 2, 6}});
		final InvertibleAffineCoordinateTransform copy = ct.copy(new BasicOptions().withPath("some/path"));

		assertEquals("some/path", copy.getParameterPath());
		assertArrayEquals(ct.affine[1], copy.affine[1], 0.0);
		assertNotNull(copy.getTransform());
	}

	@Test
	public void testWithPathUnsupported() {

		try {
			new IdentityCoordinateTransform("id", "in", "out").copy(new BasicOptions().withPath("p"));
			fail("identity has no parameter path");
		} catch (final UnsupportedOperationException e) {}

		try {
			final ScaleCoordinateTransform scale = scale("s", "a", "b", 2, 2);
			sequence("seq", "a", "b", scale).copy(new BasicOptions().withPath("p"));
			fail("sequence has no parameter path");
		} catch (final UnsupportedOperationException e) {}
	}

	@Test
	public void testCopyViaInterface() {

		final CoordinateTransform<?> ct = new IdentityCoordinateTransform("id", "in", "out");
		final CoordinateTransform<?> copy = ct.copy();
		assertEquals(IdentityCoordinateTransform.class, copy.getClass());
		assertNotSame(ct, copy);
		assertSame(ct.getType(), copy.getType());
	}
}

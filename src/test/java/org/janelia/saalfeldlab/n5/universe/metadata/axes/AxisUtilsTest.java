package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class AxisUtilsTest {

	private static final String[] XYCZT = new String[]{"x", "y", "c", "z", "t"};

	@Test
	public void testFindPermutationByName() {

		final String[] zyx = new String[]{"z", "y", "x"};
		assertArrayEquals(new int[]{2, 1, 0}, AxisUtils.findPermutationByName(zyx, "x", "y", "z"));
		assertArrayEquals(new int[]{0, 1, 2}, AxisUtils.findPermutationByName(zyx, "z", "y", "x"));

		// target is a subset of the source
		assertArrayEquals(new int[]{2, 0}, AxisUtils.findPermutationByName(zyx, "x", "z"));

		// axisLabels[p[i]] == targetLabels[i]
		final String[] labels = new String[]{"t", "c", "z", "y", "x"};
		final int[] p = AxisUtils.findPermutationByName(labels, XYCZT);
		for (int i = 0; i < p.length; i++)
			assertEquals(XYCZT[i], labels[p[i]]);
	}

	@Test
	public void testFindPermutationByNameMissingLabels() {

		final String[] zyx = new String[]{"z", "y", "x"};
		assertArrayEquals(new int[]{2, 1, -1, 0, -1}, AxisUtils.findPermutationByName(zyx, XYCZT));
		assertArrayEquals(new int[]{-1}, AxisUtils.findPermutationByName(zyx, "q"));
		assertArrayEquals(new int[]{}, AxisUtils.findPermutationByName(zyx));
	}

	@Test
	public void testFindPermutationByNameOverloads() {

		final String[] labels = new String[]{"t", "z", "y", "x"};
		final Axis[] axes = Arrays.stream(labels).map(Axis::new).toArray(Axis[]::new);

		final int[] expected = AxisUtils.findPermutationByName(labels, XYCZT);
		assertArrayEquals(new int[]{3, 2, -1, 1, 0}, expected);
		assertArrayEquals(expected, AxisUtils.findPermutationByName(axes, XYCZT));
	}

	@Test
	public void testBuildAxesNames() {

		final Axis[] axes = AxisUtils.buildAxes("x", "y", "z");
		assertArrayEquals(new String[]{"x", "y", "z"},
				Arrays.stream(axes).map(Axis::getName).toArray(String[]::new));
		assertArrayEquals(new int[]{2, 1, 0}, AxisUtils.findPermutationByName(axes, "z", "y", "x"));
	}

	@Test
	public void testFillPermutation() {

		// missing target labels are filled with new indexes after the largest
		final int[] p = new int[]{2, 1, -1, 0, -1};
		AxisUtils.fillPermutation(p);
		assertArrayEquals(new int[]{2, 1, 3, 0, 4}, p);
	}

	@Test
	@Ignore("Need to decide fillPermutation's behavior")
	public void testFillPermutationWithUnmatchedSourceAxis() {

		// the source axis "q" is not a target, so index 2 is never used
		final int[] p = AxisUtils.findPermutationByName(new String[]{"x", "y", "q", "z"}, XYCZT);
		assertArrayEquals(new int[]{0, 1, -1, 3, -1}, p);

		// the filled result should be a valid permutation of 0..4
		AxisUtils.fillPermutation(p);
		assertArrayEquals(new int[]{0, 1, 2, 3, 4}, Arrays.stream(p).sorted().toArray());
	}

	@Test
	public void testPermuteImage5D() {

		final Axis[] axes = axes("t", "c", "z", "y", "x");
		final ArrayImg<IntType, IntArray> img = ArrayImgs.ints(2, 3, 4, 5, 6);
		fillWithPositions(img);

		final RandomAccessibleInterval<IntType> out = AxisUtils.permute(img, axes, XYCZT);
		assertArrayEquals(new long[]{6, 5, 3, 4, 2}, Intervals.dimensionsAsLongArray(out));
		assertPermutedValues(img, out, AxisUtils.findPermutationByName(axes, XYCZT));
	}

	@Test
	public void testPermuteImage3DToXYCZT() {

		final Axis[] axes = axes("z", "y", "x");
		final ArrayImg<IntType, IntArray> img = ArrayImgs.ints(4, 5, 6);
		fillWithPositions(img);

		final RandomAccessibleInterval<IntType> out = AxisUtils.permute(img, axes, XYCZT);
		assertArrayEquals(new long[]{6, 5, 1, 4, 1}, Intervals.dimensionsAsLongArray(out));

		final int[] p = AxisUtils.findPermutationByName(axes, XYCZT);
		AxisUtils.fillPermutation(p);
		RandomAccessibleInterval<IntType> img5d = img;
		while (img5d.numDimensions() < 5)
			img5d = Views.addDimension(img5d, 0, 0);

		assertPermutedValues(img5d, out, p);
	}

	private static Axis[] axes(final String... labels) {

		return Arrays.stream(labels).map(Axis::new).toArray(Axis[]::new);
	}

	/**
	 * Sets each pixel to a unique value derived from its position.
	 */
	private static void fillWithPositions(final ArrayImg<IntType, IntArray> img) {

		int i = 0;
		for (final IntType t : img)
			t.set(i++);
	}

	/**
	 * Checks that out(pos) == src(q) where q[p[i]] = pos[i], for every position in out.
	 */
	private static void assertPermutedValues(
			final RandomAccessibleInterval<IntType> src,
			final RandomAccessibleInterval<IntType> out,
			final int[] p) {

		final RandomAccess<IntType> srcAccess = src.randomAccess();
		final long[] q = new long[src.numDimensions()];
		final Cursor<IntType> c = Views.flatIterable(out).localizingCursor();
		while (c.hasNext()) {
			c.fwd();
			for (int i = 0; i < p.length; i++)
				q[p[i]] = c.getLongPosition(i);

			srcAccess.setPosition(q);
			assertEquals(srcAccess.get().get(), c.get().get());
		}
	}

}

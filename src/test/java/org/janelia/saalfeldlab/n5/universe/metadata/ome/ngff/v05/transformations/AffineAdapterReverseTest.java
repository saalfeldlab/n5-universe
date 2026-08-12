package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import static org.junit.Assert.assertArrayEquals;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * Probes {@link AffineCoordinateTransformAdapter}. Two findings:
 * <ol>
 *   <li><b>Deserialization recurses infinitely</b> when the adapter is registered as a
 *       type-hierarchy adapter for {@link AbstractAffineCoordinateTransform} — the exact
 *       registration used by {@code BigWarpInit.gsonBuilder} and
 *       {@code NgffTransformations.gsonBuilder}. Its {@code deserialize} calls
 *       {@code context.deserialize(jobj, GeneralAffineCoordinateTransform.class)} (line 43),
 *       and since {@code GeneralAffineCoordinateTransform} is a subtype of the hierarchy-registered
 *       {@code AbstractAffineCoordinateTransform}, that re-enters the same adapter →
 *       {@link StackOverflowError}. Reading any NGFF affine crashes.</li>
 *   <li>The adapter reverses the matrix to C-order <b>unconditionally</b> (no reverse flag), so the
 *       on-disk matrix is identical for n5 and zarr. The reversal is otherwise self-consistent:
 *       when deserialization is done without the recursion, the matrix round-trips exactly.</li>
 * </ol>
 */
public class AffineAdapterReverseTest {

	// 3D homogeneous affine (3 rows x 4 cols), deliberately non-symmetric.
	private static final double[][] A = {
			{ 2.0, 0.1, 0.2, 100.0 },
			{ 0.3, 3.0, 0.4, 200.0 },
			{ 0.5, 0.6, 4.0, 300.0 } };

	// A reversed to C-order (what zarr stores): rows reversed, linear columns reversed, translation follows rows.
	private static final double[][] A_CORDER = {
			{ 4.0, 0.6, 0.5, 300.0 },
			{ 0.4, 3.0, 0.3, 200.0 },
			{ 0.2, 0.1, 2.0, 100.0 } };

	private static double[][] copy(final double[][] m) {
		final double[][] out = new double[m.length][];
		for (int i = 0; i < m.length; i++)
			out[i] = m[i].clone();
		return out;
	}

	private static InvertibleAffineCoordinateTransform sample() {
		return new InvertibleAffineCoordinateTransform("aff", "in", "out", copy(A));
	}

	/** As registered by BigWarpInit.gsonBuilder(reverse): affine as a reverse-aware type-hierarchy adapter. */
	private static Gson gsonHierarchy(final boolean reverse) {
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeHierarchyAdapter(AbstractAffineCoordinateTransform.class, new AffineCoordinateTransformAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.create();
	}

	private static double[][] matrix(final JsonElement json) {
		final com.google.gson.JsonArray rows = json.getAsJsonObject().getAsJsonArray(AbstractAffineCoordinateTransform.TYPE);
		final double[][] out = new double[rows.size()][];
		for (int i = 0; i < rows.size(); i++) {
			final com.google.gson.JsonArray row = rows.get(i).getAsJsonArray();
			out[i] = new double[row.size()];
			for (int j = 0; j < row.size(); j++)
				out[i][j] = row.get(j).getAsDouble();
		}
		return out;
	}

	/**
	 * Regression: deserializing an affine with the hierarchy-adapter gson (as BigWarpInit /
	 * NgffTransformations register it) used to recurse into {@link StackOverflowError}. It must now
	 * round-trip. See {@link AffineCoordinateTransformAdapter} — the base fields are read with a
	 * plain gson instead of re-entering the context.
	 */
	@Test
	public void deserializeWithHierarchyAdapterDoesNotRecurse() {
		final Gson g = gsonHierarchy(true);
		final JsonElement json = g.toJsonTree(sample(), CoordinateTransform.class);
		System.out.println("### serialized affine = " + json);

		final CoordinateTransform<?> back = g.fromJson(json, CoordinateTransform.class);
		final AbstractAffineCoordinateTransform aff = (AbstractAffineCoordinateTransform) back;
		System.out.println("### deserialized with hierarchy adapter: " + aff.getClass().getSimpleName()
				+ " name=" + aff.getName() + " input=" + aff.getInput().getName() + " output=" + aff.getOutput().getName());

		for (int i = 0; i < A.length; i++)
			assertArrayEquals("hierarchy-gson round-trip row " + i, A[i], aff.affine[i], 1e-12);
	}

	/** The on-disk matrix now depends on the reverse flag: F-order for n5, C-order for zarr. */
	@Test
	public void onDiskMatchesReverseFlag() {
		final double[][] n5Disk = matrix(gsonHierarchy(false).toJsonTree(sample(), CoordinateTransform.class));
		final double[][] zarrDisk = matrix(gsonHierarchy(true).toJsonTree(sample(), CoordinateTransform.class));
		System.out.println("### n5  (reverse=false) on-disk row0 = " + java.util.Arrays.toString(n5Disk[0]));
		System.out.println("### zarr(reverse=true ) on-disk row0 = " + java.util.Arrays.toString(zarrDisk[0]));

		for (int i = 0; i < A.length; i++) {
			assertArrayEquals("n5 stores imglib2 (F-order) row " + i, A[i], n5Disk[i], 1e-12);
			assertArrayEquals("zarr stores C-order row " + i, A_CORDER[i], zarrDisk[i], 1e-12);
		}
	}

	/**
	 * The point of the fix: within one container the affine matrix uses the <b>same</b> axis
	 * convention as a sibling scale transform — both F-order for n5, both C-order for zarr.
	 */
	@Test
	public void affineAndSiblingScaleAgree() {
		final ScaleCoordinateTransform scale = new ScaleCoordinateTransform("s", "in", "mid", new double[] { 1, 2, 3 });
		final SequenceCoordinateTransform seq = new SequenceCoordinateTransform("seq", "in", "out",
				new CoordinateTransform[] { scale, sample() });

		for (final boolean reverse : new boolean[] { false, true }) {
			final com.google.gson.JsonArray xfms = gsonHierarchy(reverse)
					.toJsonTree(seq, CoordinateTransform.class).getAsJsonObject().getAsJsonArray("transformations");
			final com.google.gson.JsonArray scaleArr = xfms.get(0).getAsJsonObject().getAsJsonArray("scale");
			final double[] scaleDisk = { scaleArr.get(0).getAsDouble(), scaleArr.get(1).getAsDouble(), scaleArr.get(2).getAsDouble() };
			final double[][] affDisk = matrix(xfms.get(1));

			if (reverse) { // zarr: both C-order
				assertArrayEquals("zarr scale C-order", new double[] { 3, 2, 1 }, scaleDisk, 1e-12);
				for (int i = 0; i < A.length; i++)
					assertArrayEquals("zarr affine C-order row " + i, A_CORDER[i], affDisk[i], 1e-12);
			} else { // n5: both F-order (imglib2)
				assertArrayEquals("n5 scale F-order", new double[] { 1, 2, 3 }, scaleDisk, 1e-12);
				for (int i = 0; i < A.length; i++)
					assertArrayEquals("n5 affine F-order row " + i, A[i], affDisk[i], 1e-12);
			}
		}
	}

	/** Self round-trip through the production-style gson, for both conventions. */
	@Test
	public void selfRoundTripBothConventions() {
		for (final boolean reverse : new boolean[] { false, true }) {
			final Gson g = gsonHierarchy(reverse);
			final JsonElement json = g.toJsonTree(sample(), CoordinateTransform.class);
			final CoordinateTransform<?> back = g.fromJson(json, CoordinateTransform.class);
			final double[][] rec = ((AbstractAffineCoordinateTransform) back).affine;
			System.out.println("### reverse=" + reverse + " recovered row0 = " + java.util.Arrays.toString(rec[0]));
			for (int i = 0; i < A.length; i++)
				assertArrayEquals("reverse=" + reverse + " row " + i, A[i], rec[i], 1e-12);
		}
	}
}

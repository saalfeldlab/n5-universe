package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffSceneParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffSceneMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.graph.TransformPath;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.RealTransform;

/**
 * Checks that affine coordinate transforms whose matrix is stored in an array (referenced by
 * {@code "path"}) instead of inline in the json can be read and built, for each storage format.
 * <p>
 * Convention (see {@link AffineAdapterReverseTest}): zarr stores coordinates in C-order and
 * reverses them on read ({@code reverse=true}), n5 stores imglib2 (F-order) coordinates
 * ({@code reverse=false}). A path-referenced affine must produce the same transform as the
 * equivalent inline affine.
 */
@RunWith(Parameterized.class)
public class AffineAdapterPathTest {

	// imglib2 (F-order) 3x4 affine, rows = target dims, cols = source dims + translation
	private static final double[][] A = {
			{ 2.0, 0.1, 0.2, 100.0 },
			{ 0.3, 3.0, 0.4, 200.0 },
			{ 0.5, 0.6, 4.0, 300.0 } };

	// A with coordinates reversed, i.e. how a zarr stores it
	private static final double[][] A_CORDER = {
			{ 4.0, 0.6, 0.5, 300.0 },
			{ 0.4, 3.0, 0.3, 200.0 },
			{ 0.2, 0.1, 2.0, 100.0 } };

	// non-square: 3D source -> 2D target
	private static final double[][] G = {
			{ 2.0, 0.1, 0.2, 100.0 },
			{ 0.3, 3.0, 0.4, 200.0 } };

	// 2D square affine, acts on two of the three dimensions in the byDimension tests
	private static final double[][] B = {
			{ 2.0, 0.1, 10.0 },
			{ 0.3, 3.0, 20.0 } };

	private static final double[] P = { 1.5, -2.0, 7.0 };

	private static final double EPS = 1e-9;

	private static InvertibleAffineCoordinateTransform invertible(final String name, final String input,
			final String output, final double[][] matrix) {

		return InvertibleAffineCoordinateTransform.options()
				.name(name).input(input).output(output).affine(matrix).build();
	}

	private static GeneralAffineCoordinateTransform general(final String name, final String input,
			final String output, final double[][] matrix) {

		return GeneralAffineCoordinateTransform.options()
				.name(name).input(input).output(output).affine(matrix).build();
	}

	/** an affine that reads its matrix from {@code path}, and has already read it */
	private static InvertibleAffineCoordinateTransform invertibleFrom(final String name, final N5Reader n5,
			final String path, final String input, final String output) {

		final InvertibleAffineCoordinateTransform ct = InvertibleAffineCoordinateTransform.options()
				.name(name).input(input).output(output).path(path).build();
		ct.bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
		ct.buildTransform(ct.getParameters());
		return ct;
	}

	/** an affine that reads its matrix from {@code path}, and has already read it */
	private static GeneralAffineCoordinateTransform generalFrom(final String name, final N5Reader n5,
			final String path, final String input, final String output) {

		final GeneralAffineCoordinateTransform ct = GeneralAffineCoordinateTransform.options()
				.name(name).input(input).output(output).path(path).build();
		ct.bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
		ct.buildTransform(ct.getParameters());
		return ct;
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> formats() {
		return Arrays.asList(new Object[][] {
				{ StorageFormat.ZARR2 },
				{ StorageFormat.ZARR3 },
				{ StorageFormat.N5 } });
	}

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private final StorageFormat format;

	private final boolean reverse;

	private N5Writer n5;

	private String root;

	public AffineAdapterPathTest(final StorageFormat format) {
		this.format = format;
		this.reverse = format != StorageFormat.N5;
	}

	@Before
	public void before() throws IOException {
		final String ext = reverse ? "test.zarr" : "test.n5";
		root = tmp.newFolder().toPath().resolve(ext).toString();
		n5 = new N5Factory()
				.options(opts -> { opts.gsonBuilder(gsonBuilder(reverse)); })
				.openWriter(format, root);
	}

	@After
	public void after() {
		if (n5 != null)
			n5.close();
	}

	private static GsonBuilder gsonBuilder(final boolean reverse) {
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter());
	}

	private Gson gson() {
		return gsonBuilder(reverse).create();
	}

	private static double[] apply(final double[][] mtx, final double[] p) {
		final int nc = mtx[0].length - 1;
		final double[] out = new double[mtx.length];
		for (int i = 0; i < mtx.length; i++) {
			out[i] = mtx[i][nc];
			for (int j = 0; j < nc; j++)
				out[i] += mtx[i][j] * p[j];
		}
		return out;
	}

	private static double[] apply(final RealTransform t, final double[] p) {
		final double[] out = new double[t.numTargetDimensions()];
		t.apply(p, out);
		return out;
	}

	private static void assertMatrixEquals(final String msg, final double[][] expected, final double[][] actual) {
		assertNotNull(msg + ": matrix is null", actual);
		assertEquals(msg + ": number of rows", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++)
			assertArrayEquals(msg + ": row " + i, expected[i], actual[i], EPS);
	}

	// writes the json as an attribute at the root, then reads it with CoordinateTransforms, which binds it
	private CoordinateTransform<?> readTransform(final String json) {

		n5.setAttribute("", "ct", JsonParser.parseString(json));
		return CoordinateTransforms.read(n5, "", "ct");
	}

	private static String pathAffineJson(final String path) {
		return "{\"type\":\"affine\",\"name\":\"aff\",\"input\":{\"name\":\"in\"},\"output\":{\"name\":\"out\"},"
				+ "\"path\":\"" + path + "\"}";
	}

	/**
	 * {@link AbstractAffineCoordinateTransform#write} followed by
	 * {@link AbstractAffineCoordinateTransform#getParameters} returns the original matrix.
	 */
	@Test
	public void writeThenGetParameters() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final InvertibleAffineCoordinateTransform ct = invertibleFrom("aff", n5,"affine", "in", "out");
		assertMatrixEquals("square affine parameters", A, ct.getParameters());
	}

	/**
	 * {@link AbstractAffineCoordinateTransform#write} returns an affine of the same type that
	 * references the written array by its path, serializes as that path, and reads it back.
	 */
	@Test
	public void writeReturnsPathTransform() {

		final AbstractAffineCoordinateTransform<?> written = invertible("aff", "in", "out",A).write(n5, "affine");
		assertTrue("same type", written instanceof InvertibleAffineCoordinateTransform);
		assertEquals("affine", written.getParameterPath());
		assertArrayEquals(apply(A, P), apply(written.getTransform(), P), EPS);
		assertMatrixEquals("parameters read from the array", A, written.getParameters());

		final JsonObject json = gson().toJsonTree(written, CoordinateTransform.class).getAsJsonObject();
		assertEquals("serialized path", "affine", json.get("path").getAsString());
		assertTrue("matrix not serialized", !json.has(AbstractAffineCoordinateTransform.TYPE));
	}

	@Test
	public void nonSquareWriteThenGetParameters() {

		general("aff", "in", "out", G).write(n5, "affine");

		final GeneralAffineCoordinateTransform ct = generalFrom("aff", n5,"affine", "in", "out");
		assertMatrixEquals("non-square affine parameters", G, ct.getParameters());
	}

	/**
	 * The on-disk shape of a written matrix is [rows, cols] for every format: zarr's "shape" and
	 * n5's "dimensions" are both [2, 4] for the 2x4 matrix G.
	 */
	@Test
	public void writtenShapeIsRowsCols() throws IOException {

		general("aff", "in", "out", G).write(n5, "affine");

		final String file;
		final String key;
		switch (format) {
		case ZARR2:
			file = ".zarray";
			key = "shape";
			break;
		case ZARR3:
			file = "zarr.json";
			key = "shape";
			break;
		case N5:
			file = "attributes.json";
			key = "dimensions";
			break;
		default:
			throw new AssertionError("unexpected format " + format);
		}

		final JsonObject meta = JsonParser.parseString(
				new String(Files.readAllBytes(Paths.get(root, "affine", file)), StandardCharsets.UTF_8))
				.getAsJsonObject();
		final long[] shape = new Gson().fromJson(meta.get(key), long[].class);
		assertArrayEquals("on-disk " + key + " of " + file, new long[] { G.length, G[0].length }, shape);
	}

	/**
	 * Independent of {@link AbstractAffineCoordinateTransform#write}: a zarr array holding the matrix
	 * exactly as it would appear inline in the json (C-order, zarr shape [rows, cols]) is read back
	 * as the imglib2 matrix.
	 */
	@Test
	public void zarrArrayMatchesInlineConvention() {

		Assume.assumeTrue("zarr only", reverse);

		// imglib2 dims (cols, rows) == zarr shape [rows, cols]
		N5Utils.save(ArrayImgs.doubles(TransformUtils.flatten(A_CORDER), 4, 3),
				n5, "affine", new int[] { 4, 3 }, new RawCompression());

		final InvertibleAffineCoordinateTransform ct = invertibleFrom("aff", n5,"affine", "in", "out");
		assertMatrixEquals("affine parameters from a C-order zarr array", A, ct.getParameters());
	}

	@Test
	public void constructorWithReaderBuildsTransform() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final InvertibleAffineCoordinateTransform ct = invertibleFrom("aff", n5,"affine", "in", "out");
		assertNotNull("transform built by constructor", ct.getTransform());
		assertArrayEquals(apply(A, P), apply(ct.getTransform(), P), EPS);
	}

	/**
	 * An affine with only a {@code path}, read with {@link CoordinateTransforms}, is bound, so
	 * {@link CoordinateTransform#getTransform()} loads the parameters and gives the same transform
	 * as the inline affine.
	 */
	@Test
	public void deserializeFromPathThenGetTransform() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(pathAffineJson("affine"));
		assertNotNull("deserialized", ct);
		assertTrue("is an affine", ct instanceof AbstractAffineCoordinateTransform);
		assertTrue("is bound", ((AbstractParametrizedTransform<?, ?>)ct).isBound());
		assertEquals("affine", ((ParametrizedTransform<?, ?>)ct).getParameterPath());
		assertEquals("in", ct.getInput().getName());
		assertEquals("out", ct.getOutput().getName());

		final RealTransform t = ct.getTransform();
		assertNotNull("transform built from path", t);
		assertArrayEquals(apply(A, P), apply(t, P), EPS);
	}

	/**
	 * An affine with only a {@code path}, deserialized without a reader, is not bound, so
	 * {@link CoordinateTransform#getTransform()} throws rather than returning null.
	 */
	@Test(expected = N5Exception.class)
	public void unboundPathAffineThrows() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final CoordinateTransform<?> ct = gson().fromJson(pathAffineJson("affine"), CoordinateTransform.class);
		ct.getTransform();
	}

	/**
	 * The deprecated {@link CoordinateTransform#getTransform(org.janelia.saalfeldlab.n5.N5Reader)}
	 * still binds an unbound transform to the reader it is given.
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedGetTransformWithReaderBinds() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final CoordinateTransform<?> ct = gson().fromJson(pathAffineJson("affine"), CoordinateTransform.class);
		assertArrayEquals(apply(A, P), apply(ct.getTransform(n5), P), EPS);
	}

	@Test
	public void deserializeNonSquareFromPathThenGetTransform() {

		general("aff", "in", "out", G).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(pathAffineJson("affine"));
		assertNotNull("deserialized", ct);

		final RealTransform t = ct.getTransform();
		assertNotNull("transform built from path", t);
		assertEquals("source dimensions", 3, t.numSourceDimensions());
		assertEquals("target dimensions", 2, t.numTargetDimensions());
		assertArrayEquals(apply(G, P), apply(t, P), EPS);
	}

	/**
	 * An inline square affine deserializes as invertible; one read from a path should be
	 * invertible too: the graph can go from its output back to its input.
	 */
	@Test
	public void deserializedSquarePathAffineIsInvertible() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		// the graph builds inverse edges when it is built, which reads the matrix, so it must be bound
		final CoordinateTransform<?> ct = readTransform(pathAffineJson("affine"));
		assertNotNull("deserialized", ct);

		final TransformGraph graph = new TransformGraph(
				Arrays.<CoordinateTransform<?>>asList(ct),
				new CoordinateSystems(Arrays.asList(new CoordinateSystem("in", 3), new CoordinateSystem("out", 3))));

		final Optional<TransformPath> inverse = graph.path("out", "in");
		assertTrue("path out -> in", inverse.isPresent());
		assertArrayEquals(P, apply(inverse.get().totalTransform(graph), apply(A, P)), EPS);
	}

	/**
	 * A sequence holding a square path affine is invertible.
	 */
	@Test
	public void pathAffineInSequenceIsInvertible() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(sequenceJson("affine"));

		final TransformGraph graph = new TransformGraph(
				Arrays.<CoordinateTransform<?>>asList(ct),
				new CoordinateSystems(Arrays.asList(new CoordinateSystem("a", 3), new CoordinateSystem("b", 3))));

		final Optional<TransformPath> inverse = graph.path("b", "a");
		assertTrue("path b -> a", inverse.isPresent());
		assertArrayEquals(P, apply(inverse.get().totalTransform(graph), apply(A, scaledP())), EPS);
	}

	/**
	 * A path that does not exist gives null parameters, not an exception.
	 */
	@Test
	public void missingParametersReturnNull() {

		final GeneralAffineCoordinateTransform ct = generalFrom("aff", n5,"doesNotExist", "in", "out");
		assertNull(ct.getParameters());
	}

	/**
	 * A path affine nested in a sequence is bound when the sequence is read, and built by
	 * {@link SequenceCoordinateTransform#getTransform()}.
	 */
	@Test
	public void pathAffineInSequence() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(sequenceJson("affine"));
		assertTrue("is a sequence", ct instanceof SequenceCoordinateTransform);

		final RealTransform t = ct.getTransform();
		assertNotNull(t);
		assertArrayEquals(apply(A, scaledP()), apply(t, P), EPS);
	}

	/**
	 * A path affine nested in a sequence is built by {@link TransformPath#totalTransform(TransformGraph)}.
	 */
	@Test
	public void pathAffineInSequenceThroughGraph() {

		invertible("aff", "in", "out",A).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(sequenceJson("affine"));
		final TransformGraph graph = new TransformGraph(
				Arrays.<CoordinateTransform<?>>asList(ct),
				new CoordinateSystems(Arrays.asList(new CoordinateSystem("a", 3), new CoordinateSystem("b", 3))));

		final Optional<TransformPath> path = graph.path("a", "b");
		assertTrue("path a -> b", path.isPresent());

		final RealTransform t = path.get().totalTransform(graph);
		assertArrayEquals(apply(A, scaledP()), apply(t, P), EPS);
	}

	/**
	 * A relative parameter {@code path} in a scene resolves against the group holding the scene
	 * metadata, not the container root.
	 */
	@Test
	public void scenePathResolvesRelativeToGroup() {

		writeSceneWithPathAffine();

		// read with the reader's own gson, so the scene's transforms are bound by getGraph
		final NgffScene scene = n5.getAttribute("scene", NgffScene.SCENE_KEY, NgffScene.class);
		assertNotNull("scene parsed", scene);

		final TransformGraph graph = scene.getGraph(n5, "scene");
		final Optional<TransformPath> path = graph.path("a", "b");
		assertTrue("path a -> b", path.isPresent());

		final RealTransform t = path.get().totalTransform(graph);
		assertArrayEquals(apply(A, P), apply(t, P), EPS);
	}

	/**
	 * A scene parsed by {@link OmeNgffSceneParser} has its transforms bound, with relative paths
	 * resolved against the scene's group, without building the graph.
	 */
	@Test
	public void sceneParserBindsTransforms() {

		writeSceneWithPathAffine();

		final Optional<NgffSceneMetadata> meta = new OmeNgffSceneParser(n5).parseMetadata(n5, new N5TreeNode("scene"));
		assertTrue("scene parsed", meta.isPresent());

		final CoordinateTransform<?> ct = meta.get().getScene().getCoordinateTransformations()[0];
		assertEquals("scene/params/affine", ((ParametrizedTransform<?, ?>)ct).getParameterPath());
		assertArrayEquals(apply(A, P), apply(ct.getTransform(), P), EPS);
	}

	// a scene at group "scene" with an affine a -> b whose matrix is at "params/affine", relative to the scene
	private void writeSceneWithPathAffine() {

		invertible("aff", "in", "out",A).write(n5, "scene/params/affine");

		final String axes = "[{\"name\":\"x\",\"type\":\"space\"},{\"name\":\"y\",\"type\":\"space\"},{\"name\":\"z\",\"type\":\"space\"}]";
		final JsonObject ome = JsonParser.parseString("{\"version\":\"0.6\",\"scene\":{"
				+ "\"coordinateSystems\":["
				+ "{\"name\":\"a\",\"axes\":" + axes + "},"
				+ "{\"name\":\"b\",\"axes\":" + axes + "}],"
				+ "\"coordinateTransformations\":[{\"type\":\"affine\",\"name\":\"a_to_b\","
				+ "\"input\":{\"name\":\"a\"},\"output\":{\"name\":\"b\"},\"path\":\"params/affine\"}]"
				+ "}}").getAsJsonObject();
		n5.setAttribute("scene", "ome", ome);
	}

	/**
	 * A path affine nested in a byDimension is bound when the byDimension is read, and built by
	 * {@link ByDimensionCoordinateTransform#getTransform()}.
	 */
	@Test
	public void pathAffineInByDimension() {

		invertible("aff", "in", "out",B).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(byDimensionJson("affine"));
		assertTrue("is a byDimension", ct instanceof ByDimensionCoordinateTransform);

		final RealTransform t = ct.getTransform();
		assertNotNull(t);
		assertArrayEquals(byDimensionExpected(), apply(t, P), EPS);
	}

	/**
	 * A path affine nested in a byDimension is built by {@link TransformPath#totalTransform(TransformGraph)}.
	 */
	@Test
	public void pathAffineInByDimensionThroughGraph() {

		invertible("aff", "in", "out",B).write(n5, "affine");

		final CoordinateTransform<?> ct = readTransform(byDimensionJson("affine"));
		final TransformGraph graph = new TransformGraph(
				Arrays.<CoordinateTransform<?>>asList(ct),
				new CoordinateSystems(Arrays.asList(new CoordinateSystem("a", 3), new CoordinateSystem("b", 3))));

		final Optional<TransformPath> path = graph.path("a", "b");
		assertTrue("path a -> b", path.isPresent());

		final RealTransform t = path.get().totalTransform(graph);
		assertArrayEquals(byDimensionExpected(), apply(t, P), EPS);
	}

	/**
	 * A byDimension made with constructors, holding a path affine without a reader, cannot be built
	 * until it is bound.
	 */
	@Test
	public void constructedPathAffineInByDimension() {

		invertible("aff", "in", "out",B).write(n5, "affine");

		final GeneralAffineCoordinateTransform affine = GeneralAffineCoordinateTransform.options()
				.name("aff").input("in").output("out").path("affine").build();
		final ByDimensionCoordinateTransform ct = constructedByDimension(affine);

		try {
			ct.getTransform();
			fail("an unbound path affine should not build");
		} catch (final N5Exception e) {}

		ParameterBinding.bind(ct, n5, "");
		final RealTransform t = ct.getTransform();
		assertNotNull(t);
		assertArrayEquals(byDimensionExpected(), apply(t, P), EPS);
	}

	/**
	 * As {@link #constructedPathAffineInByDimension()}, with the affine bound to the reader by its
	 * constructor.
	 */
	@Test
	public void constructedReaderAffineInByDimension() {

		invertible("aff", "in", "out",B).write(n5, "affine");

		final GeneralAffineCoordinateTransform affine = generalFrom("aff", n5,"affine", "in", "out");
		final ByDimensionCoordinateTransform ct = constructedByDimension(affine);

		final RealTransform t = ct.getTransform();
		assertNotNull(t);
		assertArrayEquals(byDimensionExpected(), apply(t, P), EPS);
	}

	/**
	 * A byDimension made with constructors, holding the path affine returned by
	 * {@link AbstractAffineCoordinateTransform#write}, built through the graph.
	 */
	@Test
	public void writtenAffineInByDimensionThroughGraph() {

		final AbstractAffineCoordinateTransform<?> affine = invertible("aff", "in", "out",B).write(n5, "affine");
		final ByDimensionCoordinateTransform ct = constructedByDimension(affine);

		final TransformGraph graph = new TransformGraph(
				Arrays.<CoordinateTransform<?>>asList(ct),
				new CoordinateSystems(Arrays.asList(new CoordinateSystem("a", 3), new CoordinateSystem("b", 3))));

		final Optional<TransformPath> path = graph.path("a", "b");
		assertTrue("path a -> b", path.isPresent());

		final RealTransform t = path.get().totalTransform(graph);
		assertArrayEquals(byDimensionExpected(), apply(t, P), EPS);
	}

	// axes are given directly in imglib2 order (nothing is reversed outside of deserialization):
	// scale on dim 2, the affine on dims 0 and 1
	private static ByDimensionCoordinateTransform constructedByDimension(final CoordinateTransform<?> affine) {

		final ScaleCoordinateTransform scale = new ScaleCoordinateTransform(new double[] { 2.0 });
		return ByDimensionCoordinateTransform.options()
				.name("bd").input("a").output("b")
				.transformations(
						new SubAxisTransformation(scale, new int[] { 2 }, new int[] { 2 }),
						new SubAxisTransformation(affine, new int[] { 0, 1 }, new int[] { 0, 1 }))
				.build();
	}

	// byDimension: scale by 2 on one axis, the path affine B on the other two. Axis indices in the
	// json are in the storage order of the format, so for zarr (reversed) they name the same
	// imglib2 dimensions as the n5 indices: scale on imglib2 dim 2, affine on dims 0 and 1.
	private String byDimensionJson(final String affinePath) {

		final String scaleAxes = reverse ? "[0]" : "[2]";
		final String affineAxes = reverse ? "[1,2]" : "[0,1]";
		return "{\"type\":\"byDimension\",\"name\":\"bd\",\"input\":{\"name\":\"a\"},\"output\":{\"name\":\"b\"},"
				+ "\"transformations\":["
				+ "{\"transformation\":{\"type\":\"scale\",\"scale\":[2.0]},\"inputAxes\":" + scaleAxes + ",\"outputAxes\":" + scaleAxes + "},"
				+ "{\"transformation\":{\"type\":\"affine\",\"path\":\"" + affinePath + "\"},\"inputAxes\":" + affineAxes + ",\"outputAxes\":" + affineAxes + "}]}";
	}

	private static double[] byDimensionExpected() {

		final double[] xy = apply(B, new double[] { P[0], P[1] });
		return new double[] { xy[0], xy[1], 2 * P[2] };
	}

	// sequence: uniform scale by 2 (unaffected by axis reversal), then the path affine
	private static String sequenceJson(final String affinePath) {
		return "{\"type\":\"sequence\",\"name\":\"seq\",\"input\":{\"name\":\"a\"},\"output\":{\"name\":\"b\"},"
				+ "\"transformations\":["
				+ "{\"type\":\"scale\",\"scale\":[2.0,2.0,2.0]},"
				+ "{\"type\":\"affine\",\"path\":\"" + affinePath + "\"}]}";
	}

	private static double[] scaledP() {
		return new double[] { 2 * P[0], 2 * P[1], 2 * P[2] };
	}

}

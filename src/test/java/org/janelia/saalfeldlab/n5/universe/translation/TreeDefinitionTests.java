package org.janelia.saalfeldlab.n5.universe.translation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.universe.factory.N5Factory;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Tests for the active definitions in {@code /tree.jq}, for every writable
 * dialect.
 * <p>
 * Each test runs a translation over a real container through {@link
 * TranslatedN5Reader}, and reads the result back through the delegate's
 * dialect. So it checks not only what a definition computes, but also that it
 * writes where the dialect reads (the user attributes, not a stray attribute
 * file next to them).
 */
@RunWith(Parameterized.class)
public class TreeDefinitionTests {

	private static final class Format {

		final String name;
		final String containerName;
		final Function<String, N5Writer> open;

		Format(final String name, final String containerName, final Function<String, N5Writer> open) {

			this.name = name;
			this.containerName = containerName;
			this.open = open;
		}

		@Override
		public String toString() {

			return name;
		}
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> formats() {

		return Arrays.asList(new Object[][]{
				{new Format("n5", "tree.n5", N5FSWriter::new)},
				{new Format("zarr2", "tree.zarr", path -> new N5Factory().openWriter(StorageFormat.ZARR2, path))},
				{new Format("zarr3", "tree.zarr", path -> new N5Factory().openWriter(StorageFormat.ZARR3, path))}
		});
	}

	private static final String TFORM_S0 = "{\"transform\":{\"type\":\"scale\",\"scale\":[1,1,1]}}";
	private static final String TFORM_S1 = "{\"transform\":{\"type\":\"scale\",\"scale\":[2,2,2]}}";

	private final Format format;

	private Path tempDir;
	private N5Writer n5;

	public TreeDefinitionTests(final Format format) {

		this.format = format;
	}

	@Before
	public void before() throws IOException {

		tempDir = Files.createTempDirectory("n5-tree-definition-test-");
		n5 = format.open.apply(tempDir.resolve(format.containerName).toString());
		createHierarchy(n5);
	}

	@After
	public void after() throws IOException {

		if (n5 != null)
			n5.close();
		if (tempDir != null && Files.exists(tempDir)) {
			try (Stream<Path> paths = Files.walk(tempDir)) {
				paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		}
	}

	/**
	 * <pre>
	 * /              group
	 * /ds            dataset [4,3,2], user attribute "a": 1
	 * /ms            group, a multiscale: s0 and s1 have a spatialTransform, s2 does not
	 * /ms/s0..s2     datasets
	 * /mc            group, a multichannel (children named c0, c1)
	 * /mc/c0, c1     datasets
	 * </pre>
	 */
	private static void createHierarchy(final N5Writer n5) {

		n5.createGroup("");
		createDataset(n5, "ds");
		n5.setAttribute("ds", "a", 1);

		n5.createGroup("ms");
		createDataset(n5, "ms/s0");
		createDataset(n5, "ms/s1");
		createDataset(n5, "ms/s2");
		n5.setAttribute("ms/s0", "spatialTransform", json(TFORM_S0));
		n5.setAttribute("ms/s1", "spatialTransform", json(TFORM_S1));

		n5.createGroup("mc");
		createDataset(n5, "mc/c0");
		createDataset(n5, "mc/c1");
	}

	private static void createDataset(final N5Writer n5, final String path) {

		n5.createDataset(path, new long[]{4, 3, 2}, new int[]{4, 3, 2}, DataType.UINT8, new RawCompression());
	}

	private static JsonElement json(final String json) {

		return JsonParser.parseString(json);
	}

	private TranslatedN5Reader translate(final String translation) {

		return new TranslatedN5Reader(n5, "include \"n5\"; " + translation, ".");
	}

	/**
	 * Evaluate {@code expression} against the whole (path-annotated) tree,
	 * bound to {@code $t}, and return the result. It is written into the root's
	 * user attributes, and read back from there.
	 */
	private JsonElement eval(final String expression) {

		final TranslatedN5Reader translated = translate(
				"addPaths | . as $t | setAttrs(.result = ($t | " + expression + "))");
		return translated.getAttribute("", "result", JsonElement.class);
	}

	/**
	 * Assert that the node at {@code path} of the translated hierarchy has only
	 * attribute files that the dialect reads. A definition that wrote into the
	 * file map instead of the user attributes would add a file next to them
	 * (named after the attribute), which the dialect silently ignores.
	 */
	private void assertOnlyDialectAttributeFiles(final TranslatedN5Reader translated, final String path) {

		JsonObject node = translated.getTranslatedHierarchy().getAsJsonObject();
		for (final String name : path.split("/")) {
			if (!name.isEmpty())
				node = node.getAsJsonObject("children").getAsJsonObject(name);
		}
		final Set<String> dialectFiles = new HashSet<>(Arrays.asList(DialectInfo.of((GsonN5Reader)n5).attributeFileNames()));
		final Set<String> files = node.getAsJsonObject("attributes").keySet();
		assertTrue("unexpected attribute files " + files + " at " + path, dialectFiles.containsAll(files));
	}

	/**
	 * {@link #assertOnlyDialectAttributeFiles} must catch the mistake the old
	 * definitions make on the current tree shape: editing the file map
	 * ({@code .attributes}) as if it were the user attributes.
	 */
	@Test
	public void testStrayAttributeFileIsDetected() {

		final TranslatedN5Reader translated = translate("getSubTree(\"/ds\") |= (.attributes |= . + {\"k\": 1})");
		assertThrows(AssertionError.class, () -> assertOnlyDialectAttributeFiles(translated, "ds"));
	}

	@Test
	public void testIsDataset() {

		assertEquals(json("[true, false, false, false]"),
				eval("[ (getSubTree(\"/ds\"), getSubTree(\"/ms\"), getSubTree(\"/mc\"), .) | isDataset ]"));
		// walk() visits values that are not nodes
		assertEquals(json("[false, false, false, false]"),
				eval("[ 1, \"x\", null, [] ] | map(isDataset)"));
	}

	@Test
	public void testHasDims() {

		assertEquals(json("[true, false, false]"),
				eval("[ (getSubTree(\"/ds\"), getSubTree(\"/ms\"), .) | hasDims ]"));
	}

	/**
	 * Every node has an {@code attributes} map, so {@code hasAttributes} only
	 * tells nodes from other values.
	 */
	@Test
	public void testHasAttributes() {

		assertEquals(json("[true, true, true, false, false, false]"),
				eval("[ (., getSubTree(\"/ds\"), getSubTree(\"/ms\")) | hasAttributes ] + ([ 1, null, [] ] | map(hasAttributes))"));
	}

	@Test
	public void testNumDimensions() {

		// a group has no dimensions, and null | length is 0
		assertEquals(json("[3, 0]"),
				eval("[ (getSubTree(\"/ds\"), getSubTree(\"/ms\")) | numDimensions ]"));
	}

	@Test
	public void testFlattenTree() {

		assertEquals(json("[\"\", \"ds\", \"mc\", \"mc/c0\", \"mc/c1\", \"ms\", \"ms/s0\", \"ms/s1\", \"ms/s2\"]"),
				eval("[ flattenTree | .path ] | sort"));
		assertEquals(json("false"),
				eval("[ flattenTree | has(\"children\") ] | any"));
	}

	@Test
	public void testAttrHasTform() {

		assertEquals(json("[true, true, false, false]"),
				eval("[ (getSubTree(\"/ms/s0\"), getSubTree(\"/ms/s1\"), getSubTree(\"/ms/s2\"), getSubTree(\"/ds\")) | attrHasTform ]"));
	}

	@Test
	public void testIsIJ() {

		final String[] keys = {"pixelWidth", "pixelHeight", "pixelUnit", "xOrigin", "yOrigin"};
		for (final String key : keys) {
			final Object value = key.equals("pixelUnit") ? "um" : 1.0;
			n5.setAttribute("ds", key, value);
			// a group with the ImageJ calibration is not ImageJ metadata
			n5.setAttribute("ms", key, value);
			// a dataset that lacks one of the keys is not either
			if (!key.equals("yOrigin"))
				n5.setAttribute("mc/c0", key, value);
		}

		assertEquals(json("[true, false, false, false, false]"),
				eval("[ (getSubTree(\"/ds\"), getSubTree(\"/ms\"), getSubTree(\"/mc/c0\"), getSubTree(\"/mc/c1\"), .) | isIJ ]"));
		// walk() visits values that are not nodes
		assertEquals(json("[false, false, false, false]"),
				eval("[ 1, \"x\", null, [] ] | map(isIJ)"));
	}

	/**
	 * Set the ImageJ calibration of the 3D dataset {@code /ds}, with the third
	 * axis being C, Z or T depending on which of the counts is &gt; 1.
	 */
	private void setIJCalibration(final int numChannels, final int numSlices, final int numFrames) {

		n5.setAttribute("ds", "pixelWidth", 2.0);
		n5.setAttribute("ds", "pixelHeight", 3.0);
		n5.setAttribute("ds", "pixelDepth", 4.0);
		n5.setAttribute("ds", "frameInterval", 5.0);
		n5.setAttribute("ds", "pixelUnit", "um");
		n5.setAttribute("ds", "xOrigin", 0.5);
		n5.setAttribute("ds", "yOrigin", 0.25);
		n5.setAttribute("ds", "zOrigin", 7.0);
		n5.setAttribute("ds", "numChannels", numChannels);
		n5.setAttribute("ds", "numSlices", numSlices);
		n5.setAttribute("ds", "numFrames", numFrames);
	}

	@Test
	public void testIjAffineNd() {

		// XYZ: pixelDepth and zOrigin on the third row
		setIJCalibration(1, 2, 1);
		assertEquals(json("[2, 0, 0, 0.5,  0, 3, 0, 0.25,  0, 0, 4, 7]"),
				eval("getSubTree(\"/ds\") | ijAffineNd"));

		// XYC: the channel row stays identity
		setIJCalibration(2, 1, 1);
		assertEquals(json("[2, 0, 0, 0.5,  0, 3, 0, 0.25,  0, 0, 1, 0]"),
				eval("getSubTree(\"/ds\") | ijAffineNd"));

		// XYT: frameInterval as scale, no offset
		setIJCalibration(1, 1, 2);
		assertEquals(json("[2, 0, 0, 0.5,  0, 3, 0, 0.25,  0, 0, 5, 0]"),
				eval("getSubTree(\"/ds\") | ijAffineNd"));
	}

	@Test
	public void testIjToTransform() {

		setIJCalibration(1, 2, 1);
		final TranslatedN5Reader translated = translate("walk( if isIJ then ijToTransform else . end )");

		final JsonObject spatialTransform = translated.getAttribute("ds", "spatialTransform", JsonObject.class);
		assertEquals(json("{\"type\": \"affine\", \"affine\": [2, 0, 0, 0.5,  0, 3, 0, 0.25,  0, 0, 4, 7]}"),
				spatialTransform.get("transform"));
		assertEquals(json("[" +
						"{\"label\": \"x\", \"type\": \"space\", \"unit\": \"um\"}," +
						"{\"label\": \"y\", \"type\": \"space\", \"unit\": \"um\"}," +
						"{\"label\": \"z\", \"type\": \"space\", \"unit\": \"um\"}]"),
				translated.getAttribute("ds", "axes", JsonElement.class));
		// the calibration itself is kept
		assertEquals(2.0, translated.getAttribute("ds", "pixelWidth", Double.class), 0);
		assertArrayEquals(new long[]{4, 3, 2}, translated.getDatasetAttributes("ds").getDimensions());
		assertOnlyDialectAttributeFiles(translated, "ds");

		// not ImageJ metadata: untouched
		assertNull(translated.getAttribute("ms/s0", "axes", JsonElement.class));
		assertEquals(json(TFORM_S0), translated.getAttribute("ms/s0", "spatialTransform", JsonElement.class));
	}

	@Test
	public void testNumTformChildren() {

		assertEquals(json("[2, 0, 0]"),
				eval("[ (getSubTree(\"/ms\"), getSubTree(\"/mc\"), .) | numTformChildren ]"));
	}

	@Test
	public void testHasMultiscales() {

		assertEquals(json("[true, false, false]"),
				eval("[ (getSubTree(\"/ms\"), getSubTree(\"/mc\"), .) | hasMultiscales ]"));
	}

	@Test
	public void testBuildMultiscaleST() {

		assertEquals(json("[" +
						"{\"path\":\"s0\",\"spatialTransform\":" + TFORM_S0 + "}," +
						"{\"path\":\"s1\",\"spatialTransform\":" + TFORM_S1 + "}," +
						"{\"path\":\"s2\",\"spatialTransform\":null}]"),
				eval("getSubTree(\"/ms\") | buildMultiscaleST"));
	}

	@Test
	public void testBuildMultiChannel() {

		assertEquals(json("[{\"path\":\"c0\"},{\"path\":\"c1\"}]"),
				eval("getSubTree(\"/mc\") | buildMultiChannel"));
	}

	@Test
	public void testAddMultiChannel() {

		final TranslatedN5Reader translated = translate("addPaths | getSubTree(\"/mc\") |= addMultiChannel");

		assertEquals(json("{\"datasets\":[{\"path\":\"c0\"},{\"path\":\"c1\"}],\"path\":\"mc\"}"),
				translated.getAttribute("mc", "multichannel", JsonElement.class));
		assertOnlyDialectAttributeFiles(translated, "mc");
	}

	@Test
	public void testAddAllMultichannel() {

		final TranslatedN5Reader translated = translate("addPaths | addAllMultichannel");

		assertEquals(json("{\"datasets\":[{\"path\":\"c0\"},{\"path\":\"c1\"}],\"path\":\"mc\"}"),
				translated.getAttribute("mc", "multichannel", JsonElement.class));
		assertNull(translated.getAttribute("", "multichannel", JsonElement.class));
		assertNull(translated.getAttribute("ms", "multichannel", JsonElement.class));
	}

	@Test
	public void testTreeAddAttrs() {

		final TranslatedN5Reader translated = translate("treeAddAttrs(\"/ds\"; {\"k\": 1})");

		assertEquals(1, (int)translated.getAttribute("ds", "k", Integer.class));
		assertEquals(1, (int)translated.getAttribute("ds", "a", Integer.class));
		assertArrayEquals(new long[]{4, 3, 2}, translated.getDatasetAttributes("ds").getDimensions());
		// the attributes went into the user attributes, not into a file named "k"
		assertOnlyDialectAttributeFiles(translated, "ds");
	}

	@Test
	public void testTreeEditAttrs() {

		final TranslatedN5Reader translated = translate("treeEditAttrs(\"/ds\"; del(.a) | .b = 2)");

		assertNull(translated.getAttribute("ds", "a", Integer.class));
		assertEquals(2, (int)translated.getAttribute("ds", "b", Integer.class));
		assertArrayEquals(new long[]{4, 3, 2}, translated.getDatasetAttributes("ds").getDimensions());
		assertOnlyDialectAttributeFiles(translated, "ds");
	}

	private static final String NGFF_MULTISCALES = "[" +
			"{\"name\": \"a\", \"datasets\": [{\"path\": \"s0\"}, {\"path\": \"s1\"}]}," +
			"{\"name\": \"b\", \"datasets\": [{\"path\": \"s0\"}]}]";

	@Test
	public void testIsNgffMultiscale() {

		n5.setAttribute("ms", "multiscales", json(NGFF_MULTISCALES));
		// empty
		n5.setAttribute("mc", "multiscales", json("[]"));
		// not an array
		n5.setAttribute("ds", "multiscales", json("{\"datasets\": []}"));
		// first entry without datasets
		n5.setAttribute("ms/s0", "multiscales", json("[{\"name\": \"a\"}]"));

		assertEquals(json("[true, false, false, false, false]"),
				eval("[ (getSubTree(\"/ms\"), getSubTree(\"/mc\"), getSubTree(\"/ds\"), getSubTree(\"/ms/s0\"), .) | isNgffMultiscale ]"));
		// walk() visits values that are not nodes
		assertEquals(json("[false, false, false, false]"),
				eval("[ 1, \"x\", null, [] ] | map(isNgffMultiscale)"));
	}

	@Test
	public void testSelectMultiscale() {

		n5.setAttribute("ms", "multiscales", json(NGFF_MULTISCALES));
		n5.setAttribute("ms", "k", 1);

		final TranslatedN5Reader translated = translate(
				"getSubTree(\"/ms\") |= selectMultiscale(1) | getSubTree(\"/ds\") |= selectMultiscale(0)");

		// the array is replaced by its entry 1, an object
		assertEquals(json("{\"name\": \"b\", \"datasets\": [{\"path\": \"s0\"}]}"),
				translated.getAttribute("ms", "multiscales", JsonElement.class));
		assertEquals(1, (int)translated.getAttribute("ms", "k", Integer.class));
		assertOnlyDialectAttributeFiles(translated, "ms");

		// a node without multiscales does not get one
		assertNull(translated.getAttribute("ds", "multiscales", JsonElement.class));
		assertEquals(1, (int)translated.getAttribute("ds", "a", Integer.class));
		assertArrayEquals(new long[]{4, 3, 2}, translated.getDatasetAttributes("ds").getDimensions());
	}

	@Test
	public void testBackupNgffMultiscales() {

		n5.setAttribute("ms", "multiscales", json(NGFF_MULTISCALES));
		n5.setAttribute("ms", "k", 1);

		final TranslatedN5Reader translated = translate("getSubTree(\"/ms\") |= backupNgffMultiscales(\"ngffMultiscales\")");

		assertNull(translated.getAttribute("ms", "multiscales", JsonElement.class));
		assertEquals(json(NGFF_MULTISCALES), translated.getAttribute("ms", "ngffMultiscales", JsonElement.class));
		assertEquals(1, (int)translated.getAttribute("ms", "k", Integer.class));
		assertOnlyDialectAttributeFiles(translated, "ms");
	}
}

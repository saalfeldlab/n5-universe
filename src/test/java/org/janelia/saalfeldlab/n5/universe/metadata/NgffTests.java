package org.janelia.saalfeldlab.n5.universe.metadata;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.codec.transpose.TransposeCodecInfo;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffMultiScaleGroupAttributes.MultiscaleDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3DatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.imglib2.realtransform.AffineTransform3D;

public class NgffTests {

	// indexes
	public static final char X = 'x';
	public static final char Y = 'y';
	public static final char Z = 'z';
	public static final char C = 'c';
	public static final char T = 't';

	// indexes
	public static final int IX = 0;
	public static final int IY = 1;
	public static final int IZ = 2;
	public static final int IC = 3;
	public static final int IT = 4;

	// size per dimension
	public static final int NX = 6;
	public static final int NY = 5;
	public static final int NC = 2;
	public static final int NZ = 4;
	public static final int NT = 3;

	// resolution per dimension
	public static final double RX = 6;
	public static final double RY = 5;
	public static final double RC = 1;
	public static final double RZ = 4;
	public static final double RT = 3;

	// translation per dimension
	public static final double TX = 60;
	public static final double TY = 50;
	public static final double TC = 20;
	public static final double TZ = 40;
	public static final double TT = 30;

	public static final long[] DEFAULT_DIMENSIONS = new long[]{NX, NY, NC, NZ, NT};
	public static final int[]  DEFAULT_CHUNK_SIZE = new int[]{NX, NY, NC, NZ, NT};
	public static final char[] DEFAULT_AXES = new char[]{X, Y, C, Z, T};
	public static final String[] DEFAULT_AXES_S = charToString(DEFAULT_AXES);
	public static final double[] DEFAULT_RESOLUTION = new double[]{RX, RY, RC, RZ, RT};
	public static final double[] DEFAULT_TRANSLATION = new double[]{TX, TY, TC, TZ, TT};
	public static final int[] DEFAULT_NGFF_PERMUTATION = new int[]{0, 1, 3, 2, 4};

	private N5FSReader n5;

	public static String[] charToString(char[] arr) {

		final String[] out = new String[arr.length];
		for (int i = 0; i < arr.length; i++)
			out[i] = String.valueOf(arr[i]);

		return out;
	}

	@Before
	public void setUp() throws N5Exception {
		final String n5Root = "src/test/resources/ngff.n5";
		n5 = new N5FSReader(n5Root);
	}

	@Test
	public void testNgffGroupAttributeParsing() {

		final double eps = 1e-9;
		try {
			final NgffMultiScaleGroupAttributes[] multiscales = n5.getAttribute("ngff_grpAttributes", "multiscales", NgffMultiScaleGroupAttributes[].class);
			Assert.assertEquals("one set of multiscales", 1, multiscales.length);

			final MultiscaleDataset[] datasets = multiscales[0].datasets;
			Assert.assertEquals("num levels", 6, datasets.length);

			double scale = 4;
			for (int i = 0; i < datasets.length; i++) {

				final String pathName = String.format("s%d", i);
				Assert.assertEquals("path name " + i, pathName, datasets[i].path);
				Assert.assertEquals("scale " + i, scale, datasets[i].transform.scale[2], eps);

				scale *= 2;
			}

		} catch (final N5Exception e) {
			fail("Ngff parsing failed");
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks that the reverse boolean argument to OmeNgffMetadataParser behaves as expected. 
	 */
	@Test
	public void testNgffN5ParameterOrder() {

		final DatasetAttributes attrs = new DatasetAttributes(DEFAULT_DIMENSIONS, DEFAULT_CHUNK_SIZE, DataType.UINT8, new RawCompression());
		OmeNgffMultiScaleMetadata meta = buildPermutedAxesMetadata(DEFAULT_NGFF_PERMUTATION, false, attrs);

		final Gson gson = new OmeNgffMetadataParser().gsonBuilder().create();
		final JsonElement json = gson.toJsonTree(meta);

		final Gson gsonNoRev = new OmeNgffMetadataParser(false).gsonBuilder().create();
		final JsonElement jsonNoRev = gsonNoRev.toJsonTree(meta);

		assertReversedParameterOrder(json, jsonNoRev);
	}

	private void assertReversedParameterOrder(final JsonElement jsonRev, final JsonElement jsonNoRev) {

		final JsonObject objRev = jsonRev.getAsJsonObject();
		final JsonObject objNoRev = jsonNoRev.getAsJsonObject();

		// axes names should be element-wise reversed
		final JsonArray axes = objRev.getAsJsonArray("axes");
		final JsonArray axesNoRev = objNoRev.getAsJsonArray("axes");
		Assert.assertEquals("axes count", axes.size(), axesNoRev.size());
		final int n = axes.size();
		for (int i = 0; i < n; i++) {
			Assert.assertEquals("axis name at index " + i,
					axes.get(i).getAsJsonObject().get("name").getAsString(),
					axesNoRev.get(n - 1 - i).getAsJsonObject().get("name").getAsString());
		}

		// scale and translation values should be element-wise reversed
		final JsonArray transforms = objRev.getAsJsonArray("datasets")
				.get(0).getAsJsonObject()
				.getAsJsonArray("coordinateTransformations");
		final JsonArray transformsNoRev = objNoRev.getAsJsonArray("datasets")
				.get(0).getAsJsonObject()
				.getAsJsonArray("coordinateTransformations");

		for (final JsonElement t : transforms) {
			final JsonObject transform = t.getAsJsonObject();
			final String type = transform.get("type").getAsString();
			final JsonArray values = transform.getAsJsonArray(type);

			JsonArray valuesNoRev = null;
			for (final JsonElement tNoRev : transformsNoRev) {
				final JsonObject transformNoRev = tNoRev.getAsJsonObject();
				if (transformNoRev.get("type").getAsString().equals(type)) {
					valuesNoRev = transformNoRev.getAsJsonArray(type);
					break;
				}
			}
			Assert.assertNotNull("transform type '" + type + "' not found in non-reversed JSON", valuesNoRev);
			Assert.assertEquals(type + " length", values.size(), valuesNoRev.size());

			for (int i = 0; i < values.size(); i++) {
				Assert.assertEquals(type + " at index " + i,
						values.get(i).getAsDouble(),
						valuesNoRev.get(values.size() - 1 - i).getAsDouble(),
						1e-9);
			}
		}
	}

	/**
	 * The multiscales metadata of an OME-NGFF group may refer to the group
	 * itself ({@code "path" : "."}), in which case that node is both a
	 * multiscale group and an array.
	 * <p>
	 * Such a node must be discovered as a dataset, and must keep the spatial
	 * metadata that the multiscales describe. The multiscale metadata that
	 * {@link OmeNgffMetadataParser} returns describe the group, and are not
	 * openable on their own, so they must not replace the single scale metadata
	 * that the parser sets on the node.
	 */
	@Test
	public void testSelfReferencedMultiscales() throws IOException {

		// the container root is both the multiscale group and the array
		final String rootContainer = writeSelfReferencedMultiscales("");
		assertSelfReferencedDataset("shallow parse of root", shallowParse(rootContainer, ""));
		assertSelfReferencedDataset("deep parse of root", deepParse(rootContainer, ""));

		// a group below the root is both the multiscale group and the array
		final String nestedContainer = writeSelfReferencedMultiscales("a/b");
		assertSelfReferencedDataset("shallow parse of 'a/b'", shallowParse(nestedContainer, "a/b"));

		/*
		 * Deep parsing does not yet find a self-referenced multiscale group
		 * below the root. Group parsers only run for nodes without metadata that
		 * have children, and such a node has neither: the plain parsers
		 * recognize the array it is, and its only scale level is itself.
		 */
	}

	/**
	 * Writes a container with multiscales metadata at the given group path whose
	 * only dataset is the group itself.
	 *
	 * @param groupPath the group path
	 * @return the container root
	 */
	private static String writeSelfReferencedMultiscales(final String groupPath) throws IOException {

		final String root = Files.createTempDirectory("selfMultiscales").resolve("test.zarr").toString();
		final long[] dims = new long[]{27, 226, 186};
		final int[] blkSize = new int[]{16, 64, 64};

		try (final N5Writer zarr = new N5Factory().openWriter(StorageFormat.ZARR, root)) {
			zarr.createDataset(groupPath, dims, blkSize, DataType.UINT8, new RawCompression());
			zarr.setAttribute(groupPath, "multiscales",
					new Gson().fromJson(SELF_REFERENCED_MULTISCALES, JsonElement.class));
		}
		return root;
	}

	/** Parses a single node, as done when a container is first opened. */
	private static N5TreeNode shallowParse(final String containerRoot, final String groupPath) {

		try (final N5Reader zarr = new N5Factory().openReader(StorageFormat.ZARR, containerRoot)) {
			return N5DatasetDiscoverer.discoverShallow(zarr, groupPath);
		}
	}

	/** Parses the whole container, returning the node at the given group path. */
	private static N5TreeNode deepParse(final String containerRoot, final String groupPath) {

		try (final N5Reader zarr = new N5Factory().openReader(StorageFormat.ZARR, containerRoot)) {
			final Optional<N5TreeNode> node = N5DatasetDiscoverer.discover(zarr).getDescendant(groupPath);
			Assert.assertTrue("node discovered at '" + groupPath + "'", node.isPresent());
			return node.get();
		}
	}

	private static void assertSelfReferencedDataset(final String message, final N5TreeNode node) {

		final double eps = 1e-9;
		final N5Metadata meta = node.getMetadata();
		Assert.assertNotNull(message + " has metadata", meta);
		Assert.assertTrue(message + " is a dataset, but was " + meta.getClass().getSimpleName(), node.isDataset());

		// the multiscales describe the array, so its spatial metadata must survive
		Assert.assertTrue(message + " has spatial metadata, but was " + meta.getClass().getSimpleName(),
				meta instanceof N5SpatialDatasetMetadata);

		// zarr parameters are reversed, so the json scale [2.2, 0.9, 0.9] is (x,y,z) here
		final AffineTransform3D transform = ((N5SpatialDatasetMetadata)meta).spatialTransform3d();
		Assert.assertEquals(message + " x scale", 0.9, transform.get(0, 0), eps);
		Assert.assertEquals(message + " y scale", 0.9, transform.get(1, 1), eps);
		Assert.assertEquals(message + " z scale", 2.2, transform.get(2, 2), eps);
		Assert.assertEquals(message + " unit", "mm", ((N5SpatialDatasetMetadata)meta).unit());

		Assert.assertArrayEquals(message + " axis labels",
				new String[]{"x", "y", "z"}, ((AxisMetadata)meta).getAxisLabels());
	}

	private static final String SELF_REFERENCED_MULTISCALES =
			"[{"
			+ "  'name': 'multiscales-dataset-self',"
			+ "  'type': 'Average',"
			+ "  'version': '0.4',"
			+ "  'axes': ["
			+ "    {'type': 'space', 'name': 'z', 'unit': 'mm'},"
			+ "    {'type': 'space', 'name': 'y', 'unit': 'mm'},"
			+ "    {'type': 'space', 'name': 'x', 'unit': 'mm'}"
			+ "  ],"
			+ "  'datasets': [{"
			+ "    'path': '.',"
			+ "    'coordinateTransformations': ["
			+ "      {'scale': [2.2, 0.9, 0.9], 'type': 'scale'},"
			+ "      {'translation': [0, 0, 0], 'type': 'translation'}"
			+ "    ]"
			+ "  }]"
			+ "}]";

	public static OmeNgffMultiScaleMetadata parse(final N5Writer zarr, final String base) {

		final N5TreeNode root = N5DatasetDiscoverer.discover(zarr);
		return (OmeNgffMultiScaleMetadata)root.getDescendant(base).map(n -> n.getMetadata()).orElse(null);
	}

	public static OmeNgffMultiScaleMetadata buildPermutedAxesMetadata(final int[] permutation, final DatasetAttributes dsetAttrs) {

		return buildPermutedAxesMetadata(permutation, true, dsetAttrs);
	}

	public static OmeNgffMultiScaleMetadata buildPermutedAxesMetadata(final int[] permutation, final boolean cOrder, final DatasetAttributes dsetAttrs) {

		final HashMap<String,Double> axisResolution = new HashMap<>();
		final HashMap<String, Double> axisTranslation = new HashMap<>();
		for (int i = 0; i < DEFAULT_AXES.length; i++) {
			axisResolution.put(DEFAULT_AXES_S[i], DEFAULT_RESOLUTION[i]);
			axisTranslation.put(DEFAULT_AXES_S[i], DEFAULT_TRANSLATION[i]);
		}

		final String[] axesLabels = new String[permutation.length];
		AxisUtils.permute(DEFAULT_AXES_S, axesLabels, permutation);

		final double[] resolution = AxisUtils.permute(DEFAULT_RESOLUTION, permutation);
		final double[] translation = AxisUtils.permute(DEFAULT_TRANSLATION, permutation);

		final NgffSingleScaleAxesMetadata s0Meta = new NgffSingleScaleAxesMetadata("s0", resolution, translation, dsetAttrs);
		final OmeNgffDataset[] dsets = new OmeNgffDataset[] { new OmeNgffDataset() };
		dsets[0].path = s0Meta.getPath();
		dsets[0].coordinateTransformations = s0Meta.getCoordinateTransformations();


		final Axis[] axes = AxisUtils.defaultAxes(axesLabels);

		if (cOrder) {
			ArrayUtils.reverse(resolution);
			ArrayUtils.reverse(translation);
			ArrayUtils.reverse(axes);
		}

		final int nd = axes.length;
		return new OmeNgffMultiScaleMetadata(
				nd, "", "test", "type", "0.4",
				axes, dsets,
				null, // coordinate transformations
				new DatasetAttributes[]{dsetAttrs},
				null, // metadata
				new NgffSingleScaleAxesMetadata[]{s0Meta});
	}

	public static CosemTransform buildPermutedAxesCosemMetadata(
			final int[] permutation, final boolean cOrder, final DatasetAttributes dsetAttrs) {

		final HashMap<String, Double> axisResolution = new HashMap<>();
		final HashMap<String, Double> axisTranslation = new HashMap<>();
		for (int i = 0; i < DEFAULT_AXES.length; i++) {
			axisResolution.put(DEFAULT_AXES_S[i], DEFAULT_RESOLUTION[i]);
			axisTranslation.put(DEFAULT_AXES_S[i], DEFAULT_TRANSLATION[i]);
		}

		final String[] axesLabels = new String[permutation.length];
		AxisUtils.permute(DEFAULT_AXES_S, axesLabels, permutation);

		final double[] resolution = AxisUtils.permute(DEFAULT_RESOLUTION, permutation);
		final double[] translation = AxisUtils.permute(DEFAULT_TRANSLATION, permutation);

		final NgffSingleScaleAxesMetadata s0Meta = new NgffSingleScaleAxesMetadata("s0", resolution, translation, dsetAttrs);
		final OmeNgffDataset[] dsets = new OmeNgffDataset[]{new OmeNgffDataset()};
		dsets[0].path = s0Meta.getPath();
		dsets[0].coordinateTransformations = s0Meta.getCoordinateTransformations();

		final Axis[] axes = AxisUtils.defaultAxes(axesLabels);
		final String[] units = IntStream.range(0, axes.length).mapToObj(x -> "").toArray(n -> {
			return new String[n];
		});

		String ordering;
		if (cOrder) {
			ArrayUtils.reverse(resolution);
			ArrayUtils.reverse(translation);
			ArrayUtils.reverse(axesLabels);
			ordering = "C";
		}
		else
			ordering = "F";

		return new N5CosemMetadata.CosemTransform(axesLabels, resolution, translation, units, ordering);
	}

	public static void writePermutedAxes(final N5Writer zarr, final String base, final boolean cOrder, final int[] permutation) {


		final long[] dims = AxisUtils.permute(DEFAULT_DIMENSIONS, permutation);
		final int[] blkSize = Arrays.stream(dims).mapToInt(x -> (int)x).toArray();

		final String dsetPath = base + "/s0";
		createDataset(zarr, cOrder, dsetPath, dims, blkSize, DataType.UINT8, new RawCompression());

		final DatasetAttributes dsetAttrs = zarr.getDatasetAttributes(dsetPath);

		final OmeNgffMultiScaleMetadata meta = NgffTests.buildPermutedAxesMetadata(permutation, cOrder, dsetAttrs);
		zarr.setAttribute(base, "multiscales", new OmeNgffMultiScaleMetadata[]{meta});
	}

	public static void createDataset(
			final N5Writer zarr,
			final boolean cOrder,
			final String datasetPath,
			final long[] dimensions,
			final int[] blockSize,
			final DataType dataType,
			final Compression compression) throws N5Exception {

		if (cOrder) {
			zarr.createDataset(datasetPath, dimensions, blockSize, dataType, compression);
		}
		else {
			final long[] dimsRev = ArrayUtils.clone(dimensions);
			final int[] blkSizeRev = ArrayUtils.clone(blockSize);

			if( zarr instanceof ZarrV3KeyValueWriter ) {

				ArrayUtils.reverse(dimsRev);
				ArrayUtils.reverse(blkSizeRev);

				final int nd = dimsRev.length;
				final int[] revOrder = new int[nd];
				for( int i = 0; i < nd; i++ ) {
					revOrder[i] = nd - i - 1;
				}

				final ZarrV3DatasetAttributes attrs = ZarrV3DatasetAttributes.builder(dimsRev, dataType)
					.blockSize(blkSizeRev)
					.datasetCodecInfos(new TransposeCodecInfo(revOrder))
					.build();
				zarr.createDataset(datasetPath, attrs);

			} else if( zarr instanceof ZarrKeyValueWriter ) {
				zarr.createDataset(datasetPath, dimsRev, blkSizeRev, dataType, compression);
				zarr.setAttribute(datasetPath, "order", "F");
			}

		}
	}

	public static int[] permutationFromName(final String name) {

		final String nameNorm = name.toLowerCase();
		final String[] axesOrder = nameNorm.split("_");
		final char[] axes = axesOrder[0].toCharArray();

		final int[] p = new int[axes.length];
		for (int i = 0; i < axes.length; i++) {
			p[i] = ArrayUtils.indexOf(DEFAULT_AXES, axes[i]);
		}
		return p;
	}

	public static boolean isCOrderFromName(final String name) {

		return name.toLowerCase().split("_")[1].startsWith("c");
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffMultiScaleGroupAttributes.MultiscaleDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.codec.transpose.TransposeCodecInfo;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3DatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

		final OmeNgffMultiScaleMetadata meta = NgffTests.buildPermutedAxesMetadata(permutation, true, dsetAttrs);
		zarr.setAttribute(base, "multiscales", new OmeNgffMultiScaleMetadata[]{meta});
	}

	/**
	 * Create a dataset whose elements are stored in C (row-major) or F
	 * (column-major) order, for testing how C- and F-order zarr arrays are read.
	 * <p>
	 * {@code dimensions} and {@code blockSize} are always in N5 order, the order
	 * of {@link DatasetAttributes#getDimensions()}, which is the reverse of a
	 * zarr {@code shape}. {@code cOrder} does <em>not</em> say how the arguments
	 * are ordered. It selects the storage order of the array that is written.
	 * <p>
	 * In both zarr versions, the {@code shape} written to the zarr metadata is
	 * {@code reverse(dimensions)} for C-order and {@code dimensions} itself for
	 * F-order. What differs is the N5 dimensions the array then has. For example,
	 * with {@code dimensions = [6,5,4]}:
	 * <table>
	 * <caption>resulting arrays</caption>
	 * <tr><th>writer</th><th>cOrder</th><th>zarr metadata</th><th>N5 dimensions</th></tr>
	 * <tr><td>any</td><td>true</td><td>{@code "shape": [4,5,6]} (zarr)</td><td>[6,5,4]</td></tr>
	 * <tr><td>Zarr v2</td><td>false</td><td>{@code "shape": [6,5,4], "order": "F"}</td><td>[6,5,4]</td></tr>
	 * <tr><td>Zarr v3</td><td>false</td><td>{@code "shape": [6,5,4]}, transpose codec</td><td>[4,5,6]</td></tr>
	 * </table>
	 * So for Zarr v2, F-order keeps the N5 dimensions. Only the layout of the
	 * elements within each chunk changes. Zarr v3 has no {@code "order"}: the
	 * array is created with reversed dimensions and a transpose codec that
	 * reverses the axes, so its N5 dimensions are reversed.
	 * <p>
	 * NB: n5-zarr 2.0.0 does not write the transpose codec to {@code
	 * zarr.json}, while still applying it to the chunks it writes. With that
	 * version, the Zarr v3 F-order array reads back as a C-order array with
	 * reversed dimensions and transposed chunk data.
	 *
	 * @param zarr
	 * 		the writer; must be a Zarr v2 or v3 writer unless {@code cOrder}
	 * @param cOrder
	 * 		whether to store the elements in C order (the default) or in F order
	 * @param datasetPath
	 * 		the dataset path
	 * @param dimensions
	 * 		the dimensions, in N5 order
	 * @param blockSize
	 * 		the block size, in N5 order
	 * @param dataType
	 * 		the data type
	 * @param compression
	 * 		the compression; ignored for Zarr v3 F-order
	 *
	 * @throws IllegalArgumentException
	 * 		if {@code cOrder} is false and {@code zarr} is not a Zarr writer
	 */
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
		} else if (zarr instanceof ZarrV3KeyValueWriter) {
			// Zarr v3 has no "order". Store the reversed shape, with a transpose codec.
			final long[] dimsRev = ArrayUtils.clone(dimensions);
			final int[] blkSizeRev = ArrayUtils.clone(blockSize);
			ArrayUtils.reverse(dimsRev);
			ArrayUtils.reverse(blkSizeRev);
			final int nd = dimsRev.length;
			final int[] revOrder = new int[nd];
			for (int i = 0; i < nd; i++)
				revOrder[i] = nd - i - 1;
			zarr.createDataset(datasetPath, ZarrV3DatasetAttributes.builder(dimsRev, dataType)
					.blockSize(blkSizeRev)
					.datasetCodecInfos(new TransposeCodecInfo(revOrder))
					.build());
		} else if (zarr instanceof ZarrKeyValueWriter) {
			// Zarr v2 "order": "F". Same dimensions, column-major chunks.
			zarr.createDataset(datasetPath, new ZarrDatasetAttributes(
					dimensions, blockSize, new DType(dataType), compression,
					false, // isRowMajor
					null)); // fill_value
		} else {
			throw new IllegalArgumentException("F-order requires a zarr writer");
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

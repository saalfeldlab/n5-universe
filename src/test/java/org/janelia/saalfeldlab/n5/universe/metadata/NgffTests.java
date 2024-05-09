package org.janelia.saalfeldlab.n5.universe.metadata;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;

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
import org.janelia.saalfeldlab.n5.universe.metadata.NgffMultiScaleGroupAttributes.MultiscaleDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
	public static final double RC = 2;
	public static final double RZ = 4;
	public static final double RT = 3;

	// translation per dimension
	public static final double TX = 60;
	public static final double TY = 50;
	public static final double TC = 20;
	public static final double TZ = 40;
	public static final double TT = 30;

	public static final long[] DEFAULT_DIMENSIONS = new long[]{NX, NY, NC, NZ, NT};
	public static final char[] DEFAULT_AXES = new char[]{X, Y, C, Z, T};
	public static final String[] DEFAULT_AXES_S = charToString(DEFAULT_AXES);
	public static final double[] DEFAULT_RESOLUTION = new double[]{RX, RY, RC, RZ, RT};
	public static final double[] DEFAULT_TRANSLATION = new double[]{TX, TY, TC, TZ, TT};

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
				axes,
				dsets,
				new DatasetAttributes[] { dsetAttrs },
				null, null);

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

	public static void createDataset(
			final N5Writer zarr,
			final boolean cOrder,
			final String datasetPath,
			final long[] dimensions,
			final int[] blockSize,
			final DataType dataType,
			final Compression compression) throws N5Exception {

		assert zarr instanceof ZarrKeyValueWriter;

		if (cOrder) {
			zarr.createDataset(datasetPath, dimensions, blockSize, dataType, compression);
		}
		else {
			final long[] dimsRev = ArrayUtils.clone(dimensions);
			ArrayUtils.reverse(dimsRev);

			final int[] blkSizeRev = ArrayUtils.clone(blockSize);
			ArrayUtils.reverse(blkSizeRev);

			zarr.createDataset(datasetPath, dimsRev, blkSizeRev, dataType, compression);
			zarr.setAttribute(datasetPath, "order", "F");
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

		return name.toLowerCase().split("_")[1].equals("c");
	}

}

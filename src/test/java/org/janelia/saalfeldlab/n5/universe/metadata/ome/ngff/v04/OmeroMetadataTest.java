package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero.OmeroChannel;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero.OmeroMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero.OmeroRdefs;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.omero.OmeroWindow;
import org.junit.Test;

import com.google.gson.Gson;

public class OmeroMetadataTest {

	private static final double EPS = 1e-9;

	/** the example from the ome-ngff specification */
	private static final String OMERO_JSON = "{"
			+ "\"id\": 1,"
			+ "\"name\": \"example.tif\","
			+ "\"version\": \"0.4\","
			+ "\"channels\": [{"
			+ "    \"active\": true,"
			+ "    \"coefficient\": 1,"
			+ "    \"color\": \"0000FF\","
			+ "    \"family\": \"linear\","
			+ "    \"inverted\": false,"
			+ "    \"label\": \"LaminB1\","
			+ "    \"window\": { \"end\": 1500, \"max\": 65535, \"min\": 0, \"start\": 0 }"
			+ "}],"
			+ "\"rdefs\": { \"defaultT\": 0, \"defaultZ\": 118, \"model\": \"color\" }"
			+ "}";

	private static Gson gson() {

		return new OmeNgffMetadataParser().gsonBuilder().create();
	}

	@Test
	public void testDeserializeOmero() {

		final OmeroMetadata omero = gson().fromJson(OMERO_JSON, OmeroMetadata.class);

		assertEquals("id", 1, omero.id);
		assertEquals("name", "example.tif", omero.name);
		assertEquals("version", "0.4", omero.version);

		assertEquals("num channels", 1, omero.numChannels());
		final OmeroChannel c = omero.getChannel(0);
		assertTrue("active", c.active);
		assertEquals("coefficient", 1.0, c.coefficient, EPS);
		assertEquals("color", "0000FF", c.color);
		assertEquals("family", OmeroChannel.FAMILY_LINEAR, c.family);
		assertFalse("inverted", c.inverted);
		assertEquals("label", "LaminB1", c.label);
		assertEquals("color argb", 0xff0000ff, c.getColorARGB());

		final OmeroWindow w = c.window;
		assertEquals("window min", 0.0, w.min, EPS);
		assertEquals("window max", 65535.0, w.max, EPS);
		assertEquals("window start", 0.0, w.start, EPS);
		assertEquals("window end", 1500.0, w.end, EPS);

		final OmeroRdefs rdefs = omero.rdefs;
		assertEquals("defaultT", 0, rdefs.defaultT);
		assertEquals("defaultZ", 118, rdefs.defaultZ);
		assertEquals("model", OmeroRdefs.MODEL_COLOR, rdefs.model);
		assertTrue("is color", rdefs.isColor());
		assertFalse("is greyscale", rdefs.isGreyscale());
	}

	@Test
	public void testSerializeOmero() {

		final Gson gson = gson();
		final OmeroMetadata omero = gson.fromJson(OMERO_JSON, OmeroMetadata.class);

		// serializing and deserializing again must not change anything
		final OmeroMetadata rt = gson.fromJson(gson.toJson(omero), OmeroMetadata.class);
		assertOmeroEquals(omero, rt);
	}

	/**
	 * The spec only requires "channels", so ensure the optional fields are
	 * absent rather than defaulted when missing.
	 */
	@Test
	public void testDeserializeMinimalOmero() {

		final String json = "{\"channels\": [{"
				+ "\"color\": \"FFFFFF\","
				+ "\"window\": { \"end\": 255, \"max\": 255, \"min\": 0, \"start\": 0 }}]}";

		final OmeroMetadata omero = gson().fromJson(json, OmeroMetadata.class);

		assertEquals("num channels", 1, omero.numChannels());
		assertNull("name", omero.name);
		assertNull("version", omero.version);
		assertNull("rdefs", omero.rdefs);

		final OmeroChannel c = omero.getChannel(0);
		assertNull("family", c.family);
		assertNull("label", c.label);
		assertFalse("active defaults to false", c.active);
	}

	@Test
	public void testWriteReadOmero() throws IOException {

		final OmeroMetadata omero = gson().fromJson(OMERO_JSON, OmeroMetadata.class);

		final OmeNgffMetadata meta = writeAndRead(omero, "0.4");
		assertNotNull("omero present", meta.omero);
		assertOmeroEquals(omero, meta.omero);
	}

	@Test
	public void testWriteReadOmeroV05() throws IOException {

		final OmeroMetadata omero = gson().fromJson(OMERO_JSON, OmeroMetadata.class);

		final OmeNgffMetadata meta = writeAndRead(omero, "0.5");
		assertNotNull("omero present", meta.omero);
		assertOmeroEquals(omero, meta.omero);
	}

	/**
	 * omero is optional, so metadata without it must still parse.
	 */
	@Test
	public void testWriteReadNoOmero() throws IOException {

		final OmeNgffMetadata meta = writeAndRead(null, "0.4");
		assertNull("omero absent", meta.omero);
	}

	private static OmeNgffMetadata writeAndRead(final OmeroMetadata omero, final String version)
			throws IOException {

		final String root = Files.createTempDirectory("omero-metadata-test-").toFile()
				.getCanonicalPath() + "/test.zarr";

		final String group = "img";
		final Axis[] axes = AxisUtils.defaultAxes("x", "y", "c");
		final OmeNgffMetadata meta = OmeNgffMetadata.buildForWriting(3, "test", version, axes,
				new String[]{"s0"},
				new double[][]{{1, 1, 1}},
				new double[][]{{0, 0, 0}},
				omero);

		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();
		try (final N5Writer zarr = new N5Factory().openWriter(root)) {

			zarr.createDataset(group + "/s0", new long[]{4, 4, 1}, new int[]{4, 4, 1},
					DataType.UINT8, new RawCompression());

			try {
				parser.writeMetadata(meta, zarr, group);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}

			final Optional<OmeNgffMetadata> parsed = parser.parseMetadata(zarr, new N5TreeNode(group));
			assertTrue("metadata parsed for version " + version, parsed.isPresent());

			zarr.remove();
			return parsed.get();
		}
	}

	private static void assertOmeroEquals(final OmeroMetadata expected, final OmeroMetadata actual) {

		assertEquals("id", expected.id, actual.id);
		assertEquals("name", expected.name, actual.name);
		assertEquals("version", expected.version, actual.version);
		assertEquals("num channels", expected.numChannels(), actual.numChannels());

		for (int i = 0; i < expected.numChannels(); i++) {

			final OmeroChannel e = expected.getChannel(i);
			final OmeroChannel a = actual.getChannel(i);

			assertEquals("channel " + i + " active", e.active, a.active);
			assertEquals("channel " + i + " coefficient", e.coefficient, a.coefficient, EPS);
			assertEquals("channel " + i + " color", e.color, a.color);
			assertEquals("channel " + i + " family", e.family, a.family);
			assertEquals("channel " + i + " inverted", e.inverted, a.inverted);
			assertEquals("channel " + i + " label", e.label, a.label);

			assertEquals("channel " + i + " window min", e.window.min, a.window.min, EPS);
			assertEquals("channel " + i + " window max", e.window.max, a.window.max, EPS);
			assertEquals("channel " + i + " window start", e.window.start, a.window.start, EPS);
			assertEquals("channel " + i + " window end", e.window.end, a.window.end, EPS);
		}

		assertEquals("defaultT", expected.rdefs.defaultT, actual.rdefs.defaultT);
		assertEquals("defaultZ", expected.rdefs.defaultZ, actual.rdefs.defaultZ);
		assertEquals("model", expected.rdefs.model, actual.rdefs.model);
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Validates that {@link MultiscalesAdapter} serializes {@code coordinateSystems}
 * (not a top-level {@code axes}), reversed for zarr, as used by BigWarp's field
 * export (see NGFF_scene_export_plan Stage 1).
 */
public class FieldMultiscalesSerializeTest {

	private static Gson gson(final boolean reverse) {
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(reverse))
				.create();
	}

	private static OmeNgffMultiScaleMetadata buildFieldMultiscales() {
		// imglib2 (x,y,z) order with a displacement axis prepended: [d, x, y]
		final Axis[] axes = new Axis[] {
				new Axis("displacement", "d", null, true),
				new Axis("space", "x", "pixel"),
				new Axis("space", "y", "pixel") };
		final CoordinateSystem[] cs = new CoordinateSystem[] { new CoordinateSystem("field", axes) };

		final OmeNgffMultiScaleMetadata.OmeNgffDataset dset = new OmeNgffMultiScaleMetadata.OmeNgffDataset();
		dset.path = ".";
		dset.coordinateTransformations = new CoordinateTransform[] {
				new ScaleCoordinateTransform("", ".", "field", new double[] { 1, 2, 3 }) };

		return new OmeNgffMultiScaleMetadata(
				3, "/", null, null, "0.5", null,
				new OmeNgffMultiScaleMetadata.OmeNgffDataset[] { dset },
				cs, null, null, null,
				new NgffSingleScaleAxesMetadata[ 0 ]);
	}

	@Test
	public void testZarrReversed() {
		final JsonObject obj = gson(true).toJsonTree(buildFieldMultiscales()).getAsJsonObject();

		assertFalse("no top-level axes when coordinateSystems present", obj.has("axes"));
		assertTrue("has coordinateSystems", obj.has("coordinateSystems"));

		final JsonObject cs0 = obj.getAsJsonArray("coordinateSystems").get(0).getAsJsonObject();
		assertEquals("field", cs0.get("name").getAsString());
		final JsonArray csAxes = cs0.getAsJsonArray("axes");
		// reversed for zarr: [d,x,y] -> [y,x,d]
		assertEquals("y", csAxes.get(0).getAsJsonObject().get("name").getAsString());
		assertEquals("x", csAxes.get(1).getAsJsonObject().get("name").getAsString());
		assertEquals("d", csAxes.get(2).getAsJsonObject().get("name").getAsString());

		final JsonObject ds0 = obj.getAsJsonArray("datasets").get(0).getAsJsonObject();
		assertEquals(".", ds0.get("path").getAsString());
		final JsonObject scale = ds0.getAsJsonArray("coordinateTransformations").get(0).getAsJsonObject();
		final JsonArray sc = scale.getAsJsonArray("scale");
		// reversed for zarr: [1,2,3] -> [3,2,1]
		assertEquals(3.0, sc.get(0).getAsDouble(), 1e-9);
		assertEquals(2.0, sc.get(1).getAsDouble(), 1e-9);
		assertEquals(1.0, sc.get(2).getAsDouble(), 1e-9);
	}

	@Test
	public void testN5NotReversed() {
		final JsonObject obj = gson(false).toJsonTree(buildFieldMultiscales()).getAsJsonObject();

		assertTrue(obj.has("coordinateSystems"));
		final JsonArray csAxes = obj.getAsJsonArray("coordinateSystems").get(0).getAsJsonObject().getAsJsonArray("axes");
		assertEquals("d", csAxes.get(0).getAsJsonObject().get("name").getAsString());
		assertEquals("x", csAxes.get(1).getAsJsonObject().get("name").getAsString());
		assertEquals("y", csAxes.get(2).getAsJsonObject().get("name").getAsString());
	}
}

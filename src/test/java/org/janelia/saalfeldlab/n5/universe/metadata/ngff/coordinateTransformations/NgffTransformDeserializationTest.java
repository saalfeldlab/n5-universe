package org.janelia.saalfeldlab.n5.universe.metadata.ngff.coordinateTransformations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ByDimensionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.SubAxisTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.ScaleCoordinateTransform;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.imglib2.realtransform.RealTransform;

public class NgffTransformDeserializationTest {

	private static Gson buildGson() {
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(null, false))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.create();
	}

	@Test
	public void testByDimensionSubAxesDeserialization() {

		final String json = "{"
				+ "\"type\": \"byDimension\","
				+ "\"name\": \"test\","
				+ "\"input\": {\"name\": \"input\"},"
				+ "\"output\": {\"name\": \"output\"},"
				+ "\"transformations\": ["
				+ "  {\"transformation\": {\"type\": \"identity\"}, \"inputAxes\": [0], \"outputAxes\": [0]},"
				+ "  {\"transformation\": {\"type\": \"scale\", \"scale\": [10.0]}, \"inputAxes\": [1], \"outputAxes\": [1]}"
				+ "]"
				+ "}";

		final Gson gson = buildGson();
		final CoordinateTransform<?> ct = gson.fromJson(json, CoordinateTransform.class);

		assertNotNull("byDimension transform should deserialize", ct);

		// the axes of each sub-transform are held by the byDimension
		final ByDimensionCoordinateTransform byDim = (ByDimensionCoordinateTransform) ct;

		final CoordinateTransform<?>[] sub = byDim.getTransformations();
		assertNotNull("sub-transforms should not be null", sub);

		final SubAxisTransformation[] entries = byDim.getEntries();
		assertArrayEquals("first sub-transform input axes", new int[]{0}, entries[0].getInputAxes());
		assertArrayEquals("first sub-transform output axes", new int[]{0}, entries[0].getOutputAxes());
		assertArrayEquals("second sub-transform input axes", new int[]{1}, entries[1].getInputAxes());
		assertArrayEquals("second sub-transform output axes", new int[]{1}, entries[1].getOutputAxes());
	}

	@Test
	public void testByDimensionAxesRoundTrip() {

		final ByDimensionCoordinateTransform bd = ByDimensionCoordinateTransform.options()
				.name("bd").input("a").output("b")
				.transformations(
						new SubAxisTransformation(new ScaleCoordinateTransform(new double[]{2.0}), new int[]{1}, new int[]{1}),
						new SubAxisTransformation(new ScaleCoordinateTransform(new double[]{3.0, 4.0}), new int[]{0, 2}, new int[]{0, 2}))
				.build();

		final Gson gson = buildGson();
		final CoordinateTransform<?> ct = gson.fromJson(gson.toJson(bd, CoordinateTransform.class), CoordinateTransform.class);
		assertTrue("is a byDimension", ct instanceof ByDimensionCoordinateTransform);

		final SubAxisTransformation[] entries = ((ByDimensionCoordinateTransform)ct).getEntries();
		assertEquals(2, entries.length);
		assertArrayEquals(new int[]{1}, entries[0].getInputAxes());
		assertArrayEquals(new int[]{1}, entries[0].getOutputAxes());
		assertArrayEquals(new int[]{0, 2}, entries[1].getInputAxes());
		assertArrayEquals(new int[]{0, 2}, entries[1].getOutputAxes());
	}

	@Test
	public void testByDimensionInverseSwapsAxes() {

		final ByDimensionCoordinateTransform bd = ByDimensionCoordinateTransform.options()
				.name("bd").input("a").output("b")
				.transformations(
						new SubAxisTransformation(new ScaleCoordinateTransform(new double[]{2.0}), new int[]{0}, new int[]{1}),
						new SubAxisTransformation(new ScaleCoordinateTransform(new double[]{3.0}), new int[]{1}, new int[]{0}))
				.build();

		final ByDimensionCoordinateTransform inv = bd.inverse();
		assertNotNull("inverse", inv);

		final SubAxisTransformation[] entries = inv.getEntries();
		assertArrayEquals(new int[]{1}, entries[0].getInputAxes());
		assertArrayEquals(new int[]{0}, entries[0].getOutputAxes());
		assertArrayEquals(new int[]{0}, entries[1].getInputAxes());
		assertArrayEquals(new int[]{1}, entries[1].getOutputAxes());
	}

	@Test
	public void testByDimensionIdentityChildTakesDimensionsFromAxes() {

		final ByDimensionCoordinateTransform bd = ByDimensionCoordinateTransform.options()
				.name("bd").input("a").output("b")
				.transformations(
						new SubAxisTransformation(new IdentityCoordinateTransform(), new int[]{0, 1}, new int[]{0, 1}),
						new SubAxisTransformation(new ScaleCoordinateTransform(new double[]{2.0}), new int[]{2}, new int[]{2}))
				.build();

		final RealTransform t = bd.getTransform();
		assertEquals(3, t.numSourceDimensions());
		assertEquals(3, t.numTargetDimensions());

		final double[] out = new double[3];
		t.apply(new double[]{5, 6, 7}, out);
		assertArrayEquals(new double[]{5, 6, 14}, out, 1e-9);
	}

}

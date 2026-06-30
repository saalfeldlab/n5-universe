package org.janelia.saalfeldlab.n5.universe.metadata.ngff.coordinateTransformations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
				+ "  {\"transformation\": {\"type\": \"identity\"}, \"input_axes\": [0], \"output_axes\": [0]},"
				+ "  {\"transformation\": {\"type\": \"scale\", \"scale\": [10.0]}, \"input_axes\": [1], \"output_axes\": [1]}"
				+ "]"
				+ "}";

		final Gson gson = buildGson();
		final CoordinateTransform<?> ct = gson.fromJson(json, CoordinateTransform.class);

		assertNotNull("byDimension transform should deserialize", ct);

		// sub-transforms should have input_axes / output_axes populated
		final org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ByDimensionCoordinateTransform byDim =
				(org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ByDimensionCoordinateTransform) ct;

		final CoordinateTransform<?>[] sub = byDim.getTransformations();
		assertNotNull("sub-transforms should not be null", sub);

		assertArrayEquals("first sub-transform input axes", new int[]{0}, sub[0].getInputAxes());
		assertArrayEquals("first sub-transform output axes", new int[]{0}, sub[0].getOutputAxes());
		assertArrayEquals("second sub-transform input axes", new int[]{1}, sub[1].getInputAxes());
		assertArrayEquals("second sub-transform output axes", new int[]{1}, sub[1].getOutputAxes());
	}

}

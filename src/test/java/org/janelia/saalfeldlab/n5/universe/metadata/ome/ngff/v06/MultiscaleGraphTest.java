package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.MultiscalesAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tests {@link OmeNgffMultiScaleMetadata#getGraph()} -- the self-contained
 * {@link TransformGraph} built from a single multiscales dataset's own
 * {@code coordinateSystems} and multiscale-level "additional"
 * {@code coordinateTransformations} (no {@code NgffScene}, no {@link org.janelia.saalfeldlab.n5.N5Reader}
 * needed). This is the scene-free counterpart to
 * {@link org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene#getGraph(org.janelia.saalfeldlab.n5.N5Reader, String)}.
 */
public class MultiscaleGraphTest {

	private static Gson gson() {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(false))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(false))
				.create();
	}

	private static final String PHYSICAL_CS =
			"{\"name\": \"physical\", \"axes\": ["
					+ "{\"name\": \"z\", \"type\": \"space\", \"unit\": \"micrometer\"},"
					+ "{\"name\": \"y\", \"type\": \"space\", \"unit\": \"micrometer\"},"
					+ "{\"name\": \"x\", \"type\": \"space\", \"unit\": \"micrometer\"}]}";

	private static final String ROTATED_CS =
			"{\"name\": \"rotated\", \"axes\": ["
					+ "{\"name\": \"z\", \"type\": \"space\", \"unit\": \"micrometer\"},"
					+ "{\"name\": \"y\", \"type\": \"space\", \"unit\": \"micrometer\"},"
					+ "{\"name\": \"x\", \"type\": \"space\", \"unit\": \"micrometer\"}]}";

	private static final String S0_DATASET =
			"{\"path\": \"s0\", \"coordinateTransformations\": ["
					+ "{\"type\": \"scale\", \"input\": {\"path\": \"s0\"}, \"output\": {\"name\": \"physical\"},"
					+ " \"scale\": [100.0, 100.0, 100.0]}]}";

	private static Set<String> nodeNames(final TransformGraph graph) {

		return graph.getCoordinateSystems().coordinateSystems()
				.map(CoordinateSystem::getName)
				.collect(Collectors.toSet());
	}

	/**
	 * A multiscales that declares a second coordinate system ({@code rotated})
	 * and a multiscale-level {@code physical -> rotated} transform: its
	 * {@code getGraph()} should register both coordinate systems as nodes and
	 * connect them, so a path from the dataset's local space ({@code physical},
	 * the first coordinate system) to {@code rotated} resolves.
	 */
	@Test
	public void testGraphFromMultiscaleWithExtraTransform() {

		final String json = "{"
				+ "\"name\": \"withExtra\","
				+ "\"coordinateSystems\": [" + PHYSICAL_CS + ", " + ROTATED_CS + "],"
				+ "\"coordinateTransformations\": ["
				+ "  {\"type\": \"translation\", \"input\": {\"name\": \"physical\"}, \"output\": {\"name\": \"rotated\"},"
				+ "   \"translation\": [10.0, 20.0, 30.0]}"
				+ "],"
				+ "\"datasets\": [" + S0_DATASET + "]"
				+ "}";

		final OmeNgffMultiScaleMetadata ms = gson().fromJson(json, OmeNgffMultiScaleMetadata.class);
		assertNotNull("multiscales should deserialize", ms);

		final TransformGraph graph = ms.getGraph();

		// both declared coordinate systems become graph nodes
		assertEquals("both coordinate systems are nodes",
				setOf("physical", "rotated"), nodeNames(graph));

		// the dataset's local space (coordinateSystems[0]) is "physical"
		assertEquals("physical", ms.getCoordinateSystems()[0].getName());

		// the multiscale-level transform connects physical -> rotated
		assertTrue("expected a path from physical to rotated",
				graph.path("physical", "rotated").isPresent());
		// the auto-synthesized inverse edge connects the reverse direction too
		assertTrue("expected a path from rotated to physical",
				graph.path("rotated", "physical").isPresent());
		// a coordinate system is trivially reachable from itself
		assertTrue("expected an identity self-path on physical",
				graph.path("physical", "physical").isPresent());
	}

	/**
	 * A multiscales that declares a coordinate system but no multiscale-level
	 * {@code coordinateTransformations} (the common case, e.g. every dataset in
	 * {@code scene.ome.zarr}): {@code getGraph()} must not throw, must register
	 * the declared coordinate system as a node, and must have no path out of it
	 * to any other space.
	 */
	@Test
	public void testGraphFromMultiscaleNoExtraTransform() {

		final String json = "{"
				+ "\"name\": \"noExtra\","
				+ "\"coordinateSystems\": [" + PHYSICAL_CS + "],"
				+ "\"datasets\": [" + S0_DATASET + "]"
				+ "}";

		final OmeNgffMultiScaleMetadata ms = gson().fromJson(json, OmeNgffMultiScaleMetadata.class);
		assertNotNull("multiscales should deserialize", ms);

		final TransformGraph graph = ms.getGraph();

		assertEquals("only coordinate system is a node",
				setOf("physical"), nodeNames(graph));
		assertTrue("expected an identity self-path on physical",
				graph.path("physical", "physical").isPresent());
		assertFalse("no path to an undeclared coordinate system",
				graph.path("physical", "rotated").isPresent());
	}
	
	private static Set<String> setOf(String... strings) {
		return Arrays.stream(strings)
				.collect(Collectors.toSet());
	}

}

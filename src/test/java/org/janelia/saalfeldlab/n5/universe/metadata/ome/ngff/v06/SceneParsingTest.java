package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.MultiscalesAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffSceneParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffSceneMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.junit.Test;

import com.google.gson.GsonBuilder;

public class SceneParsingTest {

	private static final Set<String> coordSystemSet = Stream.of("CBCT/physical", "Dose/physical", "LET/physical", "world").collect(Collectors.toSet());

	private static final Set<String> worldSet = Stream.of("world").collect(Collectors.toSet());

	@Test
	public void testParseScene() {

		final String root = "src/test/resources/transforms/scene.ome.zarr";
		final String dset = "";

		try( final N5Reader zarr = new N5Factory()
				.options( opts -> { opts.gsonBuilder(gsonBuilder(true)); })
				.openReader(StorageFormat.ZARR3, root) ) {
			
			final NgffScene scene = zarr.getAttribute(dset, NgffScene.SCENE_KEY, NgffScene.class);
			assertNotNull(scene);

			final Set<String> parsedSet = Arrays.stream(scene.getCoordinateSystems())
						.map(CoordinateSystem::getName)
						.collect(Collectors.toSet());
			parsedSet.forEach( System.out::println );
			assertEquals(worldSet, parsedSet);

			final TransformGraph graph = scene.getGraph(zarr, dset);
			final Set<String> graphSet = graph.getCoordinateSystems().coordinateSystems()
					.map(CoordinateSystem::getName)
					.collect(Collectors.toSet());

			assertEquals(coordSystemSet, graphSet);
			assertEquals(6, graph.getTransforms().size());
		}
	}
	
	@Test
	public void testOmeNgffSceneParser() {

		final Set<String> pathSet = Stream.of("CBCT", "Dose", "LET").collect(Collectors.toSet());

		final String root = "src/test/resources/transforms/scene.ome.zarr";
		final String dset = "";

		try( final N5Reader zarr = new N5Factory()
				.options( opts -> { opts.gsonBuilder(gsonBuilder(true)); })
				.openReader(StorageFormat.ZARR3, root) ) {

			final OmeNgffSceneParser parser = new OmeNgffSceneParser(zarr);
			final Optional<NgffSceneMetadata> metaOpt = parser.parseMetadata(zarr, dset);
			assertTrue(metaOpt.isPresent());

			final NgffSceneMetadata meta = metaOpt.get();
			assertEquals(dset, meta.getPath());

			final NgffScene scene = meta.getScene();
			assertNotNull(scene);

			final Set<String> parsedPaths = Stream.of(scene.getPaths()).collect(Collectors.toSet());
			assertEquals(pathSet, parsedPaths);

			final Set<String> parsedCoordSystems = Arrays.stream(scene.getCoordinateSystems())
						.map(CoordinateSystem::getName)
						.collect(Collectors.toSet());
			assertEquals(worldSet, parsedCoordSystems);

			final TransformGraph graph = scene.getGraph(zarr, dset);
			final Set<String> graphSet = graph.getCoordinateSystems().coordinateSystems()
					.map(CoordinateSystem::getName)
					.collect(Collectors.toSet());

			assertEquals(coordSystemSet, graphSet);
			assertEquals(6, graph.getTransforms().size());
		}
	}

	@Test
	public void testGraphPathAcrossExternalDatasets() {

		final String root = "src/test/resources/transforms/scene.ome.zarr";
		final String dset = "";

		try( final N5Reader zarr = new N5Factory()
				.options( opts -> { opts.gsonBuilder(gsonBuilder(true)); })
				.openReader(StorageFormat.ZARR3, root) ) {

			final NgffScene scene = zarr.getAttribute(dset, NgffScene.SCENE_KEY, NgffScene.class);
			assertNotNull(scene);

			final TransformGraph graph = scene.getGraph(zarr, dset);

			// each dataset's local "physical" space should be connected to "world"
			// via the scene-level coordinate transformations, but the path-qualified
			// name used to register graph nodes ("CBCT/physical") does not match the
			// unqualified name used by the transform's OmeNgffReference ("physical"),
			// so no path is currently found.
			assertTrue("expected a path from CBCT/physical to world",
					graph.path("CBCT/physical", "world").isPresent());
			assertTrue("expected a path from Dose/physical to world",
					graph.path("Dose/physical", "world").isPresent());
			assertTrue("expected a path from LET/physical to world",
					graph.path("LET/physical", "world").isPresent());
		}
	}

	private static GsonBuilder gsonBuilder(boolean reverse) {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(reverse));
	}
}

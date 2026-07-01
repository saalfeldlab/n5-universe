package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06;

import static org.junit.Assert.assertArrayEquals;
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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformPath;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.junit.Test;

import com.google.gson.GsonBuilder;

import net.imglib2.realtransform.AffineTransform3D;

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

			// each dataset's local "physical" space is connected to "world" via
			// the scene-level coordinate transformations, using the path-qualified
			// node name ("CBCT/physical") to look it up in the graph -- this
			// requires OmeNgffReference.getQualifiedName() to be used consistently
			// in TransformGraph/CoordinateSystems (see TransformGraph.addTransform,
			// getInput/getOutput).
			assertTrue("expected a path from CBCT/physical to world",
					graph.path("CBCT/physical", "world").isPresent());
			assertTrue("expected a path from Dose/physical to world",
					graph.path("Dose/physical", "world").isPresent());
			assertTrue("expected a path from LET/physical to world",
					graph.path("LET/physical", "world").isPresent());
		}
	}

	/**
	 * Checks the actual {@link AffineTransform3D} produced by resolving a
	 * {@link TransformPath} through the graph against the values expected
	 * from the raw {@code coordinateTransformations} in
	 * {@code scene.ome.zarr}'s {@code zarr.json}:
	 * <pre>
	 * CBCT_to_world: translation [0, 0, 0]
	 * Dose_to_world: translation [4400, 0, 0]
	 * LET_to_world:  translation [4400, 0, 0]
	 * </pre>
	 * The JSON translation components are ordered to match the "world"/
	 * "physical" axes (z, y, x), but {@code gsonBuilder(true)} (reverse=true,
	 * as used elsewhere in this test class) reverses parsed arrays to the
	 * imglib2 (x, y, z) convention, so the 4400 ends up in the *last*
	 * component of the resulting {@link AffineTransform3D}'s translation.
	 */
	@Test
	public void testGraphTransformValues() {

		final String root = "src/test/resources/transforms/scene.ome.zarr";
		final String dset = "";

		try( final N5Reader zarr = new N5Factory()
				.options( opts -> { opts.gsonBuilder(gsonBuilder(true)); })
				.openReader(StorageFormat.ZARR3, root) ) {

			final NgffScene scene = zarr.getAttribute(dset, NgffScene.SCENE_KEY, NgffScene.class);
			assertNotNull(scene);

			final TransformGraph graph = scene.getGraph(zarr, dset);

			final AffineTransform3D identity = new AffineTransform3D();
			assertAffineEquals(identity,
					graph.path("CBCT/physical", "world").get().totalAffine3D(zarr));

			final AffineTransform3D doseToWorld = new AffineTransform3D();
			doseToWorld.translate(0, 0, 4400);
			assertAffineEquals(doseToWorld,
					graph.path("Dose/physical", "world").get().totalAffine3D(zarr));

			final AffineTransform3D letToWorld = new AffineTransform3D();
			letToWorld.translate(0, 0, 4400);
			assertAffineEquals(letToWorld,
					graph.path("LET/physical", "world").get().totalAffine3D(zarr));

			// the inverse edges added automatically by TransformGraph should
			// also resolve, with the inverse translation
			final AffineTransform3D worldToDose = new AffineTransform3D();
			worldToDose.translate(0, 0, -4400);
			assertAffineEquals(worldToDose,
					graph.path("world", "Dose/physical").get().totalAffine3D(zarr));
		}
	}

	private static void assertAffineEquals(final AffineTransform3D expected, final AffineTransform3D actual) {
		assertArrayEquals(expected.getRowPackedCopy(), actual.getRowPackedCopy(), 1e-9);
	}

	private static GsonBuilder gsonBuilder(boolean reverse) {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(reverse));
	}
}

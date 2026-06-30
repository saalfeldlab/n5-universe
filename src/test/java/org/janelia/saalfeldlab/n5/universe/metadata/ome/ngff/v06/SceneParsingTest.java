package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.junit.Test;

import com.google.gson.GsonBuilder;

public class SceneParsingTest {

	@Test
	public void testParseScene() {
		
		final Set<String> coordSystemSet = Stream.of("CBCT/physical", "Dose/physical", "LET/physical", "world").collect(Collectors.toSet());
		final Set<String> worldSet = Stream.of("world").collect(Collectors.toSet());

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
	
	private static GsonBuilder gsonBuilder(boolean reverse) {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(reverse));
	}
}

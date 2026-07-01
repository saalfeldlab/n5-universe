package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

/**
 * Represents a "scene" that represents multiple images or other objects in a
 * one or more common coordinate system via a set of coordinate transformations.
 */
public class NgffScene {
	
	public static final String SCENE_KEY = "ome/scene";

	private final CoordinateTransform[] coordinateTransformations;

	private final CoordinateSystem[] coordinateSystems;

	public NgffScene() {
		this(null, null);
	}

	public NgffScene(final CoordinateTransform[] coordinateTransformations) {
		this(coordinateTransformations, null);
	}

	public NgffScene(
			final CoordinateTransform[] coordinateTransformations,
			final CoordinateSystem[] coordinateSystems) {

		this.coordinateTransformations = coordinateTransformations;
		this.coordinateSystems = coordinateSystems;
	}

	public CoordinateTransform[] getCoordinateTransformations() {
		return coordinateTransformations;
	}

	public CoordinateSystem[] getCoordinateSystems() {
		return coordinateSystems;
	}
	
	/**
	 * Returns the distinct external paths referenced by coordinate transformations.
	 *
	 * @return the referenced paths
	 */
	public String[] getPaths() {

		if (coordinateTransformations == null)
			return new String[0];

		final LinkedHashSet<String> paths = new LinkedHashSet<>();
		for (final CoordinateTransform<?> ct : coordinateTransformations) {
			final OmeNgffReference input = ct.getInput();
			final OmeNgffReference output = ct.getOutput();
			if (isExternalPath(input)) paths.add(input.getPath());
			if (isExternalPath(output)) paths.add(output.getPath());
		}
		return paths.toArray(new String[0]);
	}

	/**
	 * Returns the name of the first coordinate system in this scene, or
	 * {@code null} if this scene has no coordinate systems.
	 *
	 * @return the default coordinate system name
	 */
	public String getDefaultCoordinateSystemName() {

		if (coordinateSystems == null || coordinateSystems.length == 0)
			return null;

		return coordinateSystems[0].getName();
	}

	public TransformGraph getGraph(N5Reader n5, String basePath) {

		final ArrayList<CoordinateSystem> allCs = new ArrayList<>();
		if (coordinateSystems != null)
			allCs.addAll(Arrays.asList(coordinateSystems));

		for (final String extPath : getPaths()) {
			final String resolvedPath = (basePath == null || basePath.isEmpty())
					? extPath : basePath + "/" + extPath;
			final List<CoordinateSystem> extCs = readCoordinateSystems(n5, resolvedPath);
			allCs.addAll(extCs);
		}

		return new TransformGraph(
				Arrays.asList(coordinateTransformations),
				new CoordinateSystems(allCs));
	}

	private static boolean isExternalPath(final OmeNgffReference ref) {
		if (ref == null) return false;
		final String p = ref.getPath();
		return p != null && !p.isEmpty() && !p.equals(".");
	}

	private static List<CoordinateSystem> readCoordinateSystems(final N5Reader n5, final String path) {
		
		final ArrayList<CoordinateSystem> result = new ArrayList<>();
		final OmeNgffMultiScaleMetadata[] mss = n5.getAttribute(path, "ome/multiscales", OmeNgffMultiScaleMetadata[].class);
		if (mss != null)
			for( OmeNgffMultiScaleMetadata ms : mss) {
				result.addAll(prependPath(ms.getCoordinateSystems(), path));
			}

		return result;
	}

	private static List<CoordinateSystem> prependPath(CoordinateSystem[] css, String path) {
		final ArrayList<CoordinateSystem> result = new ArrayList<>();
		for (final CoordinateSystem cs : css)
			result.add(new CoordinateSystem(path + "/" + cs.getName(), cs.getAxes()));
		return result;
	}

}

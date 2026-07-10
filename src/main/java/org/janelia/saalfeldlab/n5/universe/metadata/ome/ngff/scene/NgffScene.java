package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AbstractCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Represents a "scene" that represents multiple images or other objects in a
 * one or more common coordinate system via a set of coordinate transformations.
 */
public class NgffScene {
	
	public static final String SCENE_KEY = "ome/scene";

	private static final String MULTISCALES_KEY = "ome/multiscales";

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
	 * Returns the qualified names (see {@link OmeNgffReference#getQualifiedName()})
	 * of the coordinate system(s) that this scene's coordinate transformations
	 * associate with the given (scene-relative) dataset {@code path}.
	 *
	 * @param path
	 *            the (scene-relative) dataset path
	 * @return the local coordinate system names for {@code path}
	 */
	public List<String> localSpaceNames(final String path) {

		final List<String> names = new ArrayList<>();
		if (coordinateTransformations == null)
			return names;

		for (final CoordinateTransform<?> ct : coordinateTransformations) {
			if (path.equals(ct.getInput().getPath()))
				names.add(ct.getInput().getQualifiedName());
			if (path.equals(ct.getOutput().getPath()))
				names.add(ct.getOutput().getQualifiedName());
		}
		return names;
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

	/**
	 * Builds the graph relating this scene's coordinate systems to those of the
	 * datasets it references, reading each referenced dataset's
	 * {@code ome/multiscales}.
	 * <p>
	 * The referenced metadata is deserialized with this method's own {@link Gson}
	 * (see {@link OmeNgffMetadataParser#gsonBuilder()}), not the reader's, so the
	 * result does not depend on how the caller happened to construct {@code n5} --
	 * a reader without the ome-ngff type adapters cannot deserialize
	 * {@link CoordinateTransform}, which is an interface.
	 *
	 * @param n5
	 *            the reader
	 * @param basePath
	 *            the path this scene's dataset references are relative to (may be
	 *            null or empty)
	 * @return the graph
	 * @throws IllegalStateException
	 *             if a referenced dataset's {@code ome/multiscales} is missing or
	 *             unreadable. Failing here is deliberate: skipping such a dataset
	 *             would yield a graph that looks complete but silently lacks its
	 *             nodes, so every transformation into a coordinate system it
	 *             participates in would resolve to the identity.
	 */
	public TransformGraph getGraph(N5Reader n5, String basePath) {

		final Gson gson = new OmeNgffMetadataParser(n5).gsonBuilder().create();

		final ArrayList<CoordinateSystem> allCs = new ArrayList<>();
		if (coordinateSystems != null)
			allCs.addAll(Arrays.asList(coordinateSystems));

		final ArrayList<CoordinateTransform<?>> allCts = new ArrayList<>();
		if (coordinateTransformations != null)
			for (final CoordinateTransform<?> ct : coordinateTransformations)
				allCts.add(ct);

		for (final String extPath : getPaths()) {
			final String resolvedPath = (basePath == null || basePath.isEmpty())
					? extPath : basePath + "/" + extPath;

			for (final OmeNgffMultiScaleMetadata ms : readMultiscales(n5, resolvedPath, gson)) {

				final CoordinateSystem[] css = ms.getCoordinateSystems();
				if (css != null)
					allCs.addAll(prependPath(css, resolvedPath));

				final CoordinateTransform<?>[] cts = ms.getCoordinateTransformations();
				if (cts != null)
					for (final CoordinateTransform<?> ct : cts)
						allCts.add(qualify(ct, resolvedPath));
			}
		}

		return new TransformGraph(allCts, new CoordinateSystems(allCs));
	}

	private static boolean isExternalPath(final OmeNgffReference ref) {
		if (ref == null) return false;
		final String p = ref.getPath();
		return p != null && !p.isEmpty() && !p.equals(".");
	}

	/**
	 * Reads the {@code ome/multiscales} of a dataset this scene references.
	 *
	 * @param n5
	 *            the reader
	 * @param path
	 *            the (already basePath-resolved) external dataset path
	 * @param gson
	 *            deserializes the attribute; must have the ome-ngff type adapters
	 *            registered (the reader's own gson generally does not)
	 * @return the multiscales, never null or empty
	 * @throws IllegalStateException
	 *             if the attribute is absent or cannot be read
	 */
	private static OmeNgffMultiScaleMetadata[] readMultiscales(final N5Reader n5, final String path, final Gson gson) {

		final JsonElement el = n5.getAttribute(path, MULTISCALES_KEY, JsonElement.class);
		if (el == null)
			throw new IllegalStateException("scene references \"" + path + "\", which has no \""
					+ MULTISCALES_KEY + "\" attribute");

		final OmeNgffMultiScaleMetadata[] mss = gson.fromJson(el, OmeNgffMultiScaleMetadata[].class);
		if (mss == null || mss.length == 0)
			throw new IllegalStateException("scene references \"" + path + "\", whose \""
					+ MULTISCALES_KEY + "\" attribute could not be read");

		return mss;
	}

	private static List<CoordinateSystem> prependPath(CoordinateSystem[] css, String path) {
		final ArrayList<CoordinateSystem> result = new ArrayList<>();
		for (final CoordinateSystem cs : css)
			result.add(new CoordinateSystem(path + "/" + cs.getName(), cs.getAxes()));
		return result;
	}

	/**
	 * Rewrites a multiscale-level transform's input/output references as qualified
	 * names (see {@link OmeNgffReference#getQualifiedName()}), so they name the
	 * same nodes the graph registers for {@code path}.
	 */
	private static CoordinateTransform<?> qualify(final CoordinateTransform<?> ct, final String path) {

		if (!(ct instanceof AbstractCoordinateTransform))
			return ct;

		final AbstractCoordinateTransform<?> act = (AbstractCoordinateTransform<?>)ct;
		act.setNameSpaces(act.getName(), qualifyReference(act.getInput(), path), qualifyReference(act.getOutput(), path));
		return act;
	}

	private static String qualifyReference(final OmeNgffReference ref, final String path) {

		if (ref == null)
			return null;
		return isExternalPath(ref) ? ref.getQualifiedName() : path + "/" + ref.getName();
	}

}

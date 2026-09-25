package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffSceneMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.CoordinateTransformAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class OmeNgffSceneParser implements N5MetadataParser<NgffSceneMetadata>, N5MetadataWriter<NgffSceneMetadata> {

	/** the ome-zarr version that introduced scenes */
	public static final String VERSION = "0.6";

	public static final String VERSION_KEY = "ome/version";

	private final Gson gson;
	private final boolean reverse;

	public OmeNgffSceneParser(final boolean reverse) {
		this.reverse = reverse;
		this.gson = gsonBuilder().create();
	}

	public OmeNgffSceneParser(final N5Reader n5) {
		this(OmeNgffMetadataParser.reverse(n5));
	}

	public OmeNgffSceneParser() {
		this(true);
	}

	public GsonBuilder gsonBuilder() {
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter());
	}

	@Override
	public Optional<NgffSceneMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {
		try {
			final JsonElement sceneEl = n5.getAttribute(node.getPath(), NgffScene.SCENE_KEY, JsonElement.class);
			if (sceneEl == null)
				return Optional.empty();
			final NgffScene scene = gson.fromJson(sceneEl, NgffScene.class);
			if (scene == null)
				return Optional.empty();
			return Optional.of(new NgffSceneMetadata(node.getPath(), scene));
		} catch (final Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Writes the scene to {@link NgffScene#SCENE_KEY} at {@code path}, and the
	 * ome-zarr version to {@link #VERSION_KEY} if the group does not declare one yet.
	 * <p>
	 * The scene is serialized with this parser's {@link Gson}, as it is parsed, so the
	 * result does not depend on the type adapters {@code n5} was built with, and null
	 * fields (e.g. a scene without coordinate systems) are omitted rather than written
	 * as null.
	 */
	@Override
	public void writeMetadata(final NgffSceneMetadata t, final N5Writer n5, final String path) throws Exception {

		// don't replace a more specific version, e.g. 0.6.dev4, written with a multiscales here
		if (n5.getAttribute(path, VERSION_KEY, String.class) == null)
			n5.setAttribute(path, VERSION_KEY, VERSION);

		n5.setAttribute(path, NgffScene.SCENE_KEY, gson.toJsonTree(t.getScene()));
	}

}

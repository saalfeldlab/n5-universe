package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffScene;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene.NgffSceneMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class OmeNgffSceneParser implements N5MetadataParser<NgffSceneMetadata> {

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

}

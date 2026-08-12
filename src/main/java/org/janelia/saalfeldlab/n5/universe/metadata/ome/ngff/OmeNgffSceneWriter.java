package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
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

/**
 * Writes an {@link NgffScene} to the {@code ome/scene} attribute of a group,
 * the write-side companion to {@link OmeNgffSceneParser}.
 * <p>
 * When {@code reverse} is set (zarr storage), the coordinate systems' axes are
 * written in reversed (C-order) order, matching {@link OmeNgffSceneParser} and
 * the way the field/scene readers interpret zarr containers; transform
 * parameters are reversed by the reverse-aware {@link CoordinateTransformAdapter}.
 */
public class OmeNgffSceneWriter implements N5MetadataWriter<NgffSceneMetadata> {

	private final Gson gson;
	private final boolean reverse;

	public OmeNgffSceneWriter(final boolean reverse) {
		this.reverse = reverse;
		this.gson = gsonBuilder().create();
	}

	public OmeNgffSceneWriter(final N5Writer n5) {
		this(OmeNgffMetadataParser.reverse(n5));
	}

	public OmeNgffSceneWriter() {
		this(true);
	}

	public GsonBuilder gsonBuilder() {
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter());
	}

	/**
	 * Serializes the scene to a {@link JsonElement}, applying the zarr axis
	 * reversal to coordinate systems when {@code reverse} is set. The result is
	 * what would be stored under {@code ome/scene}; exposed so callers can merge
	 * with an existing scene at the JSON level.
	 *
	 * @param scene the scene
	 * @return the serialized scene
	 */
	public JsonElement toJson(final NgffScene scene) {

		return gson.toJsonTree(reverse ? reverseCoordinateSystems(scene) : scene);
	}

	public void write(final N5Writer n5, final String path, final NgffScene scene) {

		n5.setAttribute(path, NgffScene.SCENE_KEY, toJson(scene));
	}

	@Override
	public void writeMetadata(final NgffSceneMetadata t, final N5Writer n5, final String path) throws Exception {

		write(n5, path, t.getScene());
	}

	private static NgffScene reverseCoordinateSystems(final NgffScene scene) {

		final CoordinateSystem[] css = scene.getCoordinateSystems();
		if (css == null)
			return scene;

		final CoordinateSystem[] reversed = new CoordinateSystem[css.length];
		for (int i = 0; i < css.length; i++)
			reversed[i] = css[i].reverseAxes();

		return new NgffScene(scene.getCoordinateTransformations(), reversed);
	}

}

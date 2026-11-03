package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

import com.google.gson.JsonElement;

/**
 * Reads coordinate transforms from attributes so that they can be built with
 * {@link CoordinateTransform#getTransform()}.
 * <p>
 * Transforms read with a reader's own gson, e.g.
 * {@code n5.getAttribute(group, key, CoordinateTransform.class)}, are not bound to that reader,
 * because the gson does not know the reader or the group the attribute was read from. Transforms
 * read here are bound to {@code n5}, with relative parameter paths resolved against {@code group}.
 */
public class CoordinateTransforms {

	private CoordinateTransforms() {}

	/**
	 * Reads a single transform.
	 *
	 * @param n5 the reader
	 * @param group the group holding the attribute
	 * @param key the attribute key, e.g. {@code "coordinateTransformations[0]"}
	 * @return the transform, or null if the attribute does not exist
	 */
	public static CoordinateTransform<?> read(final N5Reader n5, final String group, final String key) {

		return read(n5, group, key, CoordinateTransform.class);
	}

	/**
	 * Reads an array of transforms.
	 *
	 * @param n5 the reader
	 * @param group the group holding the attribute
	 * @param key the attribute key, e.g. {@code "coordinateTransformations"}
	 * @return the transforms, or null if the attribute does not exist
	 */
	public static CoordinateTransform<?>[] readAll(final N5Reader n5, final String group, final String key) {

		return read(n5, group, key, CoordinateTransform[].class);
	}

	/**
	 * Reads an attribute holding transforms, e.g. a transform, an array of them, or an object with
	 * transforms as fields.
	 *
	 * @param <T> the type to read
	 * @param n5 the reader
	 * @param group the group holding the attribute
	 * @param key the attribute key
	 * @param type the type to read
	 * @return the attribute, or null if it does not exist
	 */
	public static <T> T read(final N5Reader n5, final String group, final String key, final Class<T> type) {

		final JsonElement json = n5.getAttribute(group, key, JsonElement.class);
		if (json == null)
			return null;

		return new OmeNgffMetadataParser(n5).gsonBuilder(n5, group).create().fromJson(json, type);
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Writes the members an {@link OmeNgffReference} actually carries, and nothing
 * else.
 * <p>
 * A reference names a coordinate system, an array by path, or an external
 * coordinate system by both. Which members are meaningful therefore varies, and
 * the constructors differ in how they spell an absent one: some leave it null,
 * {@link OmeNgffReference#OmeNgffReference(String)} and
 * {@link OmeNgffReference#DUMMY} use the empty string. Reflection keeps the
 * empty strings, which is how {@code "path": ""} ends up beside a by-name
 * reference. Treating empty and null alike here makes the output depend on what
 * the reference holds rather than on which constructor built it.
 * <p>
 * Deserialization is left to reflection, which reads these objects correctly.
 */
public class OmeNgffReferenceAdapter implements JsonSerializer<OmeNgffReference> {

	@Override
	public JsonElement serialize(final OmeNgffReference src, final Type typeOfSrc,
			final JsonSerializationContext context) {

		final boolean hasName = notEmpty(src.getName());
		final boolean hasPath = notEmpty(src.getPath());

		// A reference with neither member says nothing, so it should not appear at
		// all. Returning null rather than an empty object is what removes the key:
		// gson omits a member whose value is null unless serializeNulls is set, and
		// CoordinateTransformAdapter.FIELD_TO_NULL_CHECK drops it from trees built
		// by reflection.
		if (!hasName && !hasPath)
			return JsonNull.INSTANCE;

		final JsonObject obj = new JsonObject();

		if (hasName)
			obj.addProperty("name", src.getName());

		if (hasPath)
			obj.addProperty("path", src.getPath());

		return obj;
	}

	private static boolean notEmpty(final String s) {

		return s != null && !s.isEmpty();
	}
}

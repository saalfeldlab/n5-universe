package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AxisAdapter implements JsonSerializer<Axis>, JsonDeserializer<Axis> {

	@Override
	public Axis deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {

		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();

		// Require name to be non-null, but try to be tolerant of null type or unit
		if (!jobj.has("name"))
			return null;

		final String name = jobj.get("name").getAsString();
		final String type = jobj.has("type") ? nullSafeString(jobj.get("type")) : "";
		final String unit = jobj.has("unit") ? nullSafeString(jobj.get("unit")) : "";

		return new Axis(type, name, unit);
	}
	

	/**
	 * Returns a String from the json element or null if it is JsonNull.
	 * <p>
	 * Gson throws an UnsupportedOperationException when calling getAsString
	 * on JsonNull, this method returns null instead.
	 *
	 * @param json a json element
	 * @return a string, or null
	 */
	private String nullSafeString(JsonElement json) {

		if( json.isJsonNull())
			return null;
		else
			return json.getAsString();
	}

	@Override
	public JsonElement serialize(final Axis src, final Type typeOfSrc, final JsonSerializationContext context) {

		final JsonObject obj = new JsonObject();
		obj.addProperty("type", src.getType());
		obj.addProperty("name", src.getName());
		obj.addProperty("unit", src.getUnit());

		return obj;
	}
}

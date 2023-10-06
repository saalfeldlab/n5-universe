package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

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
		final String type = jobj.has("type") ? jobj.get("type").getAsString() : "";
		final String name = jobj.has("name") ? jobj.get("name").getAsString() : "";
		final String unit = jobj.has("unit") ? jobj.get("unit").getAsString() : "";

		return new Axis(type, name, unit);
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

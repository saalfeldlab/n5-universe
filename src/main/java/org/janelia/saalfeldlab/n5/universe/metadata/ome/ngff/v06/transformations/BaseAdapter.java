package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.lang.reflect.Type;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class BaseAdapter<T extends CoordinateTransform<?>> implements JsonDeserializer<T>, JsonSerializer<T> {

	@Override
	public T deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		return null;
	}

	@Override
	public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject obj = new JsonObject();
		obj.addProperty(CoordinateTransform.TYPE_KEY, src.getType());
		return obj;
	}

}

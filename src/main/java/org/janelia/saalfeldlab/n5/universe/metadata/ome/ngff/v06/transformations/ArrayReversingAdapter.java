package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.lang.reflect.Type;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class ArrayReversingAdapter<T extends CoordinateTransform<?>> implements JsonDeserializer<T>, JsonSerializer<T> {
	
	private String keyToFlip;

	protected ArrayReversingAdapter(final String keyToFlip) {
		this.keyToFlip = keyToFlip;
	}

	@Override
	public T deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		reverse(json.getAsJsonObject().get(keyToFlip).getAsJsonArray());
		return context.deserialize(json, typeOfT);
	}

	@Override
	public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject obj = CoordinateTransformAdapter.serializeGeneric(src).getAsJsonObject();
		reverse(obj.get(keyToFlip).getAsJsonArray());
		return obj;
	}

	/**
	 * Reverses the order of the elements of the given {@link JsonArray} in place.
	 * 
	 * @param array the jsono array
	 */
	private static void reverse(final JsonArray array) {

		JsonElement a;
		final int max = array.size() - 1;
		for (int i = (max - 1) / 2; i >= 0; --i) {
			final int j = max - i;
			a = array.get(i);
			array.set(i, array.get(j));
			array.set(j, a);
		}
	}

}

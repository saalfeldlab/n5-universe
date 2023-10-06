package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DatasetAdapter implements JsonSerializer<OmeNgffDataset> {

	@Override
	public JsonElement serialize(final OmeNgffDataset src, final Type typeOfSrc, final JsonSerializationContext context) {

		final JsonObject obj = new JsonObject();
		obj.addProperty("path", src.path);

		if (src.coordinateTransformations != null)
			if (src.coordinateTransformations.length == 0)
				obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty  array
			else
				obj.add("coordinateTransformations", context.serialize(src.coordinateTransformations));
		else
			obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty array

		return obj;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Ignores the coordinateTransformations, as those are not present in NGFF v0.3.
 */
public class DatasetAdapterV03 implements JsonSerializer<OmeNgffDataset> {

	@Override
	public JsonElement serialize(final OmeNgffDataset src, final Type typeOfSrc, final JsonSerializationContext context) {

		final JsonObject obj = new JsonObject();
		obj.addProperty("path", src.path);
		return obj;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MultiscalesAdapter implements JsonDeserializer< OmeNgffMultiScaleMetadata >, JsonSerializer< OmeNgffMultiScaleMetadata >
{
	@Override
	public OmeNgffMultiScaleMetadata deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		if (!jobj.has("axes") && !jobj.has("datasets"))
			return null;

		final String name = jobj.get("name").getAsString();
		final String type = jobj.get("type").getAsString();
		final String version = jobj.get("version").getAsString();

		final Axis[] axes = context.deserialize(jobj.get("axes"), Axis[].class);
		final OmeNgffDataset[] datasets = context.deserialize(jobj.get("datasets"), OmeNgffDataset[].class);
		final CoordinateTransformation<?>[] coordinateTransformations = context
				.deserialize(jobj.get("coordinateTransformations"), CoordinateTransformation[].class);
		final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
				OmeNgffDownsamplingMetadata.class);

		return new OmeNgffMultiScaleMetadata(axes.length, "", name, type, version, axes, datasets, null,
				coordinateTransformations, metadata);
	}

	@Override
	public JsonElement serialize( OmeNgffMultiScaleMetadata src, Type typeOfSrc, JsonSerializationContext context )
	{
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", src.name);
		obj.addProperty("type", src.type);
		obj.addProperty("version", src.version);
		obj.add("axes", context.serialize(src.axes));
		obj.add("datasets", context.serialize(src.datasets));

		if( src.coordinateTransformations != null )
			obj.add("coordinateTransformations", context.serialize(src.coordinateTransformations));

		if( src.metadata != null )
			obj.add("metadata", context.serialize(src.metadata));

		return obj;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import com.google.gson.*;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;

import java.lang.reflect.Type;

public class MultiscalesAdapter implements JsonDeserializer<OmeNgffMultiScaleMetadata>, JsonSerializer< OmeNgffMultiScaleMetadata >
{
	@Override
	public OmeNgffMultiScaleMetadata deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		if (!jobj.has("axes") && !jobj.has("datasets"))
			return null;

		// name and type may be null
		final String name = MetadataUtils.getStringNullable(jobj.get("name"));
		final String type = MetadataUtils.getStringNullable(jobj.get("type"));
		final String version = "0.5";

		final Axis[] axes = context.deserialize(jobj.get("axes"), Axis[].class);
		final OmeNgffDataset[] datasets = context.deserialize(jobj.get("datasets"), OmeNgffDataset[].class);
		final CoordinateTransformation<?>[] coordinateTransformations = context
				.deserialize(jobj.get("coordinateTransformations"), CoordinateTransformation[].class);
		final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
				OmeNgffDownsamplingMetadata.class);

		return new OmeNgffMultiScaleMetadata(axes.length, "", name, type, version, axes, datasets, null,
				coordinateTransformations, metadata, false);
	}

	@Override
	public JsonElement serialize( final OmeNgffMultiScaleMetadata src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", src.name);
		obj.addProperty("type", src.type);
		//TODO: add property version to attributes/ome/
		//obj.addProperty("version", src.version);
		obj.add("axes", context.serialize(src.axes));
		obj.add("datasets", context.serialize(src.datasets));

		if( src.coordinateTransformations != null )
			if( src.coordinateTransformations.length == 0 )
				obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty array
			else
				obj.add("coordinateTransformations", context.serialize(src.coordinateTransformations));
		else
			obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty array

		if( src.metadata != null )
			obj.add("metadata", context.serialize(src.metadata));

		return obj;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03.OmeNgffV03MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03.OmeNgffV03MultiScaleMetadata.OmeNgffV03Dataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffV04MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffV04MultiScaleMetadata.OmeNgffV04Dataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.OmeNgffV05MultiScaleMetadata;

import com.google.gson.JsonArray;
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

		final String version = jobj.get("version").getAsString();
		if (version.equals("0.5")) {
			
			final Axis[] axes = context.deserialize(jobj.get("axes"), Axis[].class);
			final Axis[] axesInReverseOrder = MetadataUtils.reversedCopy( axes );
			final OmeNgffV04Dataset[] datasets = context.deserialize(jobj.get("datasets"), OmeNgffV04Dataset[].class);
			final CoordinateTransformation<?>[] coordinateTransformations = context
					.deserialize(jobj.get("coordinateTransformations"), CoordinateTransformation[].class);
			final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
					OmeNgffDownsamplingMetadata.class);

			return new OmeNgffV05MultiScaleMetadata(axesInReverseOrder.length, "", 
					name, type, axesInReverseOrder, datasets, null,
					coordinateTransformations, metadata);

		}
		else if (version.equals("0.4")) {

			final Axis[] axes = context.deserialize(jobj.get("axes"), Axis[].class);
			final Axis[] axesInReverseOrder = MetadataUtils.reversedCopy( axes );
			final OmeNgffV04Dataset[] datasets = context.deserialize(jobj.get("datasets"), OmeNgffV04Dataset[].class);
			final CoordinateTransformation<?>[] coordinateTransformations = context
					.deserialize(jobj.get("coordinateTransformations"), CoordinateTransformation[].class);
			final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
					OmeNgffDownsamplingMetadata.class);

			return new OmeNgffV04MultiScaleMetadata(axesInReverseOrder.length, "", 
					name, type, axesInReverseOrder, datasets, null,
					coordinateTransformations, metadata);
		}
		else if (version.equals("0.3")) {

			final String[] axes = context.deserialize(jobj.get("axes"), String[].class);
			final String[] axesInReverseOrder = MetadataUtils.reversedCopy( axes );
			final int nd = axesInReverseOrder.length;
			OmeNgffV03Dataset[] datasets = context.deserialize(jobj.get("datasets"), OmeNgffV03Dataset[].class);
			final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
					OmeNgffDownsamplingMetadata.class);

			return new OmeNgffV03MultiScaleMetadata(nd, "", name, type, 
					axesInReverseOrder, datasets, null, metadata);
		}

		return null;
	}

	@Override
	public JsonElement serialize( final OmeNgffMultiScaleMetadata src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", src.name);
		obj.addProperty("type", src.type);
		obj.addProperty("version", src.version);
		obj.add("axes", context.serialize(src.axes));
		obj.add("datasets", context.serialize(src.getDatasets()));

		CoordinateTransformation<?>[] cts = src.getCoordinateTransformations();
		if( cts != null )
			if( cts.length == 0 )
				obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty array
			else
				obj.add("coordinateTransformations", context.serialize(cts));
		else
			obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty array

		if( src.metadata != null )
			obj.add("metadata", context.serialize(src.metadata));

		return obj;
	}

}

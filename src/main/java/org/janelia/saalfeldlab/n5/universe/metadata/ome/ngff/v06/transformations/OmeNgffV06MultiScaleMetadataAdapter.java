package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.SpacesTransforms;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.serialization.JsonArrayUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class OmeNgffV06MultiScaleMetadataAdapter implements JsonDeserializer< OmeNgffV06MultiScaleMetadata >, JsonSerializer< OmeNgffV06MultiScaleMetadata > {

	@Override
	public OmeNgffV06MultiScaleMetadata deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException {

		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		if (!jobj.has("axes") && !jobj.has("datasets"))
			return null;

		// name and type may be null
		final String name = MetadataUtils.getStringNullable(jobj.get("name"));
		final String type = MetadataUtils.getStringNullable(jobj.get("type"));

		JsonElement coordinateSystemsJson = jobj.get("coordinateSystems");
		if (!coordinateSystemsJson.isJsonArray())
			return null;

		final CoordinateSystem[] coordinateSystems = context.deserialize(coordinateSystemsJson, CoordinateSystem[].class);
		reverseCoordinateSystems(coordinateSystems);
		int nd = coordinateSystems[0].getAxes().length;

		final OmeNgffDataset[] datasets = context.deserialize(jobj.get("datasets"), OmeNgffDataset[].class);
		final CoordinateTransform<?>[] coordinateTransformations = context
				.deserialize(jobj.get("coordinateTransformations"), CoordinateTransform[].class);
		final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"), OmeNgffDownsamplingMetadata.class);

		SpacesTransforms.synchronize( coordinateTransformations, coordinateSystems );		
		for( OmeNgffDataset dataset : datasets )
			SpacesTransforms.synchronize( dataset.coordinateTransformations, coordinateSystems );

		return new OmeNgffV06MultiScaleMetadata(nd, "", name, type, coordinateSystems, datasets, null,
				coordinateTransformations, metadata, false);
	}

	@Override
	public JsonElement serialize( final OmeNgffV06MultiScaleMetadata src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", src.name);
		obj.addProperty("type", src.type);

		final JsonElement coordinateSystemsJson = context.serialize(src.coordinateSystems);
		reverseAxes(coordinateSystemsJson.getAsJsonArray());
		obj.add("coordinateSystems", coordinateSystemsJson);

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

	private void reverseCoordinateSystems(CoordinateSystem[] coordinateSystems) {
		Arrays.stream(coordinateSystems).forEach(cs -> { cs.reverseInPlace(); });
	}

	private void reverseArrayCoordinateSystems(CoordinateSystem[] coordinateSystems) {
		Arrays.stream(coordinateSystems).forEach(cs -> { cs.reverseInPlaceIfArray(); });
	}

	private void reverseAxes(final JsonArray coordinateSystems) {
		coordinateSystems.forEach(cs -> {
			JsonArrayUtils.reverse(cs.getAsJsonObject().get("axes").getAsJsonArray());
		});
	}

}

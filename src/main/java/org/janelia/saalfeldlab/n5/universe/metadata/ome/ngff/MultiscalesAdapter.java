package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;

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
	private boolean reverse;

	public MultiscalesAdapter() {

		this(true);
	}

	public MultiscalesAdapter(final boolean reverse) {

		setReverseParameters(reverse);
	}

	public void setReverseParameters(final boolean reverse) {

		this.reverse = reverse;
	}

	protected Axis[] deserializeAxes( final JsonObject jobj, final JsonDeserializationContext context ) throws JsonParseException
	{
		final JsonElement elem = jobj.get("axes");
		if( !elem.isJsonArray())
			return null;

		final JsonArray arr = elem.getAsJsonArray();
		if( arr.size() == 0 ) 
			return null;

		Axis[] axes;
		if (arr.get(0).isJsonPrimitive()) {
			// v0.3 uses string labels for axes
			axes = AxisUtils.defaultAxes(context.deserialize(jobj.get("axes"), String[].class));
		} else {
			// newer versions use structures
			axes = context.deserialize(jobj.get("axes"), Axis[].class);
		}

		return reverse ? MetadataUtils.reversedCopy( axes ) : axes;
	}
	
	protected OmeNgffDataset[] deserializeDatasets( final JsonObject jobj, final JsonDeserializationContext context ) throws JsonParseException
	{
		return context.deserialize(jobj.get("datasets"), OmeNgffDataset[].class);	
	}
	
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

		final String version;
		if( jobj.has("version"))
			version = jobj.get("version").getAsString();
		else
			version = "";

		final Axis[] axes = deserializeAxes(jobj, context);
		final int nd = axes.length;
		final OmeNgffDataset[] datasets = deserializeDatasets(jobj, context);

		final CoordinateTransformation<?>[] coordinateTransformations = context
				.deserialize(jobj.get("coordinateTransformations"), CoordinateTransformation[].class);

		final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
				OmeNgffDownsamplingMetadata.class);

		return new OmeNgffMultiScaleMetadata(nd, "", 
				name, type, version, axes, datasets,
				coordinateTransformations, null, metadata);
	}

	@Override
	public JsonElement serialize( final OmeNgffMultiScaleMetadata src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonObject obj = new JsonObject();
		obj.addProperty("name", src.name);
		obj.addProperty("type", src.type);

		/*
		 * We do not support writing to 0.3 or earlier.
		 * OME-Zarr v0.4 stores version in the multiscales.
		 * v0.5 puts the version under the "ome" key.
		 */
		if (src.version.equals("0.4"))
			obj.addProperty("version", src.version);

		JsonElement serializedAxes = context.serialize(src.axes);
		if (reverse) {
			serializedAxes = MetadataUtils.reversedCopy(serializedAxes.getAsJsonArray());
		}

		obj.add("axes", serializedAxes);
		obj.add("datasets", context.serialize(src.getDatasets()));

		CoordinateTransformation<?>[] cts = src.getCoordinateTransformations();
		if( cts != null )
			if( cts.length == 0 )
				obj.add("coordinateTransformations", context.serialize(new JsonArray())); // empty array
			else
				obj.add("coordinateTransformations", context.serialize(cts));

		if( src.metadata != null )
			obj.add("metadata", context.serialize(src.metadata));

		return obj;
	}

}

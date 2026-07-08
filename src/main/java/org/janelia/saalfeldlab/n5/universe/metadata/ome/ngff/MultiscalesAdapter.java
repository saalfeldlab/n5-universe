package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * When {@code reverse} is {@code true} axes are reversed wherever present. They
 * could be present in a top-level {@code "axes"} array, or inside a
 * {@code coordinateSystem}.
 */
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
		// axes may not be present directly under multiscales for OME-Zarr >= 0.6
		final JsonElement elem = jobj.get("axes");
		if( elem == null || !elem.isJsonArray())
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

		return reverseAxes(axes);
	}

	/**
	 * Deserializes the {@code coordinateSystems} field, if present.
	 *
	 * @param jobj the json object
	 * @param context the deserialization context
	 * @return the coordinate systems, or {@code null} if not present
	 */
	protected CoordinateSystem[] deserializeCoordinateSystems( final JsonObject jobj, final JsonDeserializationContext context ) throws JsonParseException
	{
		final CoordinateSystem[] coordinateSystems = context.deserialize(jobj.get("coordinateSystems"), CoordinateSystem[].class);
		if (coordinateSystems == null || !reverse)
			return coordinateSystems;

		return Arrays.stream(coordinateSystems)
				.map(CoordinateSystem::reverseAxes)
				.toArray(CoordinateSystem[]::new);
	}

	protected Axis[] reverseAxes( final Axis[] axes )
	{
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
		if (!jobj.has("axes") && !jobj.has("coordinateSystems") && !jobj.has("datasets"))
			return null;

		// name and type may be null
		final String name = MetadataUtils.getStringNullable(jobj.get("name"));
		final String type = MetadataUtils.getStringNullable(jobj.get("type"));

		final String version;
		if( jobj.has("version"))
			version = jobj.get("version").getAsString();
		else
			version = "";

		final Axis[] declaredAxes = deserializeAxes(jobj, context);
		final CoordinateSystem[] coordinateSystems = deserializeCoordinateSystems(jobj, context);

		// axes may not be declared directly (OME-Zarr >= 0.5) -- fall back to
		// the first coordinateSystem's (already-reversed) axes
		final Axis[] axes;
		if (declaredAxes != null)
			axes = declaredAxes;
		else if (coordinateSystems != null && coordinateSystems.length > 0)
			axes = coordinateSystems[0].getAxes();
		else
			axes = null;

		final int nd = axes != null ? axes.length : 0;

		final OmeNgffDataset[] datasets = deserializeDatasets(jobj, context);

		final CoordinateTransform<?>[] coordinateTransformations = context
				.deserialize(jobj.get("coordinateTransformations"), CoordinateTransform[].class);

		final OmeNgffDownsamplingMetadata metadata = context.deserialize(jobj.get("metadata"),
				OmeNgffDownsamplingMetadata.class);

		return new OmeNgffMultiScaleMetadata(nd, "",
				name, type, version, axes, datasets,
				coordinateSystems, coordinateTransformations, null, metadata);
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

		CoordinateTransform<?>[] cts = src.getCoordinateTransformations();
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

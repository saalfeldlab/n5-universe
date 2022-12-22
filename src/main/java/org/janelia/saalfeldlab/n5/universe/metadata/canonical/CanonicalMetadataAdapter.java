package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Optional;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.ColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.IntColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.RGBAColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata.IntensityLimits;

public class CanonicalMetadataAdapter implements JsonDeserializer< CanonicalMetadata > {

	@Override
	public CanonicalMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		final JsonObject jsonObj = json.getAsJsonObject();
		final String path = jsonObj.get("path").getAsString();
		final Optional<DatasetAttributes> attrs = AbstractMetadataTemplateParser.datasetAttributes(context, json);

		SpatialMetadataCanonical spatial = null;
		if (jsonObj.has("spatialTransform")) {
			spatial = context.deserialize(jsonObj.get("spatialTransform"), SpatialMetadataCanonical.class);
		}

		MultiResolutionSpatialMetadataCanonical multiscale = null;
		if (jsonObj.has("multiscales")) {
			multiscale = context.deserialize(jsonObj.get("multiscales"), MultiResolutionSpatialMetadataCanonical.class);
		}

		MultiChannelMetadataCanonical multichannel = null;
		if (jsonObj.has("multichannel")) {
			multichannel = context.deserialize(jsonObj.get("multichannel"), MultiChannelMetadataCanonical.class);
		}

		IntensityLimits intensityLimits = null;
		if (jsonObj.has("intensityLimits")) {
			intensityLimits = context.deserialize(jsonObj.get("intensityLimits"), IntensityLimits.class);
		}

		ColorMetadata color = null;
		if (jsonObj.has("color")) {
			JsonObject colorObj = jsonObj.get("color").getAsJsonObject();
			if( colorObj.has("rgba"))
				color = context.deserialize(colorObj, IntColorMetadata.class);
			else
				color = context.deserialize(colorObj, RGBAColorMetadata.class);
		}

		if( spatial == null && multichannel == null && multiscale == null &&
				color == null && intensityLimits == null ) {
			return null;
		}

		if (attrs.isPresent()) {
			if( spatial != null )
				return new CanonicalSpatialDatasetMetadata(path, spatial, attrs.get(), intensityLimits, color );
			else 
				return new CanonicalDatasetMetadata(path, attrs.get(), intensityLimits, color );
		} else if( spatial != null ) {
			return new CanonicalSpatialMetadata( path, spatial, intensityLimits );
		} else if (multiscale != null && multiscale.getChildrenMetadata() != null) {
			return new CanonicalMultiscaleMetadata(path, multiscale );
		} else if (multichannel != null && multichannel.getPaths() != null) {
			return new CanonicalMultichannelMetadata(path, multichannel );
		} else {
			// if lots of things are present
			return null;
		}
	}

}

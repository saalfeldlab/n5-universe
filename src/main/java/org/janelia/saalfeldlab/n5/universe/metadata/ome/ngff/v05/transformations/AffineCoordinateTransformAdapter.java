package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class AffineCoordinateTransformAdapter implements JsonSerializer< AffineCoordinateTransform >, JsonDeserializer< AffineCoordinateTransform > {

	@Override
	public JsonElement serialize(AffineCoordinateTransform src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject json = CoordinateTransformAdapter.serializeGeneric(src).getAsJsonObject();
		final double[][] mtx = TransformUtils.affineToMatrix(src.getTransform());
		final double[][] mtxCOrder = TransformUtils.reverseCoordinates(mtx);
		json.add(AffineCoordinateTransform.TYPE, context.serialize(mtxCOrder));
		return json;
	}

	@Override
	public AffineCoordinateTransform deserialize(final JsonElement json, final Type typeOfT,
			final JsonDeserializationContext context) throws JsonParseException {

		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		final AffineCoordinateTransform tf = context.deserialize(jobj, AffineCoordinateTransform.class);
		tf.buildTransform();
		return tf;
	}

}

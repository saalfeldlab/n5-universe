package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class RotationCoordinateTransformAdapter implements JsonSerializer< RotationCoordinateTransform >, JsonDeserializer< RotationCoordinateTransform > {

	@Override
	public JsonElement serialize(RotationCoordinateTransform src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject json = CoordinateTransformAdapter.serializeGeneric(context, src).getAsJsonObject();
		final double[][] mtx = TransformUtils.affineToRotation(src.getTransform());
		final double[][] mtxCOrder = TransformUtils.reverseCoordinatesRotation(mtx);
		json.add(RotationCoordinateTransform.TYPE, context.serialize(mtxCOrder));
		return json;
	}

	@Override
	public RotationCoordinateTransform deserialize(final JsonElement json, final Type typeOfT,
			final JsonDeserializationContext context) throws JsonParseException {
		
		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		final RotationCoordinateTransform tf = context.deserialize(jobj, RotationCoordinateTransform.class);
		if (tf.rotation != null) {
			final double[][] mtxCOrder = MetadataUtils.toMatrix(tf.rotation);
			final double[][] mtxFOrder = TransformUtils.reverseCoordinatesRotation(mtxCOrder);
			tf.buildTransform(mtxFOrder);
		} else {
			tf.buildTransform();
		}
		return tf;
	}

}

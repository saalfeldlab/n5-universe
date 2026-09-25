package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class RotationCoordinateTransformAdapter implements JsonSerializer< RotationCoordinateTransform >, JsonDeserializer< RotationCoordinateTransform > {

	private boolean reverse; 

	public RotationCoordinateTransformAdapter() {
		this(true);
	}

	public RotationCoordinateTransformAdapter(final boolean reverse) {
		this.reverse = reverse;
	}

	@Override
	public JsonElement serialize(RotationCoordinateTransform src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject json = CoordinateTransformAdapter.serializeGeneric(context, src).getAsJsonObject();
		final double[][] mtx = TransformUtils.affineToRotation(src.getTransform());
		final double[][] mtxOut = reverse ? TransformUtils.reverseCoordinates(mtx) : mtx;
		json.add(RotationCoordinateTransform.TYPE, context.serialize(mtxOut));
		return json;
	}

	@Override
	public RotationCoordinateTransform deserialize(final JsonElement json, final Type typeOfT,
			final JsonDeserializationContext context) throws JsonParseException {
		
//		if (!json.isJsonObject())
//			return null;
//
//		final JsonObject jobj = json.getAsJsonObject();
//		final RotationCoordinateTransform tf = context.deserialize(jobj, RotationCoordinateTransform.class);
//		if (tf.rotation != null) {
//			final double[][] mtxCOrder = MetadataUtils.toMatrix(tf.rotation);
//			final double[][] mtxFOrder = TransformUtils.reverseCoordinatesRotation(mtxCOrder);
//			tf.buildTransform(mtxFOrder);
//		} else {
//			tf.buildTransform();
//		}
//		return tf;

		final JsonObject jobj = json.getAsJsonObject();
		final double[][] rotationMtxStored = context.deserialize(
				jobj.get(RotationCoordinateTransform.TYPE), double[][].class);


		double[][] rotation = null;
		final RotationCoordinateTransform base = context.deserialize(jobj, RotationCoordinateTransform.class);
		if( rotationMtxStored != null ) {
			rotation = reverse ? TransformUtils.reverseCoordinates(rotationMtxStored) : rotationMtxStored;
			final RotationCoordinateTransform tf;
			tf = new RotationCoordinateTransform(base.getName(), base.getInput(), base.getOutput(), rotation);
			tf.setInputAxes(base.getInputAxes());
			tf.setOutputAxes(base.getOutputAxes());
			return tf;
		} else {

			if( !jobj.has("path"))
				return null;
			
//			base.buildTransform();

			return base;
		}

	}

}

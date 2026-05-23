package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Collections;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class AffineCoordinateTransformAdapter implements JsonSerializer< AbstractAffineCoordinateTransform >, JsonDeserializer< AbstractAffineCoordinateTransform > {

	@Override
	public JsonElement serialize(AbstractAffineCoordinateTransform src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject json = CoordinateTransformAdapter.serializeGeneric(src).getAsJsonObject();
		final double[][] mtx = src.affine;
		final double[][] mtxCOrder = TransformUtils.reverseCoordinates(mtx);
		json.add(AbstractAffineCoordinateTransform.TYPE, context.serialize(mtxCOrder));
		return json;
	}

	@Override
	public AbstractAffineCoordinateTransform deserialize(final JsonElement json, final Type typeOfT,
			final JsonDeserializationContext context) throws JsonParseException {

		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		final double[][] affineCOrder = context.deserialize(
				jobj.get(AbstractAffineCoordinateTransform.TYPE), double[][].class);

		double[][] affine = null;
		if( affineCOrder != null )
			affine = TransformUtils.reverseCoordinates(affineCOrder);

		final GeneralAffineCoordinateTransform base = context.deserialize(jobj, GeneralAffineCoordinateTransform.class);
		final AbstractAffineCoordinateTransform tf;
		if (affine != null && sourceDimsEqualTargetDims(affine)) {
			tf = new InvertibleAffineCoordinateTransform(base.getName(), base.getInput(), base.getOutput(), affine);
			tf.setInputAxes(base.getInputAxes());
			tf.setOutputAxes(base.getOutputAxes());
		} else {
			tf = new GeneralAffineCoordinateTransform(base.getName(), base.getInput(), base.getOutput(), affine);
			tf.setInputAxes(base.getInputAxes());
			tf.setOutputAxes(base.getOutputAxes());
		}

		tf.buildTransform();
		return tf;
	}
	
	private static void reverseJsonMatrix( JsonArray arr ) { 
		
		// reverse columns
		Collections.reverse(arr.asList());

		// reverse rows
		arr.asList().forEach( row -> {
			Collections.reverse(row.getAsJsonArray().asList());
		});
		
	}

	private static boolean sourceDimsEqualTargetDims(final double[][] affine) {
		// rows = numTarget, cols = numSource + 1; square iff numTarget == numSource
		return affine != null && affine.length > 0 && affine.length == affine[0].length - 1;
	}


}

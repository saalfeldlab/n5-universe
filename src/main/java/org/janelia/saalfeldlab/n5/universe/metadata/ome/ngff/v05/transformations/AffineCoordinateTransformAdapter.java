/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

		final JsonObject json = CoordinateTransformAdapter.serializeGeneric(context, src).getAsJsonObject();
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
		if( affineCOrder != null ) { 
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
			return tf;
		} else {

			if( !jobj.has("path"))
				return null;

			GeneralAffineCoordinateTransform tf = context.deserialize(jobj, GeneralAffineCoordinateTransform.class);
			tf.buildTransform();
			return tf;
		}
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

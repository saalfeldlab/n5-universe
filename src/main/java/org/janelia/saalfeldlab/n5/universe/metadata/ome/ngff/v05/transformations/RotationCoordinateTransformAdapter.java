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

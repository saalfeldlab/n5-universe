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
import java.lang.reflect.Type;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;

public class LegacyAffineCoordinateTransformAdapter implements JsonDeserializer< AffineCoordinateTransform >
{

	@Override
	public AffineCoordinateTransform deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		if( !json.isJsonObject())
			return null;
		
		final JsonObject jobj = json.getAsJsonObject();
		final BaseLinearCoordinateTransform tmp = context.deserialize(jobj, BaseLinearCoordinateTransform.class);

		final AffineCoordinateTransform out = new AffineCoordinateTransform(tmp);

		final JsonElement elem = out.getJsonParameter();

		if( !elem.isJsonArray() )
			return null;

		final JsonArray arr = elem.getAsJsonArray();
		if( arr.size() == 0 )
			return null;

		double[] flat = null;
		double[][] nested = null;

		final JsonElement e0 = arr.get(0);
		if (e0.isJsonPrimitive()) {
			flat = new double[ arr.size()];
			for( int i = 0; i < arr.size(); i++ )
				flat[i] = arr.get(i).getAsDouble();

		} else if (e0.isJsonArray()) {

			for (int row = 0; row < arr.size(); row++) {
				
				final JsonArray jsonRowArray = arr.get(row).getAsJsonArray();
				if( row == 0 )
					nested = new double[ arr.size()][jsonRowArray.size()];

				for (int col = 0; col < jsonRowArray.size(); col++)
					nested[row][col] = jsonRowArray.get(col).getAsDouble();
			}

		} else {
			return null;
		}
		
		if( flat != null )
		{
			out.affineFlat = flat;
			out.buildTransform(flat);
			return out;
		}
		else if( nested != null )
		{
			out.affineFlat = TransformUtils.flatten(nested);
			out.buildTransform(out.affineFlat);
			return out;
		}

		return null;
	}
}

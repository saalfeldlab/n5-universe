package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class AffineCoordinateTransformAdapter implements JsonDeserializer< AffineCoordinateTransform >
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
		System.out.println( elem );
		System.out.println( tmp.getJsonParameter() );

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
			System.out.println( "flat: " + Arrays.toString(out.affineFlat));
			out.buildTransform(flat);
			return out;
		}
		else if( nested != null )
		{
			out.affineFlat = TransformUtils.flatten(nested);
			System.out.println( "nested 2 flat: " + Arrays.toString(out.affineFlat));
			out.buildTransform(out.affineFlat);
			return out;
		}

		System.out.println( "return null" );
		return null;
	}
}

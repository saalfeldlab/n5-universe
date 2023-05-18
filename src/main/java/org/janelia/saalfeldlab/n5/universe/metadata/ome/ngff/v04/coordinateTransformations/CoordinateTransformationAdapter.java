package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CoordinateTransformationAdapter implements JsonDeserializer< CoordinateTransformation<?> >, JsonSerializer< CoordinateTransformation<?> >
{
	public static final String[] FIELDS_TO_NULL_CHECK = new String[] { "path", "name" };

	@Override
	public CoordinateTransformation<?> deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		if ( !json.isJsonObject() )
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		if ( !jobj.has( "type" ) )
			return null;

		Class<?> clz;
		AbstractCoordinateTransformation< ? > out = null;
		switch ( jobj.get( "type" ).getAsString() )
		{
		case ( IdentityCoordinateTransformation.TYPE ):
			clz = IdentityCoordinateTransformation.class;
			break;
		case ( ScaleCoordinateTransformation.TYPE ):
			out = context.deserialize( jobj, ScaleCoordinateTransformation.class );
			break;
		case ( TranslationCoordinateTransformation.TYPE ):
			out = context.deserialize( jobj, TranslationCoordinateTransformation.class );
			break;
		default:
			return null;
		}

		return out;
	}

	@Override
	public JsonElement serialize( CoordinateTransformation<?> src, Type typeOfSrc, JsonSerializationContext context )
	{
		// why do i have to do this!?
		final JsonElement elem = context.serialize( src );
		if ( elem instanceof JsonObject )
		{
			final JsonObject obj = ( JsonObject ) elem;
			for ( String f : FIELDS_TO_NULL_CHECK )
				if ( obj.has( f ) && obj.get( f ).isJsonNull() )
				{
					obj.remove( f );
				}
		}
		return elem;
	}

	// singleton
	public static HashMap< String, Class< ? > > typesToClasses;

	public static HashMap< String, Class< ? > > getTypesToClasses()
	{
		if ( typesToClasses == null )
		{
			typesToClasses = new HashMap<>();
			typesToClasses.put( IdentityCoordinateTransformation.TYPE, IdentityCoordinateTransformation.class );
			typesToClasses.put( ScaleCoordinateTransformation.TYPE, ScaleCoordinateTransformation.class );
			typesToClasses.put( TranslationCoordinateTransformation.TYPE, TranslationCoordinateTransformation.class );
		}
		return typesToClasses;
	}

}

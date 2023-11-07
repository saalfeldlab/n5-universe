package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CoordinateTransformationAdapter implements JsonDeserializer< CoordinateTransformation<?> >, JsonSerializer< CoordinateTransformation<?> > {

	public static final String[] FIELDS_TO_NULL_CHECK = new String[] { "path", "name" };

//	private boolean reverse = false;
//
//	public CoordinateTransformationAdapter(final boolean reverse) {
//
//		this.reverse = reverse;
//	}

	@Override
	public CoordinateTransformation<?> deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		if ( !json.isJsonObject() )
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		if ( !jobj.has( "type" ) )
			return null;

		// always reverse parameters
		reverseParameters(jobj);

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
	public JsonElement serialize( final CoordinateTransformation<?> src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		// why do i have to do this!?
		final JsonElement elem = context.serialize( src );
		if ( elem instanceof JsonObject )
		{
			final JsonObject obj = ( JsonObject ) elem;
			for ( final String f : FIELDS_TO_NULL_CHECK )
				if ( obj.has( f ) && obj.get( f ).isJsonNull() )
				{
					obj.remove( f );
				}
		}

		// always reverse parameters
		reverseParameters( elem.getAsJsonObject() );

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

	public static void reverseParameters(final JsonObject obj) {

		if (obj.has("type")) {

			final String type = obj.get("type").getAsString();
			switch (type) {
			case ScaleCoordinateTransformation.TYPE:
				Collections.reverse(obj.get("scale").getAsJsonArray().asList());
				break;
			case TranslationCoordinateTransformation.TYPE:
				Collections.reverse(obj.get("translation").getAsJsonArray().asList());
				break;
			}
		}
	}

	public static void reverseParameters(final CoordinateTransformation<?> ct) {

		final String type = ct.getType();
		switch (type) {
		case ScaleCoordinateTransformation.TYPE:
			ArrayUtils.reverse( ((ScaleCoordinateTransformation) ct).getScale());
			break;
		case TranslationCoordinateTransformation.TYPE:
			ArrayUtils.reverse( ((TranslationCoordinateTransformation) ct).getTranslation());
			break;
		}
	}

}

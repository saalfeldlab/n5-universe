package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.N5Reader;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CoordinateTransformAdapter
	implements JsonDeserializer<CoordinateTransform<?>>, JsonSerializer<CoordinateTransform<?>> {
//	implements JsonDeserializer<LinearSpatialTransform> {

	final N5Reader n5;

	public static final String[] FIELD_TO_NULL_CHECK = new String[]{
		"path", "name",
		"input_axes", "output_axes", "output_space", "input_space"
	};

	public CoordinateTransformAdapter( final N5Reader n5 ) {
		this.n5 = n5;
	}

	@Override
	public CoordinateTransform<?> deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {

		if( !json.isJsonObject() )
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		if( !jobj.has("type") )
			return null;

		CoordinateTransform<?> out = null;
		switch( jobj.get("type").getAsString() )
		{
		case("identity"):
			out = context.deserialize( jobj, IdentityCoordinateTransform.class );
			break;
		case("scale"):
			out = context.deserialize( jobj, ScaleCoordinateTransform.class );
			break;
		case("translation"):
			out = context.deserialize( jobj, TranslationCoordinateTransform.class );
			break;
		case("scale_offset"):
			out = context.deserialize( jobj, ScaleOffsetCoordinateTransform.class );
			break;
		case("affine"):
			out = context.deserialize( jobj, AffineCoordinateTransform.class );
			break;
		case("matrix"):
			out = context.deserialize( jobj, MatrixCoordinateTransform.class );
			break;
		case("displacement_field"):
			out = context.deserialize( jobj, DisplacementFieldCoordinateTransform.class );
			break;
		case("position_field"):
			out = context.deserialize( jobj, PositionFieldCoordinateTransform.class );
			break;
		case("bijection"):

			final JsonObject btmp = context.deserialize( jobj, JsonObject.class );
			final IdentityCoordinateTransform bid = context.deserialize( btmp, IdentityCoordinateTransform.class );
			if( !btmp.has("forward") && !btmp.has("inverse")) {
				out = null;
			}
			else {
				final RealCoordinateTransform<?> fwd = context.deserialize( btmp.get("forward"), CoordinateTransform.class );
				final RealCoordinateTransform<?> inv = context.deserialize( btmp.get("inverse"), CoordinateTransform.class );
				out = new BijectionCoordinateTransform(bid.getName(), bid.getInput(), bid.getOutput(), fwd, inv );
			}
			break;
		case("sequence"):
			// don't like that this is necessary
			// in the future, look into RuntimeTypeAdapterFactory in gson-extras
			// when it is more officially maintained

			final IdentityCoordinateTransform id = context.deserialize( jobj, IdentityCoordinateTransform.class );
			if( jobj.has("transformations"))
			{
				final JsonArray ja = jobj.get("transformations").getAsJsonArray();
				final RealCoordinateTransform[] transforms = new RealCoordinateTransform[ ja.size() ];
				for( int i=0; i < ja.size(); i++) {
					final JsonElement e = ja.get(i).getAsJsonObject();
					transforms[i] = context.deserialize( e, CoordinateTransform.class );
				}
				out = new SequenceCoordinateTransform(id.getName(), id.getInput(), id.getOutput(), transforms);
//				out = seq;
			}
			else {
				out = null;
			}

			break;
		case("stacked"):
			final IdentityCoordinateTransform sid = context.deserialize( jobj, IdentityCoordinateTransform.class );
			final RealCoordinateTransform[] transforms = parseTransformList( jobj, "transformations", context );
			out = new StackedCoordinateTransform(sid.getName(), sid.getInput(), sid.getOutput(), Arrays.asList( transforms ));
			break;
		}
		/*
		 * Not necessary, since parsers or consuming code are responsible for calling
		 * getParameters and buildTransform
		 */
		//readTransformParameters(out);

		return out;
	}

	private final RealCoordinateTransform[] parseTransformList( final JsonObject elem, final String key, final JsonDeserializationContext context ) {
		final JsonArray ja = elem.get(key).getAsJsonArray();
		final RealCoordinateTransform[] transforms = new RealCoordinateTransform[ja.size()];
		for (int i = 0; i < ja.size(); i++) {
			final JsonElement e = ja.get(i).getAsJsonObject();
			transforms[i] = context.deserialize(e, CoordinateTransform.class);
		}
		return transforms;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final CoordinateTransform readTransformParameters( final CoordinateTransform transform ) {

		if( transform instanceof ParametrizedTransform ) {
			final ParametrizedTransform pt = (ParametrizedTransform)transform;
			if( pt.getParameterPath() != null ) {
				pt.buildTransform( pt.getParameters(n5));
			}
		}
		return transform;
	}

	@Override
	public JsonElement serialize(final CoordinateTransform<?> src, final Type typeOfSrc, final JsonSerializationContext context) {

		// why do i have to do this!?
		final JsonElement elem;
		if( src instanceof SequenceCoordinateTransform )
		{
			final SequenceCoordinateTransform seq = (SequenceCoordinateTransform)src;
			final JsonArray xfms = new JsonArray();
			for( final CoordinateTransform<?> t : seq.getTransformations() ) {
				final Type type = TypeToken.of(t.getClass()).getType();
				xfms.add(serialize(t, type, context ));
			}
			final JsonObject obj =  (JsonObject) context.serialize(src);
			obj.add("transformations", xfms);
			elem = obj;
		}
		else if( src instanceof BijectionCoordinateTransform )
		{
			final BijectionCoordinateTransform bct = (BijectionCoordinateTransform)src;
			final JsonObject obj =  (JsonObject) context.serialize(src);

			final RealCoordinateTransform<?> fwd = bct.getForward();
			final Type ftype = TypeToken.of(fwd.getClass()).getType();
			obj.add("forward", serialize( fwd, ftype, context ));

			final RealCoordinateTransform<?> inv = bct.getInverse();
			final Type itype = TypeToken.of(inv.getClass()).getType();
			obj.add("inverse", serialize( inv, itype, context ));

			elem = obj;
		}
		else
		{
			elem =  context.serialize(src);
		}

		if( elem instanceof JsonObject )
		{
			final JsonObject obj = (JsonObject)elem;
			for( final String f : FIELD_TO_NULL_CHECK )
			if( obj.has(f) && obj.get(f).isJsonNull())
			{
				obj.remove(f);
			}
		}
		return elem;
	}

	public static void test1()
	{

//		final String affineString = "{"
//				+ "\"type\": \"affine\","
//				+ "\"affine\" : [ 11.0, 0.0, 0.1, 0.0, 12.0, 0.2 ]"
//				+ "}";
//
//		final String scaleString = "{"
//				+ "\"type\": \"scale\","
//				+ "\"scale\" : [ 11.0, -8.0 ]"
//				+ "}";
//
//		final String translationString = "{"
//				+ "\"type\": \"translation\","
//				+ "\"translation\" : [ -0.9, 2.1 ]"
//				+ "}";
//
//		final String idString = "{"
//				+ "\"type\": \"identity\""
//				+ "}";
//
//		final String seqString = "{"
//				+ "\"type\": \"sequence\","
//				+ "\"transformations\": ["
//				+ scaleString + "," + translationString
//				+ "]}";
//
//		final CoordinateTransformAdapter adapter = new CoordinateTransformAdapter( null );
//
//		final GsonBuilder gsonBuilder = new GsonBuilder();
////		gsonBuilder.registerTypeHierarchyAdapter(SpatialTransform.class, adapter );
//		gsonBuilder.registerTypeAdapter( CoordinateTransform.class, adapter );
//		gsonBuilder.disableHtmlEscaping();
//
//		final Gson gson = gsonBuilder.create();
//
//		CoordinateTransform parsedAffine = gson.fromJson(affineString, CoordinateTransform.class);
//		System.out.println( affineString );
//		System.out.println( parsedAffine );
//		System.out.println( gson.toJson( parsedAffine ));
//		System.out.println( " " );
//
//		CoordinateTransform parsedScale = gson.fromJson(scaleString, CoordinateTransform.class);
//		System.out.println( scaleString );
//		System.out.println( parsedScale );
//		System.out.println( gson.toJson( parsedScale));
//		System.out.println( " " );
//
//		CoordinateTransform parsedTranslation = gson.fromJson(translationString, CoordinateTransform.class);
//		System.out.println( translationString );
//		System.out.println( parsedTranslation );
//		System.out.println( gson.toJson( parsedTranslation));
//		System.out.println( " " );
//
//		CoordinateTransform parsedId = gson.fromJson(idString, CoordinateTransform.class);
//		System.out.println( idString );
//		System.out.println( parsedId );
//		System.out.println( gson.toJson( parsedId ));
//		System.out.println( " " );


//		CoordinateTransform parsedSeq = gson.fromJson(seqString, CoordinateTransform.class);
//		System.out.println( seqString );
//		System.out.println( parsedSeq );
//		System.out.println( gson.toJson( parsedSeq ));
//		System.out.println( " " );

//		ScaleCoordinateTransform s = new ScaleCoordinateTransform( new double[] {1, 2 });
//		TranslationCoordinateTransform t = new TranslationCoordinateTransform( new double[] {3, 4 });
//		SequenceCoordinateTransform seq = new SequenceCoordinateTransform( new RealCoordinateTransform[] { s, t }, "", "" );
//
//		System.out.println( gson.toJson(seq) );

	}

	public static void seqTest()
	{
		final String scaleString = "{"
				+ "\"type\": \"scale\","
				+ "\"scale\" : [ 11.0, -8.0 ]"
				+ "}";

		final String translationString = "{"
				+ "\"type\": \"translation\","
				+ "\"translation\" : [ -0.9, 2.1 ]"
				+ "}";

		final String seqString = "{"
				+ "\"name\": \"myseq\","
				+ "\"output_space\": \"out\","
				+ "\"type\": \"sequence\","
				+ "\"transformations\": ["
				+ scaleString + "," + translationString
				+ "]}";

		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(null));
		final Gson gson = gsonBuilder.create();


		final CoordinateTransform ct = gson.fromJson(seqString, CoordinateTransform.class);
//		SequenceCoordinateTransform ct = gson.fromJson(seqString, SequenceCoordinateTransform.class);
		System.out.println( ct );
	}

	public static void main( final String[] args )
	{
		seqTest();
	}

}

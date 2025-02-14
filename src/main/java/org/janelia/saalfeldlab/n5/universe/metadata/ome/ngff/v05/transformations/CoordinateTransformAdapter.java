package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;

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

	final N5Reader n5;

	public static final String[] FIELD_TO_NULL_CHECK = new String[]{
		"path", "name", "input", "output"
	};

	public CoordinateTransformAdapter() {
		this(null);
	}

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
		case(ReferencedCoordinateTransform.TYPE):
			out = context.deserialize( jobj, ReferencedCoordinateTransform.class );
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
		case("thin-plate-spline"):
			out = context.deserialize( jobj, ThinPlateSplineCoordinateTransform.class );
			break;
		case(DisplacementFieldCoordinateTransform.TYPE):
			out = context.deserialize( jobj, DisplacementFieldCoordinateTransform.class );
			break;
		case(CoordinateFieldCoordinateTransform.TYPE):
			out = context.deserialize( jobj, CoordinateFieldCoordinateTransform.class );
			break;
		case("bijection"):

			final JsonObject btmp = context.deserialize( jobj, JsonObject.class );
			final IdentityCoordinateTransform bid = context.deserialize( btmp, IdentityCoordinateTransform.class );
			if( !btmp.has("forward") && !btmp.has("inverse")) {
				out = null;
			}
			else {
				final CoordinateTransform<?> fwd = context.deserialize( btmp.get("forward"), CoordinateTransform.class );
				final CoordinateTransform<?> inv = context.deserialize( btmp.get("inverse"), CoordinateTransform.class );
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
				final CoordinateTransform[] transforms = new CoordinateTransform[ ja.size() ];
				for( int i=0; i < ja.size(); i++) {
					final JsonElement e = ja.get(i).getAsJsonObject();
					transforms[i] = context.deserialize( e, CoordinateTransform.class );
				}
				out = new SequenceCoordinateTransform(id.getName(), id.getInput(), id.getOutput(), transforms);
			}
			else {
				out = null;
			}

			break;
		case("stacked"):
			final IdentityCoordinateTransform sid = context.deserialize( jobj, IdentityCoordinateTransform.class );
			final CoordinateTransform[] transforms = parseTransformList( jobj, "transformations", context );
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

	private final CoordinateTransform[] parseTransformList( final JsonObject elem, final String key, final JsonDeserializationContext context ) {
		final JsonArray ja = elem.get(key).getAsJsonArray();
		final CoordinateTransform[] transforms = new CoordinateTransform[ja.size()];
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

			final CoordinateTransform<?> fwd = bct.getForward();
			final Type ftype = TypeToken.of(fwd.getClass()).getType();
			obj.add("forward", serialize( fwd, ftype, context ));

			final CoordinateTransform<?> inv = bct.getInverse();
			final Type itype = TypeToken.of(inv.getClass()).getType();
			obj.add("inverse", serialize( inv, itype, context ));

			elem = obj;
		}
		else if( src instanceof AbstractLinearCoordinateTransform )
		{
			final AbstractLinearCoordinateTransform linCt = (AbstractLinearCoordinateTransform)src;
			final JsonObject obj =  (JsonObject)context.serialize(linCt);
			if (linCt.getParameterPath() != null) {
				obj.add("affine", context.serialize(TransformUtils.affineToMatrix(linCt.getTransform())));
			}
			return obj;
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

	public static JsonElement serializeGeneric( final CoordinateTransform<?> ct ) {

		final JsonObject json = new JsonObject();
		serializeString(json, CoordinateTransform.TYPE_KEY, ct::getType);
		serializeString(json, CoordinateTransform.NAME_KEY, ct::getName);

		serializeStringArray( json, CoordinateTransform.INPUT_KEY, ct::getInputAxes);
		serializeStringArray( json, CoordinateTransform.OUTPUT_KEY, ct::getOutputAxes);

		serializeString(json, CoordinateTransform.INPUT_KEY, ct::getInput);
		serializeString(json, CoordinateTransform.OUTPUT_KEY, ct::getOutput);

		return json;
	}
	
	private static void serializeString(JsonObject json, final String key, Supplier<String> getter) {

		final String val = getter.get();
		if( val != null )
			json.addProperty(key, val);

	}

	private static void serializeStringArray(JsonObject json, final String key, Supplier<String[]> getter) {

		final String[] val = getter.get();
		if( val != null ) {
			final JsonArray arr = new JsonArray();
			Arrays.stream(val).forEach( s -> { arr.add(s); });
			json.add(key, arr);
		}

	}


}

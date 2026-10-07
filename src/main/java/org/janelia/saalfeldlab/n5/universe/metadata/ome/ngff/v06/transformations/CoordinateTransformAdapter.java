package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class CoordinateTransformAdapter
	implements JsonDeserializer<CoordinateTransform<?>>, JsonSerializer<CoordinateTransform<?>> {

	final N5Reader n5;

	private boolean reverse;

	public static final String[] FIELD_TO_NULL_CHECK = new String[]{
		"path", "name", "input", "output", "inputAxes", "outputAxes"
	};

	public CoordinateTransformAdapter() {
		this(null, true);
	}

	public CoordinateTransformAdapter(boolean reverse) {
		this(null, reverse);
	}

	public CoordinateTransformAdapter( final N5Reader n5 ) {
		this(n5, true);
	}

	public CoordinateTransformAdapter( final N5Reader n5, final boolean reverse ) {
		this.n5 = n5;
		this.reverse = reverse;
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
			if (reverse) reverseParameters(jobj, "scale");
			out = context.deserialize( jobj, ScaleCoordinateTransform.class );
			final ScaleCoordinateTransform scale = context.deserialize( jobj, ScaleCoordinateTransform.class );
			if( scale.getParameterPath() != null) {
				final String path = scale.getParameterPath();
				scale.setParameterSupplier(() -> doubleArraySupplier(n5, path, reverse));
			}
			out = scale;

			break;
		case("translation"):

			if (reverse) reverseParameters(jobj, "translation");
			final TranslationCoordinateTransform tlation = context.deserialize( jobj, TranslationCoordinateTransform.class );
			if( tlation.getParameterPath() != null) {
				final String path = tlation.getParameterPath();
				tlation.setParameterSupplier(() -> doubleArraySupplier(n5, path, reverse));
			}
			out = tlation;

			break;
		case("mapAxis"):
			final MapAxisCoordinateTransform mapAxis = context.deserialize(jobj, MapAxisCoordinateTransform.class);
			if (reverse) {
				MapAxisCoordinateTransform.reverseParameters(mapAxis);
			}
			out = mapAxis;
			break;
		case("byDimension"):
			ByDimensionCoordinateTransform bd = context.deserialize(jobj, ByDimensionCoordinateTransform.class);
			bd.setTransformsAfterDeserialization();
			bd.buildTransform();
			if (reverse) ByDimensionCoordinateTransform.reverseParameters(bd.getTransformations());
			out = bd;
			break;
		case("affine"):
			AbstractAffineCoordinateTransform affine = new AffineCoordinateTransformAdapter(reverse).deserialize(json, typeOfT, context);
			if( affine.getParameterPath() != null) {
				final String path = affine.getParameterPath();
				affine.setParameterSupplier(() -> affineMatrixSupplier(n5, path, reverse));
			}
			out = affine;
			break;
		case("rotation"):
			RotationCoordinateTransform rotation = new RotationCoordinateTransformAdapter(reverse).deserialize(json, typeOfT, context);
			if( rotation.getParameterPath() != null) {
				final String path = rotation.getParameterPath();
				rotation.setParameterSupplier(() -> rotationMatrixSupplier(n5, path, reverse));
			}
			out = rotation;
			break;
		case("thin-plate-spline"):
			out = context.deserialize( jobj, ThinPlateSplineCoordinateTransform.class );
			break;
		case(DisplacementFieldCoordinateTransform.TYPE):
			DisplacementFieldCoordinateTransform df = context.deserialize( jobj, DisplacementFieldCoordinateTransform.class );
			df.setParameterSupplier(() -> {
				return FieldTransformParsing.getParameters(n5, df.getParameterPath(), 
						DisplacementFieldCoordinateTransform.vectorAxisType,
						df.getInput(), df.getInterpolation(), reverse);
			});
		
			out = df;
			break;
		case(CoordinateFieldCoordinateTransform.TYPE):
			CoordinateFieldCoordinateTransform cf = context.deserialize( jobj, CoordinateFieldCoordinateTransform.class );
			cf.setParameterSupplier(() -> {
				return FieldTransformParsing.getParameters(n5, cf.getParameterPath(), 
						CoordinateFieldCoordinateTransform.vectorAxisType,
						cf.getInput(), cf.getInterpolation(), reverse);
			});
			out = cf;
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
		}

		// populate input_axes / output_axes on any transform that declares them
		if( out instanceof AbstractCoordinateTransform )
		{
			final AbstractCoordinateTransform<?> act = (AbstractCoordinateTransform<?>) out;
			if( jobj.has("input_axes") )
				act.setInputAxes( context.deserialize( jobj.get("input_axes"), int[].class ));
			if( jobj.has("output_axes") )
				act.setOutputAxes( context.deserialize( jobj.get("output_axes"), int[].class ));
		}

		return out;
	}

	private static void reverseParameters(final JsonObject obj, final String field) {
		Collections.reverse(obj.get(field).getAsJsonArray().asList());
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
				pt.buildTransform( pt.getParameters());
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
		else if( src instanceof AbstractAffineCoordinateTransform )
		{
			// handle the affine matrix explicitly so the reverse flag can be passed
			// this is called for both standalone affines and affines nested in a sequence.
			elem = new AffineCoordinateTransformAdapter(reverse).serialize((AbstractAffineCoordinateTransform)src, typeOfSrc, context);
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

			if( reverse )
				reverseParametersOnWrite(obj);
		}
		return elem;
	}

	private static void reverseParametersOnWrite(final JsonObject obj) {

		if( !obj.has("type") )
			return;

		final String type = obj.get("type").getAsString();
		if( ScaleCoordinateTransform.TYPE.equals(type) && obj.has("scale") )
			Collections.reverse(obj.get("scale").getAsJsonArray().asList());
		else if( TranslationCoordinateTransform.TYPE.equals(type) && obj.has("translation") )
			Collections.reverse(obj.get("translation").getAsJsonArray().asList());
	}

	public static JsonElement serializeGeneric(final JsonSerializationContext context, final CoordinateTransform<?> ct ) {

		final JsonObject json = new JsonObject();
		serializeString(json, CoordinateTransform.TYPE_KEY, ct::getType);
		serializeString(json, CoordinateTransform.NAME_KEY, ct::getName);


		final int[] inputAxes = ct.getInputAxes();
		final int[] outputAxes = ct.getInputAxes();
		if( inputAxes != null && outputAxes != null ) {
			serializeIntArray(json, CoordinateTransform.INPUT_AXES_KEY, inputAxes);
			serializeIntArray(json, CoordinateTransform.OUTPUT_AXES_KEY, outputAxes);
		}

		serializeReference(context, json, CoordinateTransform.INPUT_KEY, ct::getInput);
		serializeReference(context, json, CoordinateTransform.OUTPUT_KEY, ct::getOutput);

		return json;
	}

	private static void serializeReference(final JsonSerializationContext context, JsonObject json, final String key, Supplier<OmeNgffReference> getter) {

		final OmeNgffReference val = getter.get();
		if( val != null ) {
			json.add(key, context.serialize(val));
		}
	}
	
	private static void serializeString(JsonObject json, final String key, Supplier<String> getter) {

		final String val = getter.get();
		if( val != null )
			json.addProperty(key, val);

	}

	private static void serializeIntArray(JsonObject json, final String key, int[] val) {

		if( val != null ) {
			final JsonArray arr = new JsonArray();
			Arrays.stream(val).forEach( s -> { arr.add(s); });
			json.add(key, arr);
		}
	}

	public static <T extends RealType<T> & NativeType<T>> double[] doubleArraySupplier(final N5Reader n5, final String path, final boolean reverse) {

		if (n5 == null)
			return null;

		if (n5.exists(path)) {
			try {
				@SuppressWarnings("unchecked")
				final CachedCellImg<T, ?> data = (CachedCellImg<T, ?>)N5Utils.open(n5, path);
				if (data.numDimensions() != 1 || !(data.getType() instanceof RealType))
					return null;

				final double[] params = new double[(int)data.dimension(0)];
				final CellCursor<T, ?> c = data.cursor();
				int i = 0;
				while (c.hasNext())
					params[i++] = c.next().getRealDouble();

				if (reverse)
					ArrayUtils.reverse(params);

				return params;
			} catch (final N5Exception e) {}
		}
		return null;
	}
	
	public static <T extends RealType<T> & NativeType<T>> double[][] affineMatrixSupplier(final N5Reader n5, final String path, final boolean reverse) {

		if (n5 == null || path == null)
			return null;

		final double[][] affineMatrix = getDoubleArray2d(n5, path);
		if (reverse) {
			return TransformUtils.reverseCoordinates(affineMatrix);
		}
		return affineMatrix;
	}

	public static <T extends RealType<T> & NativeType<T>> double[][] rotationMatrixSupplier(final N5Reader n5, final String path, final boolean reverse) {
		
		if (n5 == null || path == null)
			return null;

		final double[][] rotationMatrix = getDoubleArray2d(n5, path);
		if (reverse) {
			return TransformUtils.reverseCoordinatesRotation(rotationMatrix);
		}
		return rotationMatrix;
	}
	
	public static <T extends RealType<T> & NativeType<T>> double[][] getDoubleArray2d(final N5Reader n5, final String path) {

		assert n5 != null;
		assert path != null;

		if (n5.exists(path)) {
			try {

				final CachedCellImg<T, ?> matrixRa = (CachedCellImg<T, ?>)N5Utils.open(n5, path);

				if (matrixRa.numDimensions() != 2 || !(matrixRa.getType() instanceof RealType))
					return null;

				// the on-disk shape is [rows, cols] for every format (see writeDoubleArray2d)
				final RandomAccessibleInterval<T> matrix = OmeNgffMetadataParser.storesDimensionsReversed(n5)
						? Views.permute(matrixRa, 0, 1)
						: matrixRa;

				final double[][] params = new double[(int)matrix.dimension(0)][(int)matrix.dimension(1)];
				final Cursor<T> c = Views.flatIterable(matrix).cursor();
				while (c.hasNext()) {
					c.fwd();
					params[c.getIntPosition(0)][c.getIntPosition(1)] = c.get().getRealDouble();
				}

				return params;
			} catch (final N5Exception e) {}
		}
		return null;
	}

	/**
	 * Writes a matrix so that its on-disk shape is [rows, cols] for every format.
	 *
	 * @param n5 the writer
	 * @param dataset the dataset path
	 * @param matrix the matrix, indexed [row][col]
	 */
	public static void writeDoubleArray2d(final N5Writer n5, final String dataset, final double[][] matrix) {

		// imglib2 dimensions (cols, rows), which zarr stores as shape [rows, cols]
		final ArrayImg<DoubleType, DoubleArray> img = ArrayImgs.doubles(
				TransformUtils.flatten(matrix), matrix[0].length, matrix.length);

		// n5 stores imglib2 dimensions as they are, so swap them to (rows, cols)
		final RandomAccessibleInterval<DoubleType> toSave = OmeNgffMetadataParser.storesDimensionsReversed(n5)
				? img
				: Views.permute(img, 0, 1);

		final int[] blkSize = new int[toSave.numDimensions()];
		for (int i = 0; i < blkSize.length; i++)
			blkSize[i] = (int)toSave.dimension(i);

		N5Utils.save(toSave, n5, dataset, blkSize, new RawCompression());
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

public class AffineCoordinateTransformAdapter implements 
	JsonSerializer< AbstractAffineCoordinateTransform >, 
	JsonDeserializer< AbstractAffineCoordinateTransform > {

	/*
	 * Plain gson (no adapters) used to read the base fields (name / input / output / axes) of the
	 * affine. This must NOT go through the deserialization context: this adapter is registered as a
	 * type-hierarchy adapter for AbstractAffineCoordinateTransform (see BigWarpInit.gsonBuilder and
	 * NgffTransformations.gsonBuilder), so context.deserialize(jobj, GeneralAffineCoordinateTransform.class)
	 * would re-enter this same adapter and recurse until StackOverflowError. Reflection via a plain
	 * gson matches the path that already reads these transforms safely (SpacesTransforms, whose gson
	 * registers no hierarchy adapter).
	 */
	private static final Gson BASE_GSON = new Gson();

	/**
	 * When {@code true} (zarr) the affine matrix is stored reversed (C-order) on disk and
	 * un-reversed on read, matching sibling transforms' axis convention; when {@code false} (n5)
	 * the matrix is stored in imglib2 (F-order) as-is. Callers must use the same value the container
	 * uses for the other transforms (see {@link CoordinateTransformAdapter}).
	 */
	private final boolean reverse;

	public AffineCoordinateTransformAdapter() {
		this(true);
	}

	public AffineCoordinateTransformAdapter(final boolean reverse) {
		this.reverse = reverse;
	}

	@Override
	public JsonElement serialize(AbstractAffineCoordinateTransform src, Type typeOfSrc, JsonSerializationContext context) {

		final JsonObject json = CoordinateTransformAdapter.serializeGeneric(context, src).getAsJsonObject();
		final double[][] mtx = src.affine;
		final double[][] mtxOut = reverse ? TransformUtils.reverseCoordinates(mtx) : mtx;
		json.add(AbstractAffineCoordinateTransform.TYPE, context.serialize(mtxOut));
		return json;
	}

	@Override
	public AbstractAffineCoordinateTransform deserialize(final JsonElement json, final Type typeOfT,
			final JsonDeserializationContext context) throws JsonParseException {

		if (!json.isJsonObject())
			return null;

		final JsonObject jobj = json.getAsJsonObject();
		final double[][] affineStored = context.deserialize(
				jobj.get(AbstractAffineCoordinateTransform.TYPE), double[][].class);

		double[][] affine = null;
		if( affineStored != null ) {
			affine = reverse ? TransformUtils.reverseCoordinates(affineStored) : affineStored;
			final GeneralAffineCoordinateTransform base = BASE_GSON.fromJson(jobj, GeneralAffineCoordinateTransform.class);
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

			GeneralAffineCoordinateTransform tf = BASE_GSON.fromJson(jobj, GeneralAffineCoordinateTransform.class);
			tf.buildTransform();
			return tf;
		}
	}
	
	private static boolean sourceDimsEqualTargetDims(final double[][] affine) {
		// rows = numTarget, cols = numSource + 1; square iff numTarget == numSource
		return affine != null && affine.length > 0 && affine.length == affine[0].length - 1;
	}


}

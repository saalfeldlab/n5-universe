package org.janelia.saalfeldlab.n5.universe.translation;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axisTransforms.TransformAxes;
import org.janelia.saalfeldlab.n5.universe.metadata.axisTransforms.TransformAxesMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransformAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Builds the {@link Gson} used for the "canonical" metadata dialect, with the
 * adapters needed to de/serialize its transforms, axes and dataset attributes.
 */
public class JqUtils {

	public static GsonBuilder gsonBuilder(final N5Reader n5) {
		return gsonBuilder(n5 instanceof GsonN5Reader ? ((GsonN5Reader) n5).getGson() : null, n5);
	}

	/**
	 * @param base
	 * 		the {@code Gson} to extend (keeping its adapters), or {@code null}
	 * 		to start from scratch
	 * @param n5
	 * 		the reader that {@link SpatialTransformAdapter} reads the
	 * 		parameters of parametrized transforms from. It is only stored here,
	 * 		and used during deserialization.
	 */
	public static GsonBuilder gsonBuilder(final Gson base, final N5Reader n5) {
		final GsonBuilder gsonBuilder = base != null ? base.newBuilder() : new GsonBuilder();
		gsonBuilder.registerTypeAdapter(SpatialTransform.class, new SpatialTransformAdapter(n5));
		gsonBuilder.registerTypeAdapter(CanonicalMetadata.class, new CanonicalMetadataAdapter());
		gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeAdapter(TransformAxes.class, new TransformAxesMetadataAdapter());
		gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());

		gsonBuilder.disableHtmlEscaping();
		return gsonBuilder;
	}

	public static Gson buildGson(final N5Reader n5) {
		return gsonBuilder(n5).create();
	}

}

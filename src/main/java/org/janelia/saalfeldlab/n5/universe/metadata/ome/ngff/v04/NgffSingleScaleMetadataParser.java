package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.imglib2.realtransform.ScaleAndTranslation;

public class NgffSingleScaleMetadataParser implements N5MetadataParser<NgffSingleScaleAxesMetadata>, N5MetadataWriter<NgffSingleScaleAxesMetadata> {

	private final Gson gson;

	public NgffSingleScaleMetadataParser() {

		gson = OmeNgffMetadataParser.gsonBuilder().create();
	}

	@Override
	public Optional<NgffSingleScaleAxesMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		final Map<String, Class<?>> attrs = n5.listAttributes(node.getPath());
		if (attrs.containsKey(NgffSingleScaleAxesMetadata.AXIS_KEY) && attrs.containsKey(NgffSingleScaleAxesMetadata.COORDINATETRANSFORMATIONS_KEY)) {

			final JsonArray axArr = n5.getAttribute(node.getPath(), NgffSingleScaleAxesMetadata.AXIS_KEY, JsonArray.class);
			final Axis[] axes = gson.fromJson(axArr, Axis[].class);

			final JsonArray ctArr = n5.getAttribute(node.getPath(), NgffSingleScaleAxesMetadata.COORDINATETRANSFORMATIONS_KEY, JsonArray.class);
			final CoordinateTransformation<?>[] cts = gson.fromJson(ctArr, CoordinateTransformation[].class);

			if (cts == null || cts.length == 0)
				return Optional.of(new NgffSingleScaleAxesMetadata(node.getPath(), null, null, null));

			final ScaleAndTranslation scaleAndTranslation = MetadataUtils.scaleTranslationFromCoordinateTransformations(cts);
			final double[] scale = scaleAndTranslation.getScaleCopy();
			final double[] translation = scaleAndTranslation.getTranslationCopy();

			final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(node.getPath());
			if( OmeNgffMetadataParser.cOrder(dsetAttrs))
				ArrayUtils.reverse( axes );

			return Optional.of(new NgffSingleScaleAxesMetadata(node.getPath(), scale, translation, axes, null));
		}

		return Optional.empty();
	}


	@Override
	public void writeMetadata(final NgffSingleScaleAxesMetadata t, final N5Writer n5, final String path) throws Exception {

		final JsonElement json = gson.toJsonTree(t);
		n5.setAttribute(path, NgffSingleScaleAxesMetadata.AXIS_KEY, json.getAsJsonObject().get("axes"));
		n5.setAttribute(path, NgffSingleScaleAxesMetadata.COORDINATETRANSFORMATIONS_KEY, t.getCoordinateTransformations());
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

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

			double[] scale = null;
			double[] translation = null;
			for (int i = 0; i < cts.length; i++) {
				if (cts[i] instanceof ScaleCoordinateTransformation)
					scale = ((ScaleCoordinateTransformation)cts[i]).getScale();
				else if (cts[i] instanceof TranslationCoordinateTransformation)
					translation = ((TranslationCoordinateTransformation)cts[i]).getTranslation();
			}

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

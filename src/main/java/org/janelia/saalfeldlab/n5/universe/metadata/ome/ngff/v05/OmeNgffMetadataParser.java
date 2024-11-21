package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.DatasetAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.*;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OmeNgffMetadataParser implements N5MetadataParser<OmeNgffMetadata>, N5MetadataWriter<OmeNgffMetadata> {

	private final Gson gson;

	public OmeNgffMetadataParser(final boolean reverse) {

		gson = gsonBuilder().create();
	}

	public OmeNgffMetadataParser() {

		this(false);
	}

	public static GsonBuilder gsonBuilder() {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter())
				.registerTypeAdapter(OmeNgffDataset.class, new DatasetAdapter())
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter());
	}

	@Override
	public Optional<OmeNgffMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		OmeNgffMultiScaleMetadata[] multiscales;
		try {
			final JsonElement version = n5.getAttribute(node.getPath(), "ome/version", JsonElement.class);
			assert version.getAsString().equals("0.5");
			final JsonElement base = n5.getAttribute(node.getPath(), "ome/multiscales", JsonElement.class);
			multiscales = gson.fromJson(base, OmeNgffMultiScaleMetadata[].class);
		} catch (final Exception e) {
			throw e;
			//return Optional.empty();
		}

		if (multiscales == null || multiscales.length == 0) {
			return Optional.empty();
		}

		int nd = -1;
		final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();
		for (final N5TreeNode childNode : node.childrenList()) {
			if (childNode.isDataset() && childNode.getMetadata() != null) {
				scaleLevelNodes.put(childNode.getPath(), childNode);
				if (nd < 0)
					nd = ((N5DatasetMetadata) childNode.getMetadata()).getAttributes().getNumDimensions();
			}
		}

		if (nd < 0)
			return Optional.empty();

		/*
		 * Need to replace all children with new children with the metadata from
		 * this object
		 */
		for (int j = 0; j < multiscales.length; j++) {

			final OmeNgffMultiScaleMetadata ms = multiscales[j];

			final String[] paths = ms.getPaths();
			final DatasetAttributes[] attrs = new DatasetAttributes[ms.getPaths().length];

			final N5DatasetMetadata[] dsetMeta = new N5DatasetMetadata[paths.length];
			for (int i = 0; i < paths.length; i++) {
				dsetMeta[i] = ((N5DatasetMetadata)scaleLevelNodes.get(MetadataUtils.canonicalPath(node, paths[i])).getMetadata());
				attrs[i] = dsetMeta[i].getAttributes();
			}

			// maybe axes can be flipped first?
			ArrayUtils.reverse(ms.axes);

			final NgffSingleScaleAxesMetadata[] msChildrenMeta = OmeNgffMultiScaleMetadata.buildMetadata(
					nd, node.getPath(), ms.datasets, attrs, ms.coordinateTransformations, ms.metadata, ms.axes);

			MetadataUtils.updateChildrenMetadata(node, msChildrenMeta, false);

			// axes need to be flipped after the child is created
			// is this actually true?
			// ArrayUtils.reverse(ms.axes);

			multiscales[j] = new OmeNgffMultiScaleMetadata(ms, msChildrenMeta);
		}

		return Optional.of(new OmeNgffMetadata(node.getPath(), multiscales));
	}

	@Override
	public void writeMetadata(final OmeNgffMetadata t, final N5Writer n5, final String groupPath) throws Exception {

		final OmeNgffMultiScaleMetadata[] ms = t.multiscales;
		final JsonElement jsonElem = gson.toJsonTree(ms);

		// need to reverse axes
		for (final JsonElement e : jsonElem.getAsJsonArray().asList()) {
			final JsonArray axes = e.getAsJsonObject().get("axes").getAsJsonArray();
			Collections.reverse(axes.asList());
		}

		n5.setAttribute(groupPath, "ome.multiscales", jsonElem);
	}

	public static boolean cOrder(final DatasetAttributes datasetAttributes) {

		if (datasetAttributes instanceof ZarrDatasetAttributes) {
			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return zattrs.isRowMajor();
		}
		return false;
	}

}

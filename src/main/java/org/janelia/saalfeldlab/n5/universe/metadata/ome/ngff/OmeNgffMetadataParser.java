package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03.NgffV03SingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03.OmeNgffV03MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffV04MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffV04MultiScaleMetadata.OmeNgffV04Dataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.OmeNgffV05MultiScaleMetadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class OmeNgffMetadataParser implements N5MetadataParser<OmeNgffMetadata>, N5MetadataWriter<OmeNgffMetadata> {

	private final Gson gson;

	protected final boolean assumeChildren;

	public OmeNgffMetadataParser(final boolean assumeChildren) {

		gson = gsonBuilder().create();
		this.assumeChildren = assumeChildren;
	}

	public OmeNgffMetadataParser() {

		this(false);
	}

	public static GsonBuilder gsonBuilder() {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter())
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter());
	}

	@Override
	public Optional<OmeNgffMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		OmeNgffMultiScaleMetadata[] multiscales;
		try {
			final JsonElement base = n5.getAttribute(node.getPath(), "multiscales", JsonElement.class);
			multiscales = gson.fromJson(base, OmeNgffMultiScaleMetadata[].class);
		} catch (final Exception e) {
			return Optional.empty();
		}

		if (multiscales == null || multiscales.length == 0) {
			return Optional.empty();
		}

		int nd = -1;
		final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();

		DatasetAttributes[] attrs = null;
		if( assumeChildren ) {

			for (int j = 0; j < multiscales.length; j++) {

				final OmeNgffMultiScaleMetadata ms = multiscales[j];

				nd = ms.getAxes().length;

				final int numScales = ms.getDatasets().length;
				attrs = new DatasetAttributes[numScales];
				for (int i = 0; i < numScales; i++) {

					// TODO check existence here or elsewhere?
					final N5TreeNode child = new N5TreeNode(
							MetadataUtils.canonicalPath(node, ms.getPaths()[i]));
					final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(child.getPath());
					if (dsetAttrs == null)
						return Optional.empty();

					attrs[i] = dsetAttrs;
					node.childrenList().add(child);
				}

				final NgffSingleScaleAxesMetadata[] msChildrenMeta = OmeNgffV04MultiScaleMetadata.buildMetadata(nd,
						node.getPath(), ms.getDatasets(), attrs, ms.getCoordinateTransformations(), ms.metadata, ms.axes);

				// add to scale level nodes map
				node.childrenList().forEach(n -> {
					scaleLevelNodes.put(n.getPath(), n);
				});
			}

		} else {

			for (final N5TreeNode childNode : node.childrenList()) {
				if (childNode.isDataset() && childNode.getMetadata() != null) {
					scaleLevelNodes.put(childNode.getPath(), childNode);
					if (nd < 0)
						nd = ((N5DatasetMetadata) childNode.getMetadata()).getAttributes().getNumDimensions();
				}
			}

			if (nd < 0)
				return Optional.empty();

			for (int j = 0; j < multiscales.length; j++) {

				final OmeNgffV04MultiScaleMetadata ms = multiscales[j];
				final String[] paths = ms.getPaths();
				attrs = new DatasetAttributes[ms.getPaths().length];
				final N5DatasetMetadata[] dsetMeta = new N5DatasetMetadata[paths.length];
				for (int i = 0; i < paths.length; i++) {
					dsetMeta[i] = ((N5DatasetMetadata)scaleLevelNodes.get(MetadataUtils.canonicalPath(node, paths[i])).getMetadata());
					attrs[i] = dsetMeta[i].getAttributes();
				}
			}
		}

		/*
		 * Need to replace all children with new children with the metadata from
		 * this object
		 */
		for (int j = 0; j < multiscales.length; j++) {

			final OmeNgffMultiScaleMetadata ms = multiscales[j];

//			final NgffSingleScaleAxesMetadata[] msChildrenMeta = OmeNgffV04MultiScaleMetadata.buildMetadata(
//					nd, node.getPath(), ms.getDatasets(), attrs, ms.getCoordinateTransformations(), ms.metadata, ms.axes);
			final NgffSingleScaleAxesMetadata[] msChildrenMeta = makeChildren( ms,
					nd, node.getPath(), ms.getDatasets(), attrs, ms.getCoordinateTransformations(), ms.metadata, ms.axes);

			MetadataUtils.updateChildrenMetadata(node, msChildrenMeta, false);
			multiscales[j] = makeMultiscales(ms, msChildrenMeta);
		}

		return Optional.of(new OmeNgffMetadata(node.getPath(), multiscales));
	}
	
	private NgffSingleScaleAxesMetadata[]  makeChildren(OmeNgffMultiScaleMetadata ms,
			final int nd, final String path, final OmeNgffV04Dataset[] datasets,
			final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] transforms,
			final OmeNgffDownsamplingMetadata metadata,
			final Axis[] axes )  {
		
		switch( ms.version ) {
		case "0.3":
			return new OmeNgffV03MultiScaleMetadata((OmeNgffV03MultiScaleMetadata)ms, msChildrenMeta);
		case "0.4":
			return ms.buildChildren();
		case "0.5":
			return new OmeNgffV05MultiScaleMetadata((OmeNgffV05MultiScaleMetadata)ms, msChildrenMeta);
		default:
			return null;
		}
	}

	private OmeNgffMultiScaleMetadata makeMultiscales(OmeNgffMultiScaleMetadata ms, NgffSingleScaleAxesMetadata[] msChildrenMeta )  {
		
		switch( ms.version ) {
		case "0.3":
			return new OmeNgffV03MultiScaleMetadata((OmeNgffV03MultiScaleMetadata)ms, msChildrenMeta);
		case "0.4":
			return new OmeNgffV04MultiScaleMetadata((OmeNgffV04MultiScaleMetadata)ms, msChildrenMeta);
		case "0.5":
			return new OmeNgffV05MultiScaleMetadata((OmeNgffV05MultiScaleMetadata)ms, msChildrenMeta);
		default:
			return null;
		}
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

		n5.setAttribute(groupPath, "multiscales", jsonElem);
	}

}

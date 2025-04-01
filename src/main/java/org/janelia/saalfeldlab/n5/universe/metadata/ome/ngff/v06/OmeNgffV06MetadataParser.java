package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06;

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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.NgffV06SingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfigAdapter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class OmeNgffV06MetadataParser implements N5MetadataParser<OmeNgffV06MultiScaleMetadata>, N5MetadataWriter<OmeNgffV06MultiScaleMetadata> {

	final static String VERSION = "0.6-dev";

	private final Gson gson;

	protected final boolean assumeChildren;

	public OmeNgffV06MetadataParser(final boolean assumeChildren) {

		gson = gsonBuilder().create();
		this.assumeChildren = assumeChildren;
	}

	public OmeNgffV06MetadataParser() {

		this(false);
	}

	public static GsonBuilder gsonBuilder() {

		return new GsonBuilder()
				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter());
	}

	@Override
	public Optional<OmeNgffV06MultiScaleMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		final OmeNgffV06MultiScaleMetadata multiscales = parse( n5, node.getPath());
		if (multiscales == null )
			return Optional.empty();

		int nd = -1;
		final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();

		DatasetAttributes[] attrs = null;
		if( assumeChildren ) {
	
			nd = multiscales.getAxes().length;
			final int numScales = multiscales.getDatasets().length;
			attrs = new DatasetAttributes[numScales];
			for (int i = 0; i < numScales; i++) {

				// TODO check existence here or elsewhere?
				final N5TreeNode child = new N5TreeNode(
						MetadataUtils.canonicalPath(node, multiscales.getDatasets()[i].path));
				final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(child.getPath());
				if (dsetAttrs == null)
					return Optional.empty();

				attrs[i] = dsetAttrs;
				node.childrenList().add(child);
			}

			// add to scale level nodes map
			node.childrenList().forEach(n -> {
				scaleLevelNodes.put(n.getPath(), n);
			});

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


			final String[] paths = multiscales.getPaths();
			attrs = new DatasetAttributes[paths.length];
			final N5DatasetMetadata[] dsetMeta = new N5DatasetMetadata[paths.length];
			for (int i = 0; i < paths.length; i++) {
				dsetMeta[i] = ((N5DatasetMetadata)scaleLevelNodes.get(MetadataUtils.canonicalPath(node, paths[i])).getMetadata());
				attrs[i] = dsetMeta[i].getAttributes();
			}
		}

		/*
		 * Need to replace all children with new children with the metadata from
		 * this object
		 */

		final NgffV06SingleScaleAxesMetadata[] msChildrenMeta = OmeNgffV06MultiScaleMetadata.buildMetadata(nd,
				node.getPath(),
				multiscales.getDatasets(),
				attrs,
				multiscales.coordinateTransformations,
				multiscales.metadata,
				multiscales.getCoordinateSystems());

		MetadataUtils.updateChildrenMetadata(node, msChildrenMeta, false);
		return Optional.of(new OmeNgffV06MultiScaleMetadata(multiscales, msChildrenMeta));
	}	
	
	private OmeNgffV06MultiScaleMetadata parse(N5Reader n5, final String dset) {

		try {
			final JsonElement elem = n5.getAttribute(dset, "ome/multiscales", JsonElement.class);
			final OmeNgffV06MultiScaleMetadata meta = gson.fromJson(elem, OmeNgffV06MultiScaleMetadata.class);

			return new OmeNgffV06MultiScaleMetadata(
					meta.getAxes().length,
					dset,
					meta.getName(),
					meta.type,
					meta.coordinateSystems,
					meta.datasets,
					meta.childrenAttributes,
					meta.coordinateTransformations,
					meta.metadata, false);
		} catch (final Exception e) {
			return null;
		}
	}

	@Override
	public void writeMetadata(final OmeNgffV06MultiScaleMetadata t, final N5Writer n5, final String groupPath) throws Exception {

		final JsonElement jsonElem = gson.toJsonTree(t);

		n5.setAttribute("", "ome/version", VERSION);
		n5.setAttribute(groupPath, "ome/multiscales", jsonElem);
	}

	public static boolean cOrder(final DatasetAttributes datasetAttributes) {

		if (datasetAttributes instanceof ZarrDatasetAttributes) {
			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return zattrs.isRowMajor();
		}
		return false;
	}

}

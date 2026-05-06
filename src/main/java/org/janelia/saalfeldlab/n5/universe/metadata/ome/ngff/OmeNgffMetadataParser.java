package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformationAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03.OmeNgffV03MetadataProcessor;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3DatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OmeNgffMetadataParser implements N5MetadataParser<OmeNgffMetadata>, N5MetadataWriter<OmeNgffMetadata> {
	
	private final static String OME = "ome";
	private final static String MS = "multiscales";
	private final static String OMEMS = "ome/multiscales";

	private final Gson gson;

	protected boolean reverse;

	public OmeNgffMetadataParser(final boolean reverse) {

		this.reverse = reverse;
		gson = gsonBuilder().create();
	}

	public OmeNgffMetadataParser(final N5Reader n5) {
		this(reverse(n5));
	}
	
	public static boolean reverse(final N5Reader n5) {
		return n5 instanceof ZarrV3KeyValueReader || n5 instanceof ZarrKeyValueReader;
	}

	public OmeNgffMetadataParser() {

		this(true);
	}

	public GsonBuilder gsonBuilder() {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(reverse));
	}

	private static JsonElement getMultiscales(JsonObject obj) {

		if (obj.has(MS))
			return obj.get(MS);
		else
			return null;
	}

	@Override
	public Optional<OmeNgffMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		final JsonObject base = n5.getAttribute(node.getPath(), "", JsonElement.class).getAsJsonObject();

		final JsonElement msBase;
		if (base.has(OME)) // check v0.5
			msBase = getMultiscales(base.get(OME).getAsJsonObject());
		else
			msBase = getMultiscales(base);

		if (msBase == null)
			return Optional.empty();

		OmeNgffMultiScaleMetadata[] multiscales;
		try {
			multiscales = gson.fromJson(msBase, OmeNgffMultiScaleMetadata[].class);
		} catch (final Exception e) {
			return Optional.empty();
		}

		if (multiscales == null || multiscales.length == 0) {
			return Optional.empty();
		}

		int nd = -1;
		final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();

		DatasetAttributes[] attrs = null;
		for (int j = 0; j < multiscales.length; j++) {

			final OmeNgffMultiScaleMetadata ms = multiscales[j];
			nd = ms.getAxes().length;

			final int numScales = ms.getDatasets().length;
			attrs = new DatasetAttributes[numScales];
			for (int i = 0; i < numScales; i++) {

				final N5TreeNode child = new N5TreeNode(
						MetadataUtils.canonicalPath(node, ms.getPaths()[i]));

				final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(child.getPath());
				if (dsetAttrs == null)
					return Optional.empty();

				attrs[i] = dsetAttrs;
				node.childrenList().add(child);
			}

			if( ms.version.equals("0.3")) {
				// OME-Zarr v0.3 does not have coordinate transformation metadata per scale level
				// so modify datasets in place, adding inferred coordinate transformations
				// per scale level here
				OmeNgffV03MetadataProcessor.readProcess(ms.getDatasets(), attrs);
			}

			// add to scale level nodes map
			node.childrenList().forEach(n -> {
				scaleLevelNodes.put(n.getPath(), n);
			});
		}

	
		/*
		 * Need to replace all children with new children with the metadata from
		 * this object
		 */
		for (int j = 0; j < multiscales.length; j++) {

			final OmeNgffMultiScaleMetadata ms = multiscales[j];
			final NgffSingleScaleAxesMetadata[] msChildrenMeta = OmeNgffMultiScaleMetadata.buildMetadata(
					nd, node.getPath(), ms.getDatasets(), attrs, ms.getCoordinateTransformations(), ms.metadata, ms.axes, false);

			MetadataUtils.updateChildrenMetadata(node, msChildrenMeta, false);
			multiscales[j] = new OmeNgffMultiScaleMetadata(ms, msChildrenMeta);
		}

		return Optional.of(new OmeNgffMetadata(node.getPath(), multiscales));
	}

	@Override
	public void writeMetadata(final OmeNgffMetadata t, final N5Writer n5, final String groupPath) throws Exception {

		final OmeNgffMultiScaleMetadata[] ms = t.multiscales;
		final JsonElement jsonElem = gson.toJsonTree(ms);

		if( t.multiscales[0].version.equals("0.5")) {
			n5.setAttribute(groupPath, OME + "/version", "0.5");
			n5.setAttribute(groupPath, OMEMS, jsonElem);
			writeZarr3DimensionNames(n5, groupPath, ms);
		}
		else
			n5.setAttribute(groupPath, MS, jsonElem);
	}

	private void writeZarr3DimensionNames(N5Writer n5, final String groupPath, OmeNgffMultiScaleMetadata[] ms) {

		if (!(n5 instanceof ZarrV3KeyValueWriter)) {
			return;
		}

		final ZarrV3KeyValueWriter zarr3 = (ZarrV3KeyValueWriter)n5;
		final List<String> axisNames = Stream.of(ms[0].getAxes()).map(Axis::getName)
				.collect(Collectors.toList());

		if (reverse)
			Collections.reverse(axisNames);

		for (OmeNgffDataset dataset : ms[0].getDatasets()) {
			final String path = groupPath + "/" + dataset.path;
			zarr3.setRawAttribute(path, ZarrV3DatasetAttributes.DIMENSION_NAMES_KEY, axisNames);
		}
	}

}

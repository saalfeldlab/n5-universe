package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformationAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class OmeNgffMetadataParser implements N5MetadataParser<OmeNgffMetadata> {

	private Gson gson;

	public OmeNgffMetadataParser() {

		gson = new GsonBuilder()
				.registerTypeAdapter(CoordinateTransformation.class, new CoordinateTransformationAdapter()).create();
	}
	
	@Override
	public Optional<OmeNgffMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {
		OmeNgffMultiScaleMetadata[] multiscales;
		try {
			final JsonElement base = n5.getAttribute(node.getPath(), "multiscales", JsonElement.class);
			multiscales = gson.fromJson(base, OmeNgffMultiScaleMetadata[].class);
		} catch (IOException e) {
			return Optional.empty();
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
		 * Need to replace all children with new children with the 
		 * metadata from this  
		 */
		for ( OmeNgffMultiScaleMetadata ms : multiscales ) {
			ms.path = node.getPath();
			String[] paths = ms.getPaths();
			DatasetAttributes[] attrs = new DatasetAttributes[ ms.getPaths().length ];
			N5DatasetMetadata[] dsetMeta = new N5DatasetMetadata[ paths.length ];
			for( int i = 0; i < paths.length; i++ ) {
				dsetMeta[ i ] = ((N5DatasetMetadata)scaleLevelNodes.get( MetadataUtils.canonicalPath( node, paths[ i ] ) ).getMetadata());
				attrs[ i ] = dsetMeta[ i ].getAttributes();
			}

			final N5SingleScaleMetadata[] msChildrenMeta = ms.buildChildren( nd, attrs, ms.coordinateTransformations, ms.axes );
			MetadataUtils.updateChildrenMetadata( node, msChildrenMeta );
			ms.childrenMetadata = msChildrenMeta;
			ms.childrenAttributes = attrs;
		}
		return Optional.of(new OmeNgffMetadata(node.getPath(), multiscales ));
	}

}

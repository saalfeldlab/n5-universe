package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.util.Optional;
import java.util.Map.Entry;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OmeNgffMetadataSingleScaleParser implements N5MetadataParser<NgffSingleScaleAxesMetadata>, N5MetadataWriter<NgffSingleScaleAxesMetadata> {

	private final Gson gson;

	private boolean reverse;

	public OmeNgffMetadataSingleScaleParser() {
		this( false );
	}

	public OmeNgffMetadataSingleScaleParser(final boolean reverse) {

		this.gson = OmeNgffMetadataParser.gsonBuilder().create();
		this.reverse = reverse;
	}

	@Override
	public void writeMetadata(NgffSingleScaleAxesMetadata t, N5Writer n5, String path) throws Exception {

		final JsonObject jsonElem = gson.toJsonTree(t).getAsJsonObject();
		for( final Entry<String, JsonElement> e : jsonElem.entrySet() )
			n5.setAttribute(path, e.getKey(), e.getValue());
	}

	@Override
	public Optional<NgffSingleScaleAxesMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		// TODO implement me
//		try {
//			return Optional.of(n5.getAttribute(node.getNodeName(), null, NgffSingleScaleAxesMetadata.class));
//		} catch( final N5Exception e ) {
			return Optional.empty();
//		}
	}

}

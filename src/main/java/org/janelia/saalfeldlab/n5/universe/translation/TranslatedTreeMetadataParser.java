package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;

/**
 * A parser for metadata that are translated into the "canonical" dialect.
 * 
 * @author John Bogovic
 */
public class TranslatedTreeMetadataParser extends CanonicalMetadataParser {

	private ContainerMetadataNode translatedRoot;

	private ContainerTranslation translationFun;

	public TranslatedTreeMetadataParser(final String translation) {
		this(translation, x -> true);
	}

	public TranslatedTreeMetadataParser(final String translation,
			final Predicate<CanonicalMetadata> filter) {
		super( filter );
		translationFun = new JqContainerTranslation( translation, JqUtils.buildGson(null));
	}

	public boolean validTranslation() {
		return translationFun != null;
	}

	public TranslatedTreeMetadataParser( final N5Reader n5, final String n5Tree, final String translation) {
		super( null );
		translationFun = new JqContainerTranslation( translation, JqUtils.buildGson(n5));
	}

	protected void setup( final N5Reader n5 ) {
//		if( gson == null )
			setGson( JqUtils.buildGson( n5 ));

		root = ContainerMetadataNode.build(n5, gson);
		translatedRoot = translationFun.apply( root );
		if( translatedRoot != null )
			translatedRoot.addPathsRecursive();
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {
		setup( n5 );
		return parseMetadata( node, n5.getGroupSeparator());
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(final N5Reader n5, final String dataset) {
		return parseMetadata( n5, new N5TreeNode( dataset ) );
	}

	public Optional<CanonicalMetadata> parseMetadata(final String dataset, final String groupSep ) {
		return parseMetadata( new N5TreeNode( dataset ), groupSep );
	}

	public Optional<CanonicalMetadata> parseMetadata(N5TreeNode node, String groupSep) {

		if (translatedRoot == null)
			return Optional.empty();

		return translatedRoot.getChild( node.getPath(), groupSep )
				.map( ContainerMetadataNode::getContainerAttributes )
				.map( this::canonicalMetadata )
				.filter(filter);
	}

	public CanonicalMetadata canonicalMetadata(final HashMap<String, JsonElement> attrMap) {
		return gson.fromJson(gson.toJson(attrMap), CanonicalMetadata.class);
	}

}

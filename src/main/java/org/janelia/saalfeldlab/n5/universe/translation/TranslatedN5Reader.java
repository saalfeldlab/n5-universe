package org.janelia.saalfeldlab.n5.universe.translation;

import java.net.URI;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class TranslatedN5Reader implements GsonN5Reader {

	private final N5Reader n5;

	protected final InvertibleTreeTranslation translation;

	public TranslatedN5Reader( final N5Reader n5Base,
			final Gson gson,
			final String fwdTranslation,
			final String invTranslation ) {
		this.n5 = n5Base;
		final ContainerMetadataNode root = ContainerMetadataNode.build(n5Base, gson);
		root.addPathsRecursive();
		translation = new InvertibleTreeTranslation(root, gson, fwdTranslation, invTranslation);
	}

	public InvertibleTreeTranslation getTranslation() {
		return translation;
	}

	public TranslatedN5Reader( final GsonN5Reader n5Base,
			final String fwdTranslation,
			final String invTranslation ) {
		this( n5Base, JqUtils.buildGson(n5Base), fwdTranslation, invTranslation );
	}

	@Override
	public JsonElement getAttributes(final String pathName) throws N5Exception.N5IOException {
		return translation.getTranslated().getAttributes(pathName);
	}

	/**
	 * Returns the path in the original container given the path in the translated container.
	 *
	 * @param pathName the path in the translated container
	 * @return the path in the original container
	 */
	public String originalPath( String pathName )
	{
		final ContainerMetadataNode pathNode = new ContainerMetadataNode();
		pathNode.createGroup(pathName);
		pathNode.addPathsRecursive();
		final ContainerMetadataNode translatedPathNode = translation.getInverseTranslationFunction().apply(pathNode);
		translatedPathNode.addPathsRecursive();
		final String path = translatedPathNode.flattenLeaves().findFirst().get().getPath();
		return N5URI.normalizeGroupPath(path);
	}

	@Override
	public DataBlock<?> readBlock(String pathName, DatasetAttributes datasetAttributes, long... gridPosition) {

		return n5.readBlock( originalPath( pathName ), datasetAttributes, gridPosition);
	}

	@Override
	public boolean exists(String pathName) {
		return translation.getTranslated().exists(pathName);
	}

	@Override
	public String[] list(String pathName) {
		return translation.getTranslated().list(pathName);
	}

	@Override
	public Map<String, Class<?>> listAttributes(String pathName) {
		return translation.getTranslated().listAttributes(pathName);
	}

	@Override
	public String groupPath(String... nodes) {
		return n5.groupPath(nodes);
	}

	@Override
	public URI getURI() {
		return n5.getURI(); // add translation info?
	}

	@Override
	public Gson getGson() {
		return translation.getGson();
	}

	@Override
	public String getAttributesKey() {

		// TODO fix
		return "attributes.json";
	}

}

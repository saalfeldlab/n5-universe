package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;

public class TranslatedN5Reader extends AbstractGsonReader {
	
	private final N5Reader n5;

	protected final InvertibleTreeTranslation translation;

	public TranslatedN5Reader( final N5Reader n5Base, 
			final Gson gson,
			final String fwdTranslation, 
			final String invTranslation ) {
		this.n5 = n5Base;
		ContainerMetadataNode root = ContainerMetadataNode.build(n5Base, gson);
		root.addPathsRecursive();
		translation = new InvertibleTreeTranslation(root, gson, fwdTranslation, invTranslation);
	}
	
	public InvertibleTreeTranslation getTranslation() {
		return translation;
	}

	public TranslatedN5Reader( final AbstractGsonReader n5Base, 
			final String fwdTranslation,
			final String invTranslation ) {
		this( n5Base, JqUtils.buildGson(n5Base), fwdTranslation, invTranslation );
	}

	@Override
	public <T> T getAttribute(String pathName, String key, Class<T> clazz) throws IOException {
		return translation.getTranslated().getAttribute(pathName, key, clazz);
	}

	@Override
	public <T> T getAttribute(String pathName, String key, Type type) throws IOException {
		return translation.getTranslated().getAttribute(pathName, key, type);
	}
	
	/**
	 * Returns the path in the original container given the path in the translated container.
	 * 
	 * @param pathName the path in the translated container
	 * @return the path in the original container
	 */
	public String originalPath( String pathName )
	{
		ContainerMetadataNode pathNode = new ContainerMetadataNode();
		pathNode.createGroup(pathName);
		pathNode.addPathsRecursive();
		ContainerMetadataNode translatedPathNode = translation.getInverseTranslationFunction().apply(pathNode);
		translatedPathNode.addPathsRecursive();
		final String path = translatedPathNode.flattenLeaves().findFirst().get().getPath();
		return path;
	}

	@Override
	public DataBlock<?> readBlock(String pathName, DatasetAttributes datasetAttributes, long... gridPosition)
			throws IOException {

		return n5.readBlock( originalPath( pathName ), datasetAttributes, gridPosition);
	}

	@Override
	public boolean exists(String pathName) {
		return translation.getTranslated().exists(pathName);
	}

	@Override
	public String[] list(String pathName) throws IOException {
		return translation.getTranslated().list(pathName);
	}

	@Override
	public Map<String, Class<?>> listAttributes(String pathName) throws IOException {
		return translation.getTranslated().listAttributes(pathName);
	}

	@Override
	public HashMap<String, JsonElement> getAttributes(String pathName) throws IOException {
		return translation.getTranslated().getNode(pathName)
				.map( ContainerMetadataNode::getAttributes )
				.orElse( new HashMap<>());
	}

	@Override public JsonElement getAttributesJson(String pathName) throws IOException {

		if (n5 instanceof GsonAttributesParser) {
			return ((GsonAttributesParser)n5).getAttributesJson("/");
		} else {
			final JsonObject root = new JsonObject();
			getAttributes(pathName).forEach(root::add);
			return root;
		}

	}

}

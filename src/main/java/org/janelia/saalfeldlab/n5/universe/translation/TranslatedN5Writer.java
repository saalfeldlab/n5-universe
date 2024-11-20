package org.janelia.saalfeldlab.n5.universe.translation;

import java.util.List;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ShardedDatasetAttributes;
import org.janelia.saalfeldlab.n5.shard.Shard;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataWriter;

import com.google.gson.Gson;

public class TranslatedN5Writer extends TranslatedN5Reader implements N5Writer {

	protected N5Writer writer;

	protected ContainerMetadataWriter containerWriter;

	public TranslatedN5Writer( N5Writer n5Base, Gson gson, String fwdTranslation, String invTranslation) {
		super(n5Base, gson, fwdTranslation, invTranslation);

		this.writer = n5Base;
		containerWriter = new ContainerMetadataWriter( n5Base, translation.getTranslated() );
	}

	public N5Writer getBaseWriter() {
		return writer;
	}

	@Override
	public <T> void setAttribute( final String pathName, final String key, final T attribute) {
		translation.setTranslatedAttribute( pathName, key, attribute );
		containerWriter.setMetadataTree(translation.getOrig());
		containerWriter.writeAllAttributes();
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes) {
		translation.setTranslatedAttributes(pathName, attributes);
		containerWriter.setMetadataTree(translation.getOrig());
		containerWriter.writeAllAttributes();
	}

	@Override
	public void createGroup(String pathName) {
		translation.getTranslated().createGroup(pathName);
		translation.updateOriginal();
		writer.createGroup(originalPath(pathName));
	}

	@Override
	public boolean remove(String pathName) {
		final boolean success = writer.remove(originalPath(pathName));
		if( success ) {
			translation.getTranslated().remove(pathName);
			translation.updateOriginal();
		}
		return success;
	}

	@Override
	public boolean remove() {
		final boolean success = writer.remove();
		if( success ) {
			translation.rootOrig = new ContainerMetadataNode();
			translation.rootTranslated = new ContainerMetadataNode();
		}
		return success;
	}

	@Override
	public boolean removeAttribute(String pathName, String key) {

		final ContainerMetadataNode tlated = translation.getTranslated();
		if (tlated.removeAttribute(pathName, key)) {
			translation.updateOriginal();
			return true;
		}
		return false;
	}

	@Override
	public <T> T removeAttribute(String pathName, String key, Class<T> clazz) {

		final ContainerMetadataNode tlated = translation.getTranslated();
		final T t = tlated.removeAttribute(pathName, key, clazz);
		if (t != null) {
			translation.updateOriginal();
			return t;
		}
		return null;
	}

	@Override
	public boolean removeAttributes(String pathName, List<String> attributes) {

		final ContainerMetadataNode tlated = translation.getTranslated();
		if (tlated.removeAttributes(pathName, attributes)) {
			translation.updateOriginal();
			return true;
		}
		return false;
	}

	@Override
	public <T> void writeBlock(String pathName, DatasetAttributes datasetAttributes, DataBlock<T> dataBlock) {
		writer.writeBlock(originalPath(pathName), datasetAttributes, dataBlock);
	}

	@Override
	public boolean deleteBlock(String pathName, long... gridPosition) {
		return writer.deleteBlock(originalPath(pathName), gridPosition);
	}

	@Override
	public <T> void writeShard(String datasetPath, ShardedDatasetAttributes datasetAttributes, Shard<T> shard) throws N5Exception {

		throw new N5Exception("not yet implemented");
	}

}

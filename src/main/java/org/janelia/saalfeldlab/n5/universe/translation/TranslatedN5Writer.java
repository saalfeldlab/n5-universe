package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataWriter;

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
	public <T> void setAttribute( final String pathName, final String key, final T attribute) throws IOException {
		translation.setTranslatedAttribute( pathName, key, attribute );
		containerWriter.setMetadataTree(translation.getOrig());
		containerWriter.writeAllAttributes();
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes) throws IOException {
		translation.setTranslatedAttributes(pathName, attributes);
		containerWriter.setMetadataTree(translation.getOrig());
		containerWriter.writeAllAttributes();
	}

	@Override
	public void createGroup(String pathName) throws IOException {
		translation.getTranslated().createGroup(pathName);
		translation.updateOriginal();
		writer.createGroup(originalPath(pathName));
	}

	@Override
	public boolean remove(String pathName) throws IOException {
		boolean success = writer.remove(originalPath(pathName));
		if( success ) {
			translation.getTranslated().remove(pathName);
			translation.updateOriginal();
		}
		return success;
	}

	@Override
	public boolean remove() throws IOException {
		boolean success = writer.remove();
		if( success ) {
			translation.rootOrig = new ContainerMetadataNode();
			translation.rootTranslated = new ContainerMetadataNode();
		}
		return success;
	}

	@Override
	public <T> void writeBlock(String pathName, DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException {
		writer.writeBlock(originalPath(pathName), datasetAttributes, dataBlock);
	}

	@Override
	public boolean deleteBlock(String pathName, long... gridPosition) throws IOException {
		return writer.deleteBlock(originalPath(pathName), gridPosition);
	}

}

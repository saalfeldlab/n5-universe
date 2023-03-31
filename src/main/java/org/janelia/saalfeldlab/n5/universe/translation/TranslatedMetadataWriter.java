package org.janelia.saalfeldlab.n5.universe.translation;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;

public class TranslatedMetadataWriter<T extends N5Metadata> extends TreeTranslation implements N5MetadataWriter<T>{

	private N5Writer n5;
	
	private ContainerMetadataWriter containerWriter;
	
	private final N5MetadataWriter<T> metaWriter; 

	public TranslatedMetadataWriter(
			final N5Writer n5,
			final String dataset,
			final String translation,
			final N5MetadataWriter<T> metaWriter ) {
		super(	ContainerMetadataNode.build(n5, dataset, JqUtils.buildGson(n5) ),
				JqUtils.buildGson(n5), 
				translation );

		this.n5 = n5;
		this.metaWriter = metaWriter;
		containerWriter = new ContainerMetadataWriter( n5, this.rootTranslated );
	}
	
	public TranslatedMetadataWriter(
			final N5Writer n5,
			final String translation,
			final N5MetadataWriter<T> metaWriter ) {

		this( n5, "", translation, metaWriter );
	}

	@Override
	public void writeMetadata(T t, N5Writer n5, String path) throws Exception {
		this.n5 = n5;
		metaWriter.writeMetadata(t, getOrig(), path);
		updateTranslated();

		containerWriter.setN5Writer(n5);
		containerWriter.setMetadataTree(rootTranslated);
		containerWriter.writeAllAttributes();
	}

}

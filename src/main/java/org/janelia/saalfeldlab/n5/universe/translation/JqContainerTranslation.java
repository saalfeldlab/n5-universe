package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;

public class JqContainerTranslation extends JqFunction<ContainerMetadataNode,ContainerMetadataNode> 
	implements ContainerTranslation {

	public JqContainerTranslation(String translation, Gson gson ) {
		super(translation, gson, ContainerMetadataNode.class );
	}

}

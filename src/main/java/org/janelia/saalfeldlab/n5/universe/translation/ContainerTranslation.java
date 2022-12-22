package org.janelia.saalfeldlab.n5.universe.translation;

import java.util.function.Function;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;

public interface ContainerTranslation extends Function<ContainerMetadataNode,ContainerMetadataNode>{
	
}


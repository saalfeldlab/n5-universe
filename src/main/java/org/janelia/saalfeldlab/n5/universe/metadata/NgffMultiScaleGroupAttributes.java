package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;

public class NgffMultiScaleGroupAttributes {
	
	public MultiscaleDataset[] datasets;
	
	public static class MultiscaleDataset {

		public String path;
		public CosemTransform transform;
	}

}

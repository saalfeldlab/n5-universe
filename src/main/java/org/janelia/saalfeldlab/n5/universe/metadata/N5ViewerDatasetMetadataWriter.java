package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.N5Writer;

public class N5ViewerDatasetMetadataWriter implements N5MetadataWriter< N5SingleScaleMetadata > {

	@Override
	public void writeMetadata(N5SingleScaleMetadata t, N5Writer n5, String path) throws Exception {

		n5.setAttribute(path, N5SingleScaleMetadataParser.PIXEL_RESOLUTION_KEY, 
			new FinalVoxelDimensions( t.unit(), t.getPixelResolution()));

		n5.setAttribute(path, N5SingleScaleMetadataParser.DOWNSAMPLING_FACTORS_KEY,
				t.getDownsamplingFactors());
	}

}

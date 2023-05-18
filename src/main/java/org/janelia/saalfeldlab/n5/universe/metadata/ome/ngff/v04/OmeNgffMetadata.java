package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;

public class OmeNgffMetadata extends SpatialMultiscaleMetadata<N5SingleScaleMetadata> 
{
	public final OmeNgffMultiScaleMetadata[] multiscales;

	public OmeNgffMetadata( final String path,
			final OmeNgffMultiScaleMetadata[] multiscales)
	{
		// assumes children metadata are the same for all multiscales, which should be true
		super(path, multiscales[0].childrenMetadata); 
		this.multiscales = multiscales;
	}

}

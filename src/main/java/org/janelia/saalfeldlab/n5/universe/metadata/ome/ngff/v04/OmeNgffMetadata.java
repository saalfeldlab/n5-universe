package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import org.janelia.saalfeldlab.n5.universe.metadata.MultiscaleMetadata;

public class OmeNgffMetadata extends MultiscaleMetadata<NgffSingleScaleAxesMetadata>
{
	public final OmeNgffMultiScaleMetadata[] multiscales;

	public OmeNgffMetadata( final String path, final OmeNgffMultiScaleMetadata[] multiscales)
	{
		// assumes children metadata are the same for all multiscales, which should be true
		super(path, multiscales[0].getChildrenMetadata());
		this.multiscales = multiscales;
	}

}

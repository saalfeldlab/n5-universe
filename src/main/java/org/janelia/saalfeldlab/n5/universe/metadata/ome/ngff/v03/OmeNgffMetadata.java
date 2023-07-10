package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;

public class OmeNgffMetadata implements N5Metadata
{
	protected final OmeNgffMultiScaleMetadata[] multiscales;

	protected transient final String path;

	public OmeNgffMetadata( final String path,
			final OmeNgffMultiScaleMetadata[] multiscales )
	{
		this.path = path;
		this.multiscales = multiscales;
	}

	@Override
	public String getPath() {
		return path;
	}

	public OmeNgffMultiScaleMetadata[] getMultiscales() {
		return multiscales;
	}

}

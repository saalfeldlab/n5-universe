package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

/**
 * Abstract class for single-scale or multi-scale N5 metadata.
 */
public abstract class AbstractN5DatasetMetadata extends AbstractN5Metadata implements N5DatasetMetadata
{

  private DatasetAttributes attributes;

  public AbstractN5DatasetMetadata(final String path, final DatasetAttributes attributes) {

	super(path);
	this.attributes = attributes;
  }

  @Override
  public DatasetAttributes getAttributes() {

	return attributes;
  }

}

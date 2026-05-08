package org.janelia.saalfeldlab.n5.universe.metadata;

/**
 * Abstract class for single-scale or multi-scale N5 metadata.
 */
public abstract class AbstractN5Metadata implements N5Metadata
{

  private String path;

  public AbstractN5Metadata(final String path) {

	this.path = path;
  }

  @Override
  public String getPath() {

	return path;
  }
}

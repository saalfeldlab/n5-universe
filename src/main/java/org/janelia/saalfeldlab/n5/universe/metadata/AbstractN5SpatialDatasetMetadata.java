package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

/**
 * Abstract class for single-scale or multi-scale N5 metadata.
 */
public abstract class AbstractN5SpatialDatasetMetadata extends AbstractN5DatasetMetadata implements N5SpatialDatasetMetadata {

  public AbstractN5SpatialDatasetMetadata(String path, DatasetAttributes attributes) {

	super(path, attributes);
  }
}

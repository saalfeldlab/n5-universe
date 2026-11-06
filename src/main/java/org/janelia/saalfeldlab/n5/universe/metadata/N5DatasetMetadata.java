package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

/**
 * Interface for metadata that corresponds to an N5 dataset.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface N5DatasetMetadata extends N5Metadata {

  /**
   * Return the dataset attributes if this metadata object represents a single dataset.
   *
   * @return the attributes
   */
  DatasetAttributes getAttributes();
}

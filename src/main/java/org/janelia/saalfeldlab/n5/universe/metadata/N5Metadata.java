package org.janelia.saalfeldlab.n5.universe.metadata;

/**
 * Base interfaces for metadata stored with N5.
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface N5Metadata {

  /**
   * @return the path to this metadata, with respect to the base of the container
   */
  String getPath();

  /**
   * @return the name of the dataset or group corresponding to
   */
  default String getName() {

	final String[] split = getPath().split("/");
	if (split.length == 0 && getPath().trim().equals("/")) {
	  return getPath().trim();
	}
	return split[split.length - 1];
  }
}

package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.N5Writer;

/**
 * Interface for writing metadata to an N5 container.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 *
 * @param <T> metadata type
 */
public interface N5MetadataWriter<T extends N5Metadata> {

	/**
	 * Writes metadata to a dataset or group in an N5 container.
	 * 
	 * @param t metadata
	 * @param n5 the n5 writer
	 * @param path the path relative to the container root of the group or dataset
	 * @throws Exception the exception
	 */
	void writeMetadata(T t, N5Writer n5, String path) throws Exception;
}

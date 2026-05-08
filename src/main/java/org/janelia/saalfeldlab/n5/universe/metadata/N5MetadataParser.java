package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.Optional;
import java.util.function.BiFunction;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;

/**
 * Interface for reading metadata from N5 containers.
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface N5MetadataParser<T extends N5Metadata> extends BiFunction<N5Reader, N5TreeNode, Optional<T>> {

	static DatasetAttributes parseDatasetAttributes(N5Reader n5, N5TreeNode node) {

		return n5.getDatasetAttributes(node.getPath());
	}

	/**
	 * Called by the {@link org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer} while discovering the N5 tree and filling the metadata for datasets or groups.
	 * <p>
	 * The metadata parsing is done in the bottom-up fashion, so the children of the given {@code node} have already been processed and should already contain valid metadata (if any).
	 *
	 * @param n5
	 *            the reader
	 * @param node
	 *            list of tree nodes
	 * @return the metadata
	 */
	Optional<T> parseMetadata(final N5Reader n5, final N5TreeNode node);

	default Optional<T> parseMetadata(final N5Reader n5, final String dataset) {

		if (!n5.exists(dataset))
			return Optional.empty();
		return parseMetadata(n5, new N5TreeNode(dataset));
	}

	@Override
	default Optional<T> apply(N5Reader n5Reader, N5TreeNode n5TreeNode) {

		return parseMetadata(n5Reader, n5TreeNode);
	}
}

package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;

/**
 * Janelia COSEM's implementation of a {@link MultiscaleMetadata}.
 * 
 * @see <a href="https://www.janelia.org/project-team/cosem">https://www.janelia.org/project-team/cosem</a>
 * 
 * @author John Bogovic
 */
public class N5CosemMultiScaleMetadata extends SpatialMultiscaleMetadata<N5CosemMetadata> {

  public N5CosemMultiScaleMetadata(String basePath, N5CosemMetadata[] childMetadata) {

	super(basePath, childMetadata);
  }

  public static class CosemMultiScaleParser implements N5MetadataParser<N5CosemMultiScaleMetadata> {

	/**
	 * Called by the {@link N5DatasetDiscoverer}
	 * while discovering the N5 tree and filling the metadata for datasets or groups.
	 *
	 * @param node the node
	 * @return the metadata
	 */
	@Override public Optional<N5CosemMultiScaleMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

	  final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();

	  for (final N5TreeNode childNode : node.childrenList()) {
		if (scaleLevelPredicate.test(childNode.getNodeName()) && childNode.isDataset() && childNode.getMetadata() instanceof N5CosemMetadata) {
		  scaleLevelNodes.put(childNode.getNodeName(), childNode);
		}
	  }

	  if (scaleLevelNodes.isEmpty())
		return Optional.empty();

	  final N5CosemMetadata[] childMetadata = scaleLevelNodes.values().stream().map(N5TreeNode::getMetadata).toArray(N5CosemMetadata[]::new);
	  if (!sortScaleMetadata(childMetadata)) {
		return Optional.empty();
	  }
	  //TODO parse group attributes also;
	  return Optional.of(new N5CosemMultiScaleMetadata(node.getPath(), childMetadata));
	}
  }

}

/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.universe;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;

/**
 * A node representing a dataset or group in an N5 container,
 * and stores its corresponding {@link N5Metadata}, and
 * child nodes if any exist.
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 *
 */
public class N5TreeNode {

  private final String path;

  private N5Metadata metadata;

  private final ArrayList<N5TreeNode> children;

  public N5TreeNode(final String path) {

	this.path = path.trim();
	children = new ArrayList<>();
  }

  public static Stream<N5TreeNode> flattenN5Tree(N5TreeNode root) {

	return Stream.concat(
			Stream.of(root),
			root.childrenList().stream().flatMap(N5TreeNode::flattenN5Tree));
  }

  public String getNodeName() {

	return Paths.get(removeLeadingSlash(path)).getFileName().toString();
  }

  public String getParentPath() {

	return Paths.get(removeLeadingSlash(path)).getParent().toString();
  }

  /**
   * Adds a node as a child of this node.
   *
   * @param child the child node
   */
  public void add(final N5TreeNode child) {

	children.add(child);
  }

  public void remove(final N5TreeNode child) {

	children.remove(child);
  }

  public void removeAllChildren() {

	children.clear();
  }

  public List<N5TreeNode> childrenList() {

	return children;
  }

  public Optional<N5TreeNode> getDescendant( String path ) {

	  return getDescendants( x -> x.getPath().endsWith(path)).findFirst();
  }

  /**
   * Adds a node at the specified full path and any parent nodes along the path,
   * if they do not already exist. Returns the node at the specified path.
   *
   * @param path the full path to node
   * @return the node
   */
  public N5TreeNode addPath( final String path ) {
	  return addPath( path, x -> new N5TreeNode( x ));
  }

  /**
   * Adds a node at the specified full path and any parent nodes along the path,
   * if they do not already exist. Returns the node at the specified path.
   *
   * @param path the full path to node
   * @param constructor function creating a node from a path
   * @return the node
   */
  public N5TreeNode addPath( final String path, Function<String, N5TreeNode> constructor ) {

	  final String normPath = removeLeadingSlash(path);
	  if( !getPath().isEmpty() && !normPath.startsWith(getPath()))
		  return null;

	  if( this.path.equals(normPath))
		  return this;

	  final String relativePath = removeLeadingSlash( normPath.replaceAll(this.path, ""));
	  final int sepIdx = relativePath.indexOf("/");
	  final String childName;
	  if( sepIdx < 0 )
		  childName = relativePath;
	  else
		  childName = relativePath.substring(0, sepIdx);

	  // get the appropriate child along the path if it exists, otherwise add it
	  N5TreeNode child = null;
	  Stream<N5TreeNode> cs = children.stream().filter( n -> n.getNodeName().equals(childName));
	  Optional<N5TreeNode> copt = cs.findFirst();
	  if( copt.isPresent() )
		  child = copt.get();
	  else {
		  child = constructor.apply( this.path.isEmpty() ? childName : this.path + "/" + childName );
		  add( child );
	  }
	  return child.addPath(normPath);
  }

  public Stream<N5TreeNode> getDescendants( Predicate<N5TreeNode> filter ) {

	  return N5TreeNode.flattenN5Tree(this).filter( filter );
  }

  public boolean isDataset() {

	return Optional.ofNullable(getMetadata()).map(N5DatasetMetadata.class::isInstance).orElse(false);
  }

  public void setMetadata(final N5Metadata metadata) {

	this.metadata = metadata;
  }

  public N5Metadata getMetadata() {

	return metadata;
  }

  public String getPath() {

	return path;
  }

  @Override
  public String toString() {

	final String nodeName = getNodeName();
	return nodeName.isEmpty() ? "/" : nodeName;
  }

  public boolean structureEquals( N5TreeNode other )
  {
	  final boolean samePath = getPath().equals(other.getPath());
	  if( !samePath )
		  return false;

	  boolean childrenEqual = true;
	  for( N5TreeNode c : childrenList()) {
		  Optional<N5TreeNode> otherChildOpt = other.childrenList().stream()
			  .filter( x -> x.getNodeName().equals( c.getNodeName()))
			  .findFirst();

		 childrenEqual = childrenEqual &&
				 otherChildOpt.map( x -> x.structureEquals(c))
				 .orElse(false);

		 if( !childrenEqual )
			 break;
	  }
	  return childrenEqual;
  }

  public String printRecursive() {

	return printRecursiveHelper(this, "");
  }

  private static String printRecursiveHelper(N5TreeNode node, String prefix) {

	StringBuffer out = new StringBuffer();
	out.append(prefix + node.path + "\n");
	for (N5TreeNode c : node.childrenList()) {
	  System.out.println(c.path);
	  out.append(printRecursiveHelper(c, prefix + " "));
	}

	return out.toString();
  }

  /**
   * Generates a tree based on the output of {@link N5Reader#deepList}, returning the root node.
   *
   * @param base           the path used to call deepList
   * @param pathList       the output of deepList
   * @param groupSeparator the n5 group separator
   * @return the root node
   */
  public static N5TreeNode fromFlatList(final String base, final String[] pathList, final String groupSeparator) {

	final N5TreeNode root = new N5TreeNode(base);
	fromFlatList( root, pathList, groupSeparator );
	return root;
  }

  /**
   * Generates a tree based on the output of {@link N5Reader#deepList}, returning the root node.
   *
   * @param root           the root node corresponding to the base
   * @param pathList       the output of deepList
   * @param groupSeparator the n5 group separator
   */
  public static void fromFlatList(final N5TreeNode root, final String[] pathList, final String groupSeparator) {

	final HashMap<String, N5TreeNode> pathToNode = new HashMap<>();

	final String normalizedBase = normalDatasetName(root.getPath(), groupSeparator);
	pathToNode.put(normalizedBase, root);

	// sort the paths by length such that parent nodes always have smaller
	// indexes than their children
	Arrays.sort(pathList);

	final String prefix = normalizedBase == groupSeparator ? "" : normalizedBase;
	for (final String datasetPath : pathList) {

	  final String fullPath = prefix + groupSeparator + datasetPath;
	  final N5TreeNode node = new N5TreeNode(fullPath);
	  pathToNode.put(fullPath, node);

	  final String parentPath = fullPath.substring(0, fullPath.lastIndexOf(groupSeparator));

	  N5TreeNode parent = pathToNode.get(parentPath);
	  if (parent == null) {
		// possible for the parent to not appear in the list
		// if deepList is called with a filter
		parent = new N5TreeNode(parentPath);
		pathToNode.put(parentPath, parent);
	  }
	  parent.add(node);
	}
  }

  private static String normalDatasetName(final String fullPath, final String groupSeparator) {

	return fullPath.replaceAll("(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
  }

  /**
   * Removes the leading slash from a given path and returns the corrected path.
   * It ensures correctness on both Unix and Windows, otherwise {@code pathName} is treated
   * as UNC path on Windows, and {@code Paths.get(pathName, ...)} fails with {@code InvalidPathException}.
   *
   * @param pathName the path
   * @return the corrected path
   */
  protected static String removeLeadingSlash(final String pathName) {

	return pathName.startsWith("/") || pathName.startsWith("\\") ? pathName.substring(1) : pathName;
  }

}

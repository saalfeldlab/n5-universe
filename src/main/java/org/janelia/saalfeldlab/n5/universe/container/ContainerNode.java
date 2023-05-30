package org.janelia.saalfeldlab.n5.universe.container;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author John Bogovic
 */

public class ContainerNode<T> {

	public static final char SEPARATOR = '/';

	protected String path;
	protected Map<String, ContainerNode<T>> children;
	protected HashMap<String, T> files;

	public ContainerNode() {
		files = new HashMap<String, T>();
		children = new HashMap<String,ContainerNode<T>  >();
		path = "";
	}

	public ContainerNode(final String path, final HashMap<String, T> files, final Map<String,ContainerNode<T> > children) {
		this.path = path;
		this.files = files;
		this.children = children;
	}

//	public ContainerNode(final JsonObject attributesJson, final Map<String, ContainerNode<T>> children) {
//		this.children = children;
//
//		files = new HashMap<>();
//		attributesJson.entrySet().forEach(e -> {
//			files.put(e.getKey(), e.getValue());
//		});
//	}

	public ContainerNode(final ContainerNode<T> other) {
		files = other.files;
		children = other.children;
	}
	
	public ContainerNode<T> create() {
		return new ContainerNode<T>();
	}

	public String getPath() {
		return path;
	}

	public HashMap<String, T> getFiles() {
		return files;
	}

	public Map<String,ContainerNode<T>> getChildren() {
		return children;
	}

	public Stream<ContainerNode<T>> getChildrenStream() {
		return children.entrySet().stream().map( e -> e.getValue() );
	}

	public Stream<ContainerNode<T>> flatten() {
		return Stream.concat(Stream.of(this), getChildrenStream().flatMap( ContainerNode::flatten ));
	}

	public Stream<ContainerNode<T>> flattenLeaves() {
		// probably not the fastest implementation,
		// but not worried about optimization yet
		return flatten().filter( x -> x.getChildren().isEmpty() );
	}

	public void addChild(String relativepath, ContainerNode<T> child) {
		child.setPath( path + "/" + relativepath );
		children.put(relativepath, child);
	}

	protected void setPath(final String path) {
		this.path = path;
	}

	/**
	 * Returns a stream of nodes for the given path.
	 *
	 * @param path full or relative path
	 * @return stream of nodes
	 */
	public Stream<ContainerNode<T>> pathToChild(final String path) {
		final String normPath = path.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");

		final String thisNodePath = getPath();
		String relativePath = normPath;
		if( !thisNodePath.isEmpty() && normPath.startsWith( thisNodePath )) {
			relativePath = normPath.replaceFirst( thisNodePath, "");
		}

		return Stream.concat(Stream.of(this), children.get(relativePath).pathToChild(relativePath));
	}

	public Stream<String> getChildPathsRecursive( String thisPath ) {
		return Stream.concat( Stream.of( thisPath ),
				this.children.keySet().stream().flatMap( k ->
						this.children.get(k).getChildPathsRecursive( thisPath + "/" + k )));
	}

	public Optional<ContainerNode<T>> getParent( final String path ) {
		final String normPath = path.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
		final int idx = normPath.lastIndexOf( SEPARATOR );
		if (idx >= 0) {
			final String parentPath = normPath.substring(0, idx);
			return getNode(parentPath);
		} else if( !path.isEmpty())
			return Optional.of(this);
		else
			return Optional.empty();
	}

	public Optional<ContainerNode<T>> getNode( final String path ) {

		final String normPath = path.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
		final String thisNodePath = getPath();

		if( normPath.startsWith( thisNodePath )) {
			return getChild( normPath.replaceFirst( thisNodePath, ""));
		}

		return Optional.empty();
	}

	public ContainerNode<T> childRelative(final String normRelativePath) {
		String childName = normRelativePath.substring( 0, normRelativePath.indexOf(SEPARATOR));
		if( children.containsKey(childName) )
			return children.get(childName);
		else
			return null;
	}

	public Optional<ContainerNode<T>> getChild(final String relativePath ) {
		if (relativePath.isEmpty())
			return Optional.of(this);

		final String normPath = relativePath.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
		final int i = normPath.indexOf(SEPARATOR);

		final String cpath;
		final String relToChild;
		if (i < 0) {
			cpath = normPath;
			relToChild = "";
		} else {
			final String[] pathSplit = normPath.split(""+SEPARATOR);
			final String[] relToChildList = new String[pathSplit.length - 1];

			cpath = pathSplit[0];
			System.arraycopy(pathSplit, 1, relToChildList, 0, relToChildList.length);
			relToChild = Arrays.stream(relToChildList).collect(Collectors.joining("/"));
		}

		final ContainerNode<T> c = children.get(cpath);
		if (c == null)
			return Optional.empty();
		else
			return c.getChild(relToChild);
	}
	
	public boolean exists(String pathName) {
		return getNode( pathName ).isPresent();
	}
	
	public boolean remove(String pathName) {
		if( exists(pathName))
		{
			final String normPath = pathName.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
			if (normPath.isEmpty()) {
				clear();
				return true;
			} else {
				final String name = pathName.substring(pathName.lastIndexOf("/") + 1);
				getParent(pathName).ifPresent(x -> x.children.remove(name));
				return !exists(pathName);
			}
		}
		else
			return false;
	}
	
	public void clear() {
		files.clear();
		children.clear();
	}

}
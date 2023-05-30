package org.janelia.saalfeldlab.n5.universe.container;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.LockedChannel;
import org.janelia.saalfeldlab.n5.N5URL;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 *
 * @author John Bogovic
 */

//public class MemoryKeyValueAccess<T extends MemoryKeyValueAccess<T>> implements KeyValueAccess {
public class MemoryKeyValueAccess implements KeyValueAccess {

	public static final char SEPARATOR = '/';

	protected ContainerNode<JsonElement> root;

	public MemoryKeyValueAccess() {
		root= new ContainerNode<>();
	}
	
	public ContainerNode<JsonElement> getRoot() {
		return root;
	}

//	public MemoryKeyValueAccess(final HashMap<String, JsonElement> attributes,
//			final Map<String, T> children) {
//		this.attributes = attributes;
//		this.children = children;
//	}
//
//	public MemoryKeyValueAccess(final JsonObject attributesJson,
//			final Map<String, T> children) {
//		this.children = children;
//
//		attributes = new HashMap<>();
//		attributesJson.entrySet().forEach( e -> {
//			attributes.put( e.getKey(), e.getValue());
//		});
//	}
//
//	public MemoryKeyValueAccess( final T other ) {
//		attributes = other.attributes;
//		children = other.children;
//	}
	
//	@SuppressWarnings("unchecked")
//	public T create() {
//		return (T)new MemoryKeyValueAccess<T>();
//	}

//	public HashMap<String, JsonElement> getContainerAttributes() {
//		return attributes;
//	}
//
//	public Map<String, T> getChildren() {
//		return children;
//	}
//
//	public Stream<T> getChildrenStream() {
//		return children.entrySet().stream().map( e -> e.getValue() );
//	}
//
//	public Stream<MemoryKeyValueAccess<T>> flatten() {
//		return Stream.concat(Stream.of(this), getChildrenStream().flatMap( MemoryKeyValueAccess::flatten ));
//	}
//
//	public Stream<MemoryKeyValueAccess<T>> flattenLeaves() {
//		// probably not the fastest implementation,
//		// but not worried about optimization yet
//		return flatten().filter( x -> x.getChildren().isEmpty() );
//	}
//
//
//	public void addChild(String relativepath, T child) {
//		children.put(relativepath, child);
//	}

//	/**
//	 * Returns a stream of nodes for the given path.
//	 *
//	 * @param path full or relative path
//	 * @return stream of nodes
//	 */
//	public Stream<MemoryKeyValueAccess<T>> pathToChild(final String path) {
//		final String normPath = path.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
//
//		final String thisNodePath = getPath();
//		String relativePath = normPath;
//		if( !thisNodePath.isEmpty() && normPath.startsWith( thisNodePath )) {
//			relativePath = normPath.replaceFirst( thisNodePath, "");
//		}
//
//		return Stream.concat(Stream.of(this), children.get(relativePath).pathToChild(relativePath));
//	}
//
//	/**
//	 * @return the path to this node from the root.
//	 */
//	public String getPath() {
//		if( attributes.containsKey("path"))
//			return attributes.get("path").getAsString();
//		else
//			return "";
//	}
//
//	public Stream<String> getChildPathsRecursive( String thisPath ) {
//		return Stream.concat( Stream.of( thisPath ),
//				this.children.keySet().stream().flatMap( k ->
//						this.children.get(k).getChildPathsRecursive( thisPath + "/" + k )));
//	}
//
//	/**
//	 * Adds path attributes to this node and recursively to its children.
//	 */
//	public void addPathsRecursive() {
//		addPathsRecursive(getPath());
//	}
//
//	/**
//	 * Adds path attributes to this node and recursively to its children.
//	 *
//	 * @param thisPath path to a node
//	 */
//	public void addPathsRecursive( String thisPath ) {
//		attributes.put("path", new JsonPrimitive( thisPath ));
//		for ( String childPath : children.keySet() )
//			children.get(childPath).addPathsRecursive( thisPath + "/" + childPath );
//	}
//
//	public Optional<MemoryKeyValueAccess<T>> getParent( final String path ) {
//		final String normPath = path.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
//		final int idx = normPath.lastIndexOf( SEPARATOR );
//		if (idx >= 0) {
//			final String parentPath = normPath.substring(0, idx);
//			return getNode(parentPath);
//		} else if( !path.isEmpty())
//			return Optional.of(this);
//		else
//			return Optional.empty();
//	}
//
//	public Optional<MemoryKeyValueAccess<T>> getNode( final String path ) {
//
//		final String normPath = path.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
//		final String thisNodePath = getPath();
//
//		if( normPath.startsWith( thisNodePath )) {
//			return getChild( normPath.replaceFirst( thisNodePath, ""));
//		}
//
//		return Optional.empty();
//	}


//	public T childRelative(final String normRelativePath) {
//		String childName = normRelativePath.substring( 0, normRelativePath.indexOf(SEPARATOR));
//		if( children.containsKey(childName) )
//			return children.get(childName);
//		else
//			return null;
//	}
//
//	public Optional<MemoryKeyValueAccess<T>> getChild(final String relativePath ) {
//		if (relativePath.isEmpty())
//			return Optional.of(this);
//
//		final String normPath = relativePath.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
//		final int i = normPath.indexOf(SEPARATOR);
//
//		final String cpath;
//		final String relToChild;
//		if (i < 0) {
//			cpath = normPath;
//			relToChild = "";
//		} else {
//			final String[] pathSplit = normPath.split(""+SEPARATOR);
//			final String[] relToChildList = new String[pathSplit.length - 1];
//
//			cpath = pathSplit[0];
//			System.arraycopy(pathSplit, 1, relToChildList, 0, relToChildList.length);
//			relToChild = Arrays.stream(relToChildList).collect(Collectors.joining("/"));
//		}
//
//		final T c = children.get(cpath);
//		if (c == null)
//			return Optional.empty();
//		else
//			return c.getChild(relToChild);
//	}

	@Override
	public boolean exists(String pathName) {
		return root.exists( pathName );
	}

	public boolean remove(String pathName) {
		if (exists(pathName)) {
			final String normPath = pathName.replaceAll("^(" + SEPARATOR + "*)|(" + SEPARATOR + "*$)", "");
			if (normPath.isEmpty()) {
				root.clear();
				return true;
			} else {
				final String name = pathName.substring(pathName.lastIndexOf("/") + 1);
				root.getParent(pathName).ifPresent(x -> x.children.remove(name));
				return !exists(pathName);
			}
		} else
			return false;
	}

//	public boolean remove() {
//		root.clear();
//		return true;
//	}

//	@SuppressWarnings("unchecked")
//	public static  <N extends GsonKeyValueReader & N5Reader > MemoryKeyValueAccess build(
//			final N5Reader n5, final String dataset, final Gson gson ) {
//		if (n5 instanceof GsonKeyValueReader) {
//			try {
//				return buildGson((N)n5, dataset, gson );
//			} catch (Exception e) {
//			}
//		}
//		else {
//			try {
//				return buildN5( n5, dataset, gson );
//			} catch (Exception e) {
//			}
//		}
//		return null;
//	}

//	public static MemoryKeyValueAccess build(final N5Reader n5, final Gson gson ) {
//		return build( n5, "", gson );
//	}
//
//	public static <N extends GsonKeyValueReader & N5Reader > MemoryKeyValueAccess buildGson(
//			final N n5, final String dataset, final Gson gson )
//			throws InterruptedException, ExecutionException {
//		String[] datasets;
//		N5TreeNode root;
//		try {
//			datasets = n5.deepList(dataset, Executors.newSingleThreadExecutor());
//			root = N5TreeNode.fromFlatList(dataset, datasets, "/");
//			final MemoryKeyValueAccess containerRoot = buildHelper(n5, root );
//			containerRoot.addPathsRecursive(dataset);
//			return containerRoot;
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

//	public static <N extends GsonKeyValueReader & N5Reader> MemoryKeyValueAccess buildHelper(final N n5, N5TreeNode baseNode ) {
//
//		final JsonObject attrs = n5.getAttributes(baseNode.getPath()).getAsJsonObject();
//		final List<N5TreeNode> children = baseNode.childrenList();
//
//		final HashMap<String, MemoryKeyValueAccess> childMap = new HashMap<>();
//		for (N5TreeNode child : children)
//			childMap.put(child.getNodeName(), buildHelper(n5, child));
//
//		if ( attrs != null )
//			return new MemoryKeyValueAccess(attrs, childMap, n5.getGson());
//		else
//			return new MemoryKeyValueAccess(new HashMap<>(), childMap, n5.getGson());
//	}

//	public static <T extends N5Reader> MemoryKeyValueAccess buildN5(final T n5, final String dataset, final Gson gson )
//			throws InterruptedException, ExecutionException {
//		String[] datasets;
//		N5TreeNode root;
//		try {
//
//			datasets = n5.deepList(dataset, Executors.newSingleThreadExecutor());
//			root = N5TreeNode.fromFlatList(dataset, datasets, "/");
//			final MemoryKeyValueAccess containerRoot = buildHelperN5(n5, root, gson );
//			containerRoot.addPathsRecursive(dataset);
//			return containerRoot;
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

//	public static MemoryKeyValueAccess buildHelperN5(final N5Reader n5, N5TreeNode baseNode, Gson gson ) {
//		final Optional<HashMap<String, JsonElement>> attrs = getMetadataMapN5(n5, baseNode.getPath(), gson );
//		final List<N5TreeNode> children = baseNode.childrenList();
//
//		final HashMap<String, MemoryKeyValueAccess> childMap = new HashMap<>();
//		for (N5TreeNode child : children)
//			childMap.put(child.getNodeName(), buildHelperN5(n5, child, gson));
//
//		if (attrs.isPresent())
//			return new MemoryKeyValueAccess(attrs.get(), childMap, gson );
//		else
//			return new MemoryKeyValueAccess(new HashMap<>(), childMap, gson);
//	}

//	public static Optional<HashMap<String, JsonElement>> getMetadataMapN5(final N5Reader n5, final String dataset,
//			final Gson gson) {
//		try {
//			final HashMap<String, JsonElement> attrs = new HashMap<>();
//			Map<String, Class<?>> attrClasses = n5.listAttributes(dataset);
//			for (String k : attrClasses.keySet()) {
//
//				if( attrClasses.get(k).equals(String.class)) {
//
//					String s = n5.getAttribute(dataset, k, String.class );
//					Optional<JsonObject> elem = stringToJson( s, gson );
//					if( elem.isPresent())
//						attrs.put( k, elem.get());
//					else
//						attrs.put( k, gson.toJsonTree( s ));
//				}
//				else
//					attrs.put(k, gson.toJsonTree(n5.getAttribute(dataset, k, attrClasses.get(k))));
//			}
//
//			if (attrs != null)
//				return Optional.of(attrs);
//
//		} catch (Exception e) {
//		}
//		return Optional.empty();
//	}

	public static Optional<JsonObject> stringToJson(String s, final Gson gson) {

		try {
			JsonObject elem = gson.fromJson(s, JsonObject.class);
			return Optional.of(elem);
		} catch (JsonSyntaxException e) {
			return Optional.empty();
		}
	}

	@Override
	public String[] components(String path) {
		return normalize(path).split("/");
	}

	@Override
	public String compose(String... components) {
		return Arrays.stream(components).collect(Collectors.joining("/"));
	}
	
	public String leaf(String path) {
		final String normPath = normalize(path);
		return normPath.substring(normPath.lastIndexOf(SEPARATOR)+1);
	}

	@Override
	public String parent(String path) {
		final String normPath = normalize(path);
		return normPath.substring(0, normPath.lastIndexOf(SEPARATOR));
	}

	@Override
	public String relativize(String path, String base) {
		return null;
	}

	@Override
	public String normalize(String path) {
		return N5URL.normalizeGroupPath(path);
	}

	@Override
	public URI uri(String normalPath) throws URISyntaxException {
//		return new URI("n5mem", null, normalPath, null);
		throw new UnsupportedOperationException("uri not supported by MemoryKeyValueAccess");
	}

	@Override
	public boolean isDirectory(String normalPath) {
		return root.getNode(normalPath).isPresent();
	}

	@Override
	public boolean isFile(String normalPath) {

		return root.getParent(normalPath).map(x -> {
			return x.getFiles().containsKey(leaf(normalPath));
		}).orElse(false);
	}

	@Override
	public LockedChannel lockForReading(String normalPath) throws IOException {
		throw new UnsupportedOperationException("lockForReading not supported by MemoryKeyValueAccess");
	}

	@Override
	public LockedChannel lockForWriting(String normalPath) throws IOException {
		throw new UnsupportedOperationException("lockForWriting not supported by MemoryKeyValueAccess");
	}

	@Override
	public String[] listDirectories(String normalPath) throws IOException {
		final Optional<ContainerNode<JsonElement>> node = root.getNode(normalPath);
		if (node.isPresent()) {
			Set<String> set = node.get().children.keySet();
			return set.toArray(new String[set.size()]);
		} else
			return new String[] {};
	}

	@Override
	public String[] list(String normalPath) throws IOException {

		return Streams.concat(
					Arrays.stream(listDirectories(normalPath)),
					root.getFiles().keySet().stream())
				.toArray(String[]::new);
	}

	@Override
	public void createDirectories(String normalPath) throws IOException {
		String[] components = components(normalPath);
		ContainerNode<JsonElement> current = root;
		for (String c : components) {
			if (current.getChildren().containsKey(c)) {
				current = current.getChildren().get(c);
			} else {
				final ContainerNode<JsonElement> child = root.create();
				current.getChildren().put(c, child);
				current = child;
			}
		}
	}

	@Override
	public void delete(String normalPath) throws IOException {
		final String name = normalPath.substring( normalPath.lastIndexOf("/") + 1 );
		root.getParent( normalPath ).ifPresent( x -> x.children.remove(name));
	}

}
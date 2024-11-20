package org.janelia.saalfeldlab.n5.universe.container;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.GsonN5Writer;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.ShardedDatasetAttributes;
import org.janelia.saalfeldlab.n5.shard.Shard;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class ContainerMetadataNode implements GsonN5Writer {

	protected String path;
	protected HashMap<String, JsonElement> attributes;
	protected Map<String, ContainerMetadataNode> children;
	protected final transient Gson gson;

	public ContainerMetadataNode() {
		gson = JqUtils.buildGson(null);
		attributes = new HashMap<String, JsonElement>();
		children = new HashMap<String, ContainerMetadataNode >();
		path = "";
		addPathsRecursive();
	}

	public ContainerMetadataNode(final HashMap<String, JsonElement> attributes,
			final Map<String, ContainerMetadataNode> children, final Gson gson) {
		this.attributes = attributes;
		this.children = children;
		this.gson = gson;
	}

	public ContainerMetadataNode(final JsonObject attributes,
			final Map<String, ContainerMetadataNode> children, final Gson gson) {
		this.attributes = gson.fromJson(attributes,
				TypeToken.getParameterized( HashMap.class, String.class, JsonElement.class ).getType());
		this.children = children;
		this.gson = gson;
	}

	public ContainerMetadataNode( final ContainerMetadataNode other ) {
		gson = other.gson;
		attributes = other.attributes;
		children = other.children;
	}

	public HashMap<String, JsonElement> getContainerAttributes() {
		return attributes;
	}

	@Override
	public JsonElement getAttributes(final String pathName) throws N5Exception.N5IOException {

		final String groupPath = N5URI.normalizeGroupPath(pathName);
		final Optional<ContainerMetadataNode> nodeOpt = getNode(groupPath);
		if( nodeOpt.isPresent() )
		{
			final ContainerMetadataNode node = nodeOpt.get();
			return gson.toJsonTree( node.getContainerAttributes() );
		}
		return null; // TODO is this correct?
	}

	public Map<String, ContainerMetadataNode> getChildren() {
		return children;
	}

	public Stream<ContainerMetadataNode> getChildrenStream() {
		return children.entrySet().stream().map( e -> e.getValue() );
	}

	public Stream<ContainerMetadataNode> flatten() {
		return Stream.concat(Stream.of(this), getChildrenStream().flatMap( ContainerMetadataNode::flatten ));
	}

	public Stream<ContainerMetadataNode> flattenLeaves() {
		// probably not the fastest implementation,
		// but not worried about optimization yet
		return flatten().filter( x -> x.getChildren().isEmpty() );
	}


	public void addChild(String relativepath, ContainerMetadataNode child) {
		children.put(relativepath, child);
	}

	/**
	 * Returns a stream of nodes for the given path.
	 *
	 * @param path full or relative path
	 * @return stream of nodes
	 */
	public Stream<ContainerMetadataNode> pathToChild(final String path) {
		final String groupSeparator = "/";
		final String normPath = path.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");

		final String thisNodePath = getPath();
		String relativePath = normPath;
		if( !thisNodePath.isEmpty() && normPath.startsWith( thisNodePath )) {
			relativePath = normPath.replaceFirst( thisNodePath, "");
		}

		return Stream.concat(Stream.of(this), children.get(relativePath).pathToChild(relativePath));
	}

	/**
	 * @return the path to this node from the root.
	 */
	public String getPath() {
		return path;
	}

	public Stream<String> getChildPathsRecursive( String thisPath ) {
		return Stream.concat( Stream.of( thisPath ),
				this.children.keySet().stream().flatMap( k ->
						this.children.get(k).getChildPathsRecursive( thisPath + "/" + k )));
	}

	/**
	 * Adds path attributes to this node and recursively to its children.
	 */
	public void addPathsRecursive() {
		addPathsRecursive(getPath());
	}

	/**
	 * Adds path attributes to this node and recursively to its children.
	 *
	 * @param thisPath path to a node
	 */
	public void addPathsRecursive( String thisPath ) {
		path = thisPath;
		for ( final String childPath : children.keySet() )
			children.get(childPath).addPathsRecursive( thisPath + "/" + childPath );
	}

	public Optional<ContainerMetadataNode> getParent( final String path ) {
		final String groupSeparator = "/";
		final String normPath = path.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final String parentPath = normPath.substring(0, normPath.lastIndexOf( groupSeparator ) );
		return getNode( parentPath );
	}

	public Optional<ContainerMetadataNode> getNode( final String path ) {

		final String groupSeparator = "/";
		final String normPath = path.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final String thisNodePath = getPath();

		if( normPath.startsWith( thisNodePath )) {
			return getChild( normPath.replaceFirst( thisNodePath, ""));
		}

		return Optional.empty();
	}

	public Optional<ContainerMetadataNode> getChild(final String relativePath ) {
		return getChild(relativePath, "/");
	}

	public ContainerMetadataNode childRelative(final String normRelativePath) {
		final String childName = normRelativePath.substring( 0, normRelativePath.indexOf('/'));
		if( children.containsKey(childName) )
			return children.get(childName);
		else
			return null;
	}

	public Optional<ContainerMetadataNode> getChild(final String relativePath, final String groupSeparator) {
		if (relativePath.isEmpty())
			return Optional.of(this);

		final String normPath = relativePath.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final int i = normPath.indexOf(groupSeparator);

		final String cpath;
		final String relToChild;
		if (i < 0) {
			cpath = normPath;
			relToChild = "";
		} else {
			final String[] pathSplit = normPath.split(groupSeparator);
			final String[] relToChildList = new String[pathSplit.length - 1];

			cpath = pathSplit[0];
			System.arraycopy(pathSplit, 1, relToChildList, 0, relToChildList.length);
			relToChild = Arrays.stream(relToChildList).collect(Collectors.joining("/"));
		}

		final ContainerMetadataNode c = children.get(cpath);
		if (c == null)
			return Optional.empty();
		else
			return c.getChild(relToChild, groupSeparator);
	}

	@Override
	public boolean exists(String pathName) {
		return getNode( pathName ).isPresent();
	}

	@Override
	public String[] list(String pathName) throws N5Exception.N5IOException {
		final Optional<ContainerMetadataNode> node = getNode(pathName);
		if( node.isPresent() ) {
			final Set<String> set = node.get().getChildren().keySet();
			return set.toArray( new String[ set.size() ]);
		}
		else
			return new String[]{};
	}

	@Override
	public <T> void setAttribute( final String pathName, final String key, final T attribute) {
		setAttributes(pathName, Collections.singletonMap(key, attribute));
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes) {
		final Type mapType = new TypeToken<HashMap<String, JsonElement>>(){}.getType();
		final JsonElement json = gson.toJsonTree(attributes);
		final HashMap<String, JsonElement> map = gson.fromJson(json, mapType);
		getNode( pathName ).ifPresent( x -> x.attributes.putAll(map) );
	}

	@Override
	public boolean removeAttribute(String pathName, String key) {
		final Optional<ContainerMetadataNode> node = getNode( pathName );
		if( node.isPresent() )
			return node.get().remove(key);

		return false;
	}

	@Override
	public <T> T removeAttribute(String pathName, String key, Class<T> clazz) {
		final Optional<ContainerMetadataNode> node = getNode(pathName);
		final T t = getAttribute(pathName, key, clazz);
		if (t != null) {
			node.get().remove(key);
		}
		return t;
	}

	@Override
	public boolean removeAttributes(String pathName, List<String> attributes) {
		boolean removed = false;
		for (final String attribute : attributes) {
			removed |= removeAttribute(pathName, attribute);
		}
		return removed;
	}

	@Override
	public void createGroup(String pathName) {
		final String groupSeparator = "/";
		final String normPath = pathName.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final String thisNodePath = getPath();

		final String relativePath;
		if( normPath.startsWith( thisNodePath ))
			relativePath = normPath.replaceFirst( thisNodePath, "" );
		else
			relativePath = normPath;

		final String[] parts = relativePath.split(groupSeparator);
		createGroupHelper( this, parts, 0 );
		addPathsRecursive();
	}

	private static void createGroupHelper( ContainerMetadataNode node, String[] parts, int i )
	{
		if( i >= parts.length )
			return;

		final String childRelpath = parts[i];
		ContainerMetadataNode child;
		if( !node.children.containsKey( childRelpath )) {
			child = new ContainerMetadataNode();
			node.addChild(childRelpath, child);
		}
		else {
			child = node.children.get(childRelpath);
		}

		createGroupHelper( child, parts, i+1 )	;
	}

	@Override
	public boolean remove(String pathName) {
		final String groupSeparator = "/";
		if( exists(pathName))
		{
			final String normPath = pathName.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
			if( normPath.isEmpty())
				return remove();
			else {
				final String name = pathName.substring( pathName.lastIndexOf("/") + 1 );
				getParent( pathName ).ifPresent( x -> x.children.remove(name));
				return !exists( pathName );
			}
		}
		else
			return false;
	}

	@Override
	public boolean remove() {
		attributes.clear();
		children.clear();
		return true;
	}

	@Override
	public <T> void writeBlock(String pathName, DatasetAttributes datasetAttributes, DataBlock<T> dataBlock) {
	}

	@Override
	public <T> void writeShard(String datasetPath, ShardedDatasetAttributes datasetAttributes, Shard<T> shard) throws N5Exception {
	}

	@Override
	public boolean deleteBlock(String pathName, long... gridPosition) {
		return false;
	}

	@Override
	public DataBlock<?> readBlock(String pathName, DatasetAttributes datasetAttributes, long... gridPosition) {
		return null;
	}



	@SuppressWarnings("unchecked")
	public static  <N extends GsonN5Reader & N5Reader > ContainerMetadataNode build(
			final N5Reader n5, final String dataset, final Gson gson ) {
		if (n5 instanceof GsonN5Reader) {
			try {
				return buildGson((N)n5, dataset, gson );
			} catch (final Exception e) {
			}
		}
		else {
			try {
				return buildN5( n5, dataset, gson );
			} catch (final Exception e) {
			}
		}
		return null;
	}

	public static ContainerMetadataNode build(final N5Reader n5, final Gson gson ) {
		return build( n5, "", gson );
	}

	public static <N extends GsonN5Reader & N5Reader > ContainerMetadataNode buildGson(
			final N n5, final String dataset, final Gson gson )
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {
			datasets = n5.deepList(dataset, Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList(dataset, datasets, "/");
			final ContainerMetadataNode containerRoot = buildHelper(n5, root );
			containerRoot.addPathsRecursive(dataset);
			return containerRoot;

		} catch (final N5Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <N extends GsonN5Reader> ContainerMetadataNode buildHelper(final N n5, N5TreeNode baseNode ) {

		final JsonElement attrsRaw = n5.getAttributes(baseNode.getPath());
		final JsonObject attrs = (attrsRaw != null && attrsRaw.isJsonObject() ) ? attrsRaw.getAsJsonObject() : new JsonObject();
		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (final N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelper(n5, child));

		if ( attrs != null )
			return new ContainerMetadataNode(attrs, childMap, n5.getGson());
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap, n5.getGson());
	}

	public static <T extends N5Reader> ContainerMetadataNode buildN5(final T n5, final String dataset, final Gson gson )
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {

			datasets = n5.deepList(dataset, Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList(dataset, datasets, "/");
			final ContainerMetadataNode containerRoot = buildHelperN5(n5, root, gson );
			containerRoot.addPathsRecursive(dataset);
			return containerRoot;

		} catch (final N5Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContainerMetadataNode buildHelperN5(final N5Reader n5, N5TreeNode baseNode, Gson gson ) {
		final Optional<HashMap<String, JsonElement>> attrs = getMetadataMapN5(n5, baseNode.getPath(), gson );
		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (final N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelperN5(n5, child, gson));

		if (attrs.isPresent())
			return new ContainerMetadataNode(attrs.get(), childMap, gson );
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap, gson);
	}

	public static Optional<HashMap<String, JsonElement>> getMetadataMapN5(final N5Reader n5, final String dataset,
			final Gson gson) {
		try {
			final HashMap<String, JsonElement> attrs = new HashMap<>();
			final Map<String, Class<?>> attrClasses = n5.listAttributes(dataset);
			for (final String k : attrClasses.keySet()) {

				if( attrClasses.get(k).equals(String.class)) {

					final String s = n5.getAttribute(dataset, k, String.class );
					final Optional<JsonObject> elem = stringToJson( s, gson );
					if( elem.isPresent())
						attrs.put( k, elem.get());
					else
						attrs.put( k, gson.toJsonTree( s ));
				}
				else
					attrs.put(k, gson.toJsonTree(n5.getAttribute(dataset, k, attrClasses.get(k))));
			}

			if (attrs != null)
				return Optional.of(attrs);

		} catch (final Exception e) {
		}
		return Optional.empty();
	}

	public static Optional<JsonObject> stringToJson(String s, final Gson gson) {

		try {
			final JsonObject elem = gson.fromJson(s, JsonObject.class);
			return Optional.of(elem);
		} catch (final JsonSyntaxException e) {
			return Optional.empty();
		}
	}

	@Override
	public String groupPath(String... nodes) {
		return Arrays.stream(nodes).collect( Collectors.joining(","));
	}

	@Override
	public Gson getGson() {
		return gson;
	}

	@Override
	public URI getURI() {
		throw new UnsupportedOperationException("getURI not supported by ContainerMetadataNode");
	}

	@Override
	public void setAttributes(String groupPath, JsonElement attributes) throws N5Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public String getAttributesKey() {

		return "attributes.json";
	}

}

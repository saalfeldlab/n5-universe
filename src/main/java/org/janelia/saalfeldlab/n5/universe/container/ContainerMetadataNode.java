package org.janelia.saalfeldlab.n5.universe.container;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5URL;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;

/**
 *
 * @author John Bogovic
 */

public class ContainerMetadataNode extends AbstractGsonReader implements N5Writer {
	protected JsonElement attributes;
	protected Map<String, ContainerMetadataNode> children;

	public ContainerMetadataNode() {
		super(JqUtils.gsonBuilder(null));
		attributes = new JsonObject();
		children = new HashMap<String, ContainerMetadataNode >();
		addPathsRecursive();
	}

	public ContainerMetadataNode(final JsonElement attributes,
			final Map<String, ContainerMetadataNode> children, final Gson gson) {
		super(JqUtils.newBuilder(gson));
		this.attributes = attributes;
		this.children = children;
	}

	public ContainerMetadataNode( ContainerMetadataNode other) {
		super(JqUtils.newBuilder(other.gson));
		attributes = other.attributes;
		children = other.children;
	}

	public HashMap<String, JsonElement> getAttributes() {
		if (!attributes.isJsonObject()) return new HashMap<>();
		final Type mapType = new TypeToken<HashMap<String, JsonElement>>(){}.getType();
		return gson.fromJson(attributes, mapType);
	}
	public JsonElement getAttributesJson() {
		return attributes;
	}

	public void setAttributesJson(JsonElement json) {
		attributes = json;
	}

	@Override public HashMap<String, JsonElement> getAttributes( String pathName ) {
		return getNode( pathName ).map(ContainerMetadataNode::getAttributes).orElse( new HashMap<>());
	}

	@Override public JsonElement getAttributesJson(String pathName) {

		return getNode( pathName ).map(ContainerMetadataNode::getAttributesJson).orElse( new JsonObject());
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
		if( attributes.isJsonObject() && attributes.getAsJsonObject().has("path"))
			return attributes.getAsJsonObject().get("path").getAsString();
		else
			return "";
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
		if (attributes.isJsonObject()) {
			attributes.getAsJsonObject().addProperty("path", thisPath );
		}
		for ( String childPath : children.keySet() )
			children.get(childPath).addPathsRecursive( thisPath + "/" + childPath );
	}

	public Optional<ContainerMetadataNode> getParent( final String path ) {
		final String groupSeparator = "/";
		final String normPath = path.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		if (!normPath.contains(groupSeparator)) return Optional.empty();
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

	public ContainerMetadataNode getOrCreateGroup(String pathName) {
		Optional<ContainerMetadataNode> childOpt = getNode(pathName);
		if (!childOpt.isPresent()) {
			createGroup(pathName);
			childOpt = getNode(pathName);
		}

		/* We create the group if it doesn't exist, so it should always exist at this point. */
		//noinspection OptionalGetWithoutIsPresent
		return childOpt.get();
	}

	public Optional<ContainerMetadataNode> getChild(final String relativePath ) {
		return getChild(relativePath, "/");
	}

	public ContainerMetadataNode childRelative(final String normRelativePath) {
		String childName = normRelativePath.substring( 0, normRelativePath.indexOf('/'));
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
	public String[] list(String pathName) throws IOException {
		Optional<ContainerMetadataNode> node = getNode(pathName);
		if( node.isPresent() ) {
			Set<String> set = node.get().children.keySet();
			return set.toArray( new String[ set.size() ]);
		}
		else
			return new String[]{};
	}

	@Override
	public <T> void setAttribute( final String pathName, final String key, final T attribute) {
		final ContainerMetadataNode node = getOrCreateGroup(pathName);

		node.attributes = GsonAttributesParser.insertAttribute(
				node.attributes,
				N5URL.normalizeAttributePath(key),
				attribute,
				gson
		);
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes) {
		final ContainerMetadataNode node = getOrCreateGroup(pathName);

		/* If we are setting attributes via map on a non-json-object, we overwrite the existing jsonElement*/
		if (!node.attributes.isJsonObject()) {
			node.setAttributesJson(new JsonObject());
		}
		final JsonObject currentAttributes = node.attributes.getAsJsonObject();
		attributes.forEach((key, value) -> currentAttributes.add(key, gson.toJsonTree(value)));
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

		String[] parts = relativePath.split(groupSeparator);
		createGroupHelper( this, parts, 0 );
		addPathsRecursive();
	}

	private static void createGroupHelper( ContainerMetadataNode node, String[] parts, int i )
	{
		if( i >= parts.length )
			return;

		String childRelpath = parts[i];
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
		attributes = new JsonObject();
		children.clear();
		return true;
	}

	@Override
	public <T> void writeBlock(String pathName, DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException {
	}

	@Override
	public boolean deleteBlock(String pathName, long... gridPosition) throws IOException {
		return false;
	}

	@Override
	public DataBlock<?> readBlock(String pathName, DatasetAttributes datasetAttributes, long... gridPosition)
			throws IOException {
		return null;
	}



	@SuppressWarnings("unchecked")
	public static  <N extends GsonAttributesParser & N5Reader > ContainerMetadataNode build(
			final N5Reader n5, final String dataset, final Gson gson ) {
		if (n5 instanceof GsonAttributesParser) {
			try {
				return buildGson((N)n5, dataset, gson );
			} catch (Exception e) {
			}
		}
		else {
			try {
				return buildN5( n5, dataset, gson );
			} catch (Exception e) {
			}
		}
		return null;
	}

	public static ContainerMetadataNode build(final N5Reader n5, final Gson gson ) {
		return build( n5, "", gson );
	}

	public static <N extends GsonAttributesParser & N5Reader > ContainerMetadataNode buildGson(
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

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <N extends GsonAttributesParser & N5Reader> ContainerMetadataNode buildHelper(final N n5, N5TreeNode baseNode ) {

		JsonElement attrs;
		try {
			attrs = n5.getAttributesJson(baseNode.getPath());
		} catch (IOException e) {
			attrs = new JsonObject();
		}

		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelper(n5, child));

		return new ContainerMetadataNode(attrs, childMap, n5.getGson());
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

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContainerMetadataNode buildHelperN5(final N5Reader n5, N5TreeNode baseNode, Gson gson ) {
		JsonElement attributeElement = new JsonObject();
		if (n5 instanceof GsonAttributesParser) {
			try {
				attributeElement = ((GsonAttributesParser)n5).getAttributesJson(baseNode.getPath());
			} catch (IOException e) {
			}
		} else {
			final Optional<HashMap<String, JsonElement>> attrs = getMetadataMapN5(n5, baseNode.getPath(), gson );
			if (attrs.isPresent()) {
				attrs.get().forEach(attributeElement.getAsJsonObject()::add);
			}
		}

		final List<N5TreeNode> children = baseNode.childrenList();
		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelperN5(n5, child, gson));

		return new ContainerMetadataNode(attributeElement, childMap, gson );
	}

	public static Optional<HashMap<String, JsonElement>> getMetadataMapN5(final N5Reader n5, final String dataset,
			final Gson gson) {
		try {
			final HashMap<String, JsonElement> attrs = new HashMap<>();
			Map<String, Class<?>> attrClasses = n5.listAttributes(dataset);
			for (String k : attrClasses.keySet()) {

				if( attrClasses.get(k).equals(String.class)) {

					String s = n5.getAttribute(dataset, k, String.class );
					Optional<JsonObject> elem = stringToJson( s, gson );
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

		} catch (Exception e) {
		}
		return Optional.empty();
	}

	public static Optional<JsonObject> stringToJson(String s, final Gson gson) {

		try {
			JsonObject elem = gson.fromJson(s, JsonObject.class);
			return Optional.of(elem);
		} catch (JsonSyntaxException e) {
			return Optional.empty();
		}
	}

}

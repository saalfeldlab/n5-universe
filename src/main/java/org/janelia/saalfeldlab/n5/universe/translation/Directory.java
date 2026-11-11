package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.GsonN5Writer;
import org.janelia.saalfeldlab.n5.HierarchyStore;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;

/**
 * A directory node: the attribute files it contains, and its directory-like
 * children.
 */
final class Directory {

	/**
	 * Maps name of attributes file to attributes file content.
	 */
	private Map<String, JsonElement> attributes;

	/**
	 * Maps path of directory-like child (relative to this {@code
	 * Directory}) to child {@code Directory} node.
	 */
	private Map<String, Directory> children;

	void putChild(final String name, final Directory child) {
		if (children == null) {
			children = new LinkedHashMap<>();
		}
		children.put(name, child);
	}

	private Directory getOrCreateChild(final String name) {
		if (children == null) {
			children = new LinkedHashMap<>();
		}
		return children.computeIfAbsent(name, k -> new Directory());
	}

	Directory getChild(final String name) {
		return children == null ? null : children.get(name);
	}

	void removeChild(final String name) {
		if (children != null) {
			children.remove(name);
		}
	}

	JsonElement getAttributes(final String name) {
		return attributes == null ? null : attributes.get(name);
	}

	void putAttributes(final String name, final JsonElement json) {
		if (attributes == null) {
			attributes = new LinkedTreeMap<>();
		}
		attributes.put(name, json);
	}

	String[] list() {
		return children == null ? new String[0] : children.keySet().toArray(new String[0]);
	}

	/**
	 * Get the {@code Directory} at {@code path}, relative to this one. An empty
	 * {@code path} refers to this {@code Directory}.
	 *
	 * @return the {@code Directory} at {@code path}, or {@code null} if it does not exist
	 */
	Directory getDirectory(final N5DirectoryPath path) {

		if (path.path().isEmpty())
			return this;
		Directory current = this;
		for (final String component : path.components()) {
			current = current.getChild(component);
			if (current == null)
				return null;
		}
		return current;
	}

	/**
	 * Get the {@code Directory} at {@code path}, relative to this one, creating
	 * it and every node on the way that does not exist yet. An empty {@code
	 * path} refers to this {@code Directory}.
	 *
	 * @return the {@code Directory} at {@code path}
	 */
	Directory getOrCreateDirectory(final N5DirectoryPath path) {

		if (path.path().isEmpty())
			return this;
		Directory current = this;
		for (final String component : path.components())
			current = current.getOrCreateChild(component);
		return current;
	}


	// -- harvest from GsonN5Reader --

	/**
	 * Harvest the hierarchy of {@code n5}, sequentially, on the calling thread.
	 *
	 * @see #collectFrom(GsonN5Reader, Executor)
	 */
	static Directory collectFrom(final GsonN5Reader n5) {

		return collectFrom(n5, null);
	}

	/**
	 * Harvest the hierarchy of {@code n5}: the attribute files of every
	 * directory, and its children, except below datasets (so chunks are never
	 * listed).
	 * <p>
	 * If {@code executor} is non-null, every directory is visited in a task on
	 * {@code executor}. The cost of harvesting is dominated by latency on cloud
	 * backends (a few store requests per directory), which this hides. No task
	 * ever waits for another, so any {@code Executor} works, including a
	 * single-threaded one. The calling thread waits for the result.
	 * <p>
	 * The resulting tree is the same with and without {@code executor},
	 * including the order of children (sorted by name).
	 *
	 * @param n5
	 * 		the container to harvest
	 * @param executor
	 * 		executor to visit directories on, or {@code null} to harvest
	 * 		sequentially on the calling thread
	 *
	 * @return the harvested hierarchy
	 */
	static Directory collectFrom(final GsonN5Reader n5, final Executor executor) {

		return collectFrom(n5, executor, false);
	}

	/**
	 * @param requireGroup
	 * 		if {@code true}, a directory that is neither a dataset nor a group is
	 * 		left out, with everything below it. Currently not used outside of
	 * 		tests.
	 *
	 * @see #collectFrom(GsonN5Reader, Executor)
	 */
	static Directory collectFrom(final GsonN5Reader n5, final Executor executor, final boolean requireGroup) {

		final ContainerDialect dialect = n5.getContainerDialect();
		final String[] attributeFileNames = DialectInfo.of(n5).attributeFileNames();
		final Supplier<Directory> harvest = executor == null
				? new SequentialHarvest(dialect, attributeFileNames, requireGroup)
				: new ParallelHarvest(dialect, attributeFileNames, requireGroup, executor);
		return harvest.get();
	}


	// -- write to GsonN5Writer --

	/**
	 * Write the tree rooted at {@code root} into {@code n5}: create a directory for
	 * every node (if it doesn't exist), and (over-)write every attributes file.
	 * <p>
	 * A {@code null} {@code root} means the hierarchy does not exist, so the
	 * container is removed.
	 * <p>
	 * Otherwise this only creates and overwrites. Directories and attribute
	 * files that exist in the container but not in the tree are left alone.
	 * <p>
	 * TODO: diff against the container and apply only the differences.
	 *
	 * @param root
	 * 		tree to write, or {@code null} to remove the container
	 * @param n5
	 * 		container to write to
	 */
	static void writeTo(final Directory root, final GsonN5Writer n5) {

		if (root == null) {
			n5.remove();
			return;
		}
		final ContainerDialect dialect = n5.getContainerDialect();
		final HierarchyStore store = dialect.getHierarchyStore();
		final Gson gson = dialect.getGson();
		root.writeTo(store, N5DirectoryPath.of(""), gson);
	}

	private void writeTo(final HierarchyStore store, final N5DirectoryPath path, final Gson gson) {

		store.createDirectories(path);
		if (attributes != null)
			attributes.forEach((filename, json) -> store.writeAttributesJson(path, filename, json, gson));
		if (children != null)
			children.forEach((name, child) -> child.writeTo(store, path.resolve(name).asDirectory(), gson));
	}


	// -- json de/serialization for jq translation --

	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String CHILDREN_KEY = "children";

	/**
	 * Serialize this {@code Directory} recursively
	 * (for feeding into a jq translation).
	 */
	JsonObject toJson() {

		final JsonObject attributesJson = new JsonObject();
		if (attributes != null)
			attributes.forEach(attributesJson::add);

		final JsonObject childrenJson = new JsonObject();
		if (children != null)
			children.forEach((name, child) -> childrenJson.add(name, child.toJson()));

		final JsonObject json = new JsonObject();
		json.add(ATTRIBUTES_KEY, attributesJson);
		json.add(CHILDREN_KEY, childrenJson);
		return json;
	}

	/**
	 * Deserialize a {@code Directory} recursively
	 * (for parsing the output of a jq translation).
	 */
	static Directory fromJson(final JsonElement json) {

		final Directory dir = new Directory();
		if (json == null || !json.isJsonObject())
			return dir;
		final JsonObject obj = json.getAsJsonObject();

		final JsonElement attributesJson = obj.get(ATTRIBUTES_KEY);
		if (attributesJson != null && attributesJson.isJsonObject()) {
			attributesJson.getAsJsonObject().asMap().forEach((name, attr) -> {
				if (!attr.isJsonNull())
					dir.putAttributes(name, attr);
			});
		}

		final JsonElement childrenJson = obj.get(CHILDREN_KEY);
		if (childrenJson != null && childrenJson.isJsonObject()) {
			childrenJson.getAsJsonObject().asMap().forEach((name, child) -> {
				if (!child.isJsonNull())
					dir.putChild(name, fromJson(child));
			});
		}

		return dir;
	}
}

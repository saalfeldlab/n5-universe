package org.janelia.saalfeldlab.n5.universe.kvr;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.janelia.saalfeldlab.n5.HierarchyStore;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5Exception.N5NoSuchKeyException;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;

/**
 * In-memory {@link HierarchyStore} over a {@link Directory} tree.
 * <p>
 * This is used by {@code TranslatedN5Reader/Writer} to hold the translated
 * hierarchy and present it to the {@code ContainerDialect}.
 * <p>
 * {@code DirectoryStore} is not thread-safe (unless for read-only use).
 */
class DirectoryStore implements HierarchyStore {

	private Directory root;

	public DirectoryStore(final Directory root) {
		this.root = root;
	}

	/**
	 * Get the root of the hierarchy, or {@code null} if the hierarchy does not
	 * exist (for example, because the root was removed).
	 */
	Directory getRoot() {
		return root;
	}

	void setRoot(final Directory root) {
		this.root = root;
	}


	// -- read --

	@Override
	public JsonElement readAttributesJson(
			final N5DirectoryPath parent,
			final String filename,
			final Gson gson) throws N5IOException {

		final Directory dir = getDirectory(parent);
		final JsonElement json = dir == null ? null : dir.getAttributes(filename);
		return json == null ? null : json.deepCopy();
	}

	@Override
	public boolean isDirectory(final N5DirectoryPath path) {

		return getDirectory(path) != null;
	}

	@Override
	public String[] listDirectories(final N5DirectoryPath path) throws N5IOException {

		final Directory dir = getDirectory(path);
		if (dir == null)
			throw new N5NoSuchKeyException("No such directory: " + path);
		return dir.list();
	}


	// -- write --

	@Override
	public void writeAttributesJson(
			final N5DirectoryPath parent,
			final String filename,
			final JsonElement attributes,
			final Gson gson) throws N5IOException {

		// Gson only filters out nulls when you write the JsonElement. This
		// means it doesn't filter them out when caching.
		// To handle this, we explicitly write the existing JsonElement to
		// a new JsonElement.
		// The output is identical to the input if:
		// - serializeNulls is true
		// - no null values are present
		final JsonElement json = gson.serializeNulls() ? attributes : gson.toJsonTree(attributes);

		getOrCreateDirectory(parent).putAttributes(filename, json);
	}

	@Override
	public void createDirectories(final N5DirectoryPath path) {

		getOrCreateDirectory(path);
	}

	@Override
	public void removeDirectory(final N5DirectoryPath path) {

		if (path.path().isEmpty()) {
			// removing the root: the hierarchy no longer exists
			root = null;
		} else {
			final Directory parentDir = getDirectory(path.parent());
			if (parentDir != null)
				parentDir.removeChild(path.filename());
		}
	}


	// -- helpers --

	/**
	 * Get the {@code Directory} at {@code path} (relative to root).
	 *
	 * @return the {@code Directory} at {@code path}, or {@code null} if it does not exist
	 */
	private Directory getDirectory(final N5DirectoryPath path) {

		return root == null ? null : root.getDirectory(path);
	}

	/**
	 * Get the {@code Directory} at {@code path} (relative to root).
	 * Create the {@code Directory} if it does not exist.
	 * <p>
	 * NB: This re-creates the root if it was removed, so writing to a removed
	 * hierarchy brings it back, like it would in a file-system container.
	 *
	 * @return the {@code Directory} at {@code path}
	 */
	private Directory getOrCreateDirectory(final N5DirectoryPath path) {

		if (root == null)
			root = new Directory();
		return root.getOrCreateDirectory(path);
	}

}

package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Arrays;
import java.util.function.Supplier;
import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.HierarchyStore;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;

/**
 * Harvests one container sequentially, on the calling thread: a plain
 * depth-first walk.
 * <p>
 * {@link ParallelHarvest} does the same in parallel. The two are kept as
 * separate implementations on purpose: this one is the readable reference for
 * what a harvest does, and the tests check that the other produces the same
 * tree. Use {@link Directory#collectFrom}, which picks one.
 */
final class SequentialHarvest implements Supplier<Directory> {

	private final ContainerDialect dialect;
	private final HierarchyStore store;
	private final Gson gson;
	private final String[] attributeFileNames;
	private final boolean requireGroup;

	SequentialHarvest(final ContainerDialect dialect,
			final String[] attributeFileNames,
			final boolean requireGroup) {

		this.dialect = dialect;
		this.store = dialect.getHierarchyStore();
		this.gson = dialect.getGson();
		this.attributeFileNames = attributeFileNames;
		this.requireGroup = requireGroup;
	}

	@Override
	public Directory get() {

		return collect(N5DirectoryPath.of(""));
	}

	/**
	 * Harvest the directory at {@code path} and everything below it.
	 *
	 * @return the {@code Directory}, or {@code null} if it is left out
	 * 		({@link #requireGroup})
	 */
	private Directory collect(final N5DirectoryPath path) {

		final Directory dir = new Directory();

		// harvest attribute files
		for (String filename : attributeFileNames) {
			final JsonElement attr = store.readAttributesJson(path, filename, gson);
			if (attr != null) {
				dir.putAttributes(filename, attr);
			}
		}

		// harvest children
		if (dialect.datasetExists(path)) {
			// The path is a dataset:
			// Do not recurse into subdirectories.
			return dir;
		} else if (requireGroup && !dialect.groupExists(path)) {
			// We are strict about every level of the hierarchy above datasets
			// being a proper group. And path is neither a dataset nor a group:
			// Do not store anything and do not recurse into subdirectories.
			return null;
		} else {
			final String[] list = store.listDirectories(path);
			Arrays.sort(list);
			for (final String name : list) {
				final Directory child = collect(path.resolve(name).asDirectory());
				if (child != null)
					dir.putChild(name, child);
			}
			return dir;
		}
	}
}

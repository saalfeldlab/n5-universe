package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.HierarchyStore;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;

/**
 * Harvests one container in parallel, on an {@code Executor}. Produces the
 * same tree as {@link SequentialHarvest}, which is the simpler reference for
 * what a harvest does. Use {@link Directory#collectFrom}, which picks one.
 * <p>
 * Each directory is one task: read its attribute files, and list its
 * children (unless it is a dataset). The tasks of the children are then
 * started, and a callback assembles the directory once they have all
 * completed. So no task blocks waiting for another.
 * <p>
 * A {@code Directory} is not thread-safe, so each one is only modified by
 * the task that creates it, and then by the callback that adds its children
 * (after those have completed).
 */
final class ParallelHarvest implements Supplier<Directory> {

	private final ContainerDialect dialect;
	private final HierarchyStore store;
	private final Gson gson;
	private final String[] attributeFileNames;
	private final boolean requireGroup;
	private final Executor executor;

	ParallelHarvest(final ContainerDialect dialect,
			final String[] attributeFileNames,
			final boolean requireGroup,
			final Executor executor) {

		this.dialect = dialect;
		this.store = dialect.getHierarchyStore();
		this.gson = dialect.getGson();
		this.attributeFileNames = attributeFileNames;
		this.requireGroup = requireGroup;
		this.executor = executor;
	}

	/**
	 * Harvest on the executor, and wait for the result on the calling
	 * thread.
	 */
	@Override
	public Directory get() {

		return join(collect(N5DirectoryPath.of("")));
	}

	/**
	 * Wait for {@code future}, and rethrow what it failed with unwrapped
	 * (rather than as a {@code CompletionException}).
	 */
	private static <T> T join(final CompletableFuture<T> future) {

		try {
			return future.join();
		} catch (final CompletionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException)cause;
			if (cause instanceof Error)
				throw (Error)cause;
			throw new N5Exception(cause);
		}
	}

	/**
	 * Harvest the directory at {@code path} and everything below it.
	 *
	 * @return the {@code Directory}, or {@code null} if it is left out
	 * 		({@link #requireGroup})
	 */
	private CompletableFuture<Directory> collect(final N5DirectoryPath path) {

		return CompletableFuture
				.supplyAsync(() -> visit(path), executor)
				.thenCompose(visited -> {
					if (visited == null || visited.childNames.length == 0)
						return CompletableFuture.completedFuture(visited == null ? null : visited.dir);

					final String[] names = visited.childNames;
					@SuppressWarnings("unchecked")
					final CompletableFuture<Directory>[] children = new CompletableFuture[names.length];
					for (int i = 0; i < names.length; i++)
						children[i] = collect(path.resolve(names[i]).asDirectory());

					// NB: Add children in the (sorted) order of names, not in
					// the order they complete, so the tree is deterministic.
					return CompletableFuture.allOf(children).thenApply(ignored -> {
						for (int i = 0; i < names.length; i++) {
							final Directory child = children[i].join(); // already complete
							if (child != null)
								visited.dir.putChild(names[i], child);
						}
						return visited.dir;
					});
				});
	}

	/**
	 * The directory at {@code path}, with its attribute files but without
	 * children, and the names of the children to harvest.
	 */
	private static final class Visited {

		final Directory dir;
		final String[] childNames;

		Visited(final Directory dir, final String[] childNames) {

			this.dir = dir;
			this.childNames = childNames;
		}
	}

	/**
	 * @return the visited directory, or {@code null} if it is left out
	 * 		({@link #requireGroup})
	 */
	private Visited visit(final N5DirectoryPath path) {

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
			return new Visited(dir, new String[0]);
		} else if (requireGroup && !dialect.groupExists(path)) {
			// We are strict about every level of the hierarchy above datasets
			// being a proper group. And path is neither a dataset nor a group:
			// Do not store anything and do not recurse into subdirectories.
			return null;
		} else {
			final String[] list = store.listDirectories(path);
			Arrays.sort(list);
			return new Visited(dir, list);
		}
	}
}

package org.janelia.saalfeldlab.n5.universe.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.ByteArrayDataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.universe.factory.N5Factory;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * {@link Directory#collectFrom(GsonN5Reader, java.util.concurrent.Executor)}
 * must produce the same tree in parallel as sequentially, including the order
 * of children, for every dialect.
 */
@RunWith(Parameterized.class)
public class ParallelHarvestTests {

	/**
	 * A container format: how to open a writer for it, and the name of the file
	 * that makes a directory a group, if there is one (to build a directory that
	 * is not a group, by deleting it).
	 */
	private static final class Format {

		final String name;
		final String containerName;
		final Function<String, N5Writer> open;
		final String groupFile;

		Format(final String name, final String containerName, final Function<String, N5Writer> open, final String groupFile) {

			this.name = name;
			this.containerName = containerName;
			this.open = open;
			this.groupFile = groupFile;
		}

		@Override
		public String toString() {

			return name;
		}
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> formats() {

		return Arrays.asList(new Object[][]{
				{new Format("n5", "harvest.n5", N5FSWriter::new, null)},
				{new Format("zarr2", "harvest.zarr", path -> new N5Factory().openWriter(StorageFormat.ZARR2, path), ".zgroup")},
				{new Format("zarr3", "harvest.zarr", path -> new N5Factory().openWriter(StorageFormat.ZARR3, path), "zarr.json")},
				{new Format("hdf5", "harvest.h5", N5HDF5Writer::new, null)}
		});
	}

	private final Format format;

	private Path tempDir;
	private Path containerPath;
	private N5Writer n5;

	public ParallelHarvestTests(final Format format) {

		this.format = format;
	}

	@Before
	public void before() throws IOException {

		tempDir = Files.createTempDirectory("n5-parallel-harvest-test-");
		containerPath = tempDir.resolve(format.containerName);
		n5 = format.open.apply(containerPath.toString());
		createHierarchy(n5);
	}

	@After
	public void after() throws IOException {

		if (n5 != null)
			n5.close();
		if (tempDir != null && Files.exists(tempDir)) {
			try (Stream<Path> paths = Files.walk(tempDir)) {
				paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		}
	}

	/**
	 * Deeper than the thread pools below are large, and with a wide level, so
	 * that a harvest that blocked pool threads waiting for subtrees would
	 * deadlock. Datasets have a chunk written, so a harvest that descended into
	 * datasets would find directories there.
	 */
	private static void createHierarchy(final N5Writer n5) {

		n5.setAttribute("", "root", 1);

		n5.createGroup("a/b/c/d/e/f");
		n5.setAttribute("a", "name", "a");
		n5.setAttribute("a/b/c", "depth", 3);
		createDatasetWithChunk(n5, "a/b/c/d/e/f/deep");
		createDatasetWithChunk(n5, "a/shallow");
		n5.setAttribute("a/shallow", "kind", "dataset");

		for (int i = 0; i < 20; i++) {
			final String group = String.format("wide/g%02d", i);
			n5.createGroup(group);
			n5.setAttribute(group, "index", i);
			if (i % 5 == 0)
				createDatasetWithChunk(n5, group + "/ds");
		}

		n5.createGroup("empty");
	}

	private static void createDatasetWithChunk(final N5Writer n5, final String path) {

		final DatasetAttributes attributes = new DatasetAttributes(
				new long[]{4, 4}, new int[]{2, 2}, DataType.UINT8, new RawCompression());
		n5.createDataset(path, attributes);
		n5.writeChunk(path, n5.getDatasetAttributes(path), new ByteArrayDataBlock(new int[]{2, 2}, new long[]{1, 1}, new byte[4]));
	}

	private GsonN5Reader reader() {

		return (GsonN5Reader)n5;
	}

	/**
	 * The tree as a string: unlike {@code JsonObject.equals}, this compares the
	 * order of children too.
	 */
	private static String serialize(final Directory dir) {

		return dir == null ? "null" : dir.toJson().toString();
	}

	private static String harvest(final GsonN5Reader n5, final int nThreads, final boolean requireGroup) {

		final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		try {
			return serialize(Directory.collectFrom(n5, executor, requireGroup));
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void testParallelEqualsSequential() {

		final String sequential = serialize(Directory.collectFrom(reader()));
		for (final int nThreads : new int[]{1, 2, 16})
			assertEquals("nThreads = " + nThreads, sequential, harvest(reader(), nThreads, false));
	}

	@Test
	public void testHarvestedTree() {

		final ExecutorService executor = Executors.newFixedThreadPool(4);
		final Directory root;
		try {
			root = Directory.collectFrom(reader(), executor);
		} finally {
			executor.shutdownNow();
		}

		assertTrue(Arrays.asList(root.list()).containsAll(Arrays.asList("a", "empty", "wide")));
		assertNotNull(root.getDirectory(N5DirectoryPath.of("a/b/c/d/e/f/deep")));
		assertNotNull(root.getDirectory(N5DirectoryPath.of("empty")));

		final String[] wide = root.getDirectory(N5DirectoryPath.of("wide")).list();
		assertEquals(20, wide.length);
		final String[] sorted = wide.clone();
		Arrays.sort(sorted);
		assertTrue("children in sorted order", Arrays.equals(sorted, wide));

		// The harvest stops at datasets: their chunk directories are not children.
		for (final String dataset : new String[]{"a/shallow", "a/b/c/d/e/f/deep", "wide/g05/ds"})
			assertEquals(dataset, 0, root.getDirectory(N5DirectoryPath.of(dataset)).list().length);
	}

	@Test
	public void testRequireGroup() {

		final String sequential = serialize(Directory.collectFrom(reader(), null, true));
		for (final int nThreads : new int[]{1, 2, 16})
			assertEquals("nThreads = " + nThreads, sequential, harvest(reader(), nThreads, true));
	}

	/**
	 * A dataset below a directory that is not a group is left out with {@code
	 * requireGroup}, and kept without it.
	 */
	@Test
	public void testRequireGroupLeavesOutNonGroups() throws IOException {

		assumeTrue("format has a group file", format.groupFile != null);

		createDatasetWithChunk(n5, "notAGroup/ds");
		n5.close();
		Files.delete(containerPath.resolve("notAGroup").resolve(format.groupFile));
		n5 = format.open.apply(containerPath.toString());

		final ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			final Directory strict = Directory.collectFrom(reader(), executor, true);
			assertNull(strict.getDirectory(N5DirectoryPath.of("notAGroup")));
			assertNotNull(strict.getDirectory(N5DirectoryPath.of("a/b/c/d/e/f/deep")));
			assertEquals(serialize(Directory.collectFrom(reader(), null, true)), serialize(strict));

			final Directory lenient = Directory.collectFrom(reader(), executor, false);
			assertNotNull(lenient.getDirectory(N5DirectoryPath.of("notAGroup/ds")));
		} finally {
			executor.shutdownNow();
		}
	}

	/**
	 * A failure while harvesting comes out as itself, not wrapped in a {@code
	 * CompletionException}, and the same way with and without an executor.
	 */
	@Test
	public void testFailure() throws IOException {

		assumeTrue("format has attribute files", !format.name.equals("hdf5"));

		final String attributesFile = format.name.equals("n5") ? "attributes.json"
				: format.name.equals("zarr2") ? ".zattrs"
				: "zarr.json";
		n5.close();
		Files.write(containerPath.resolve("wide/g07").resolve(attributesFile), "{ not json".getBytes());
		n5 = format.open.apply(containerPath.toString());

		final RuntimeException sequential = assertThrows(RuntimeException.class, () -> Directory.collectFrom(reader()));
		final ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			final RuntimeException parallel = assertThrows(RuntimeException.class, () -> Directory.collectFrom(reader(), executor));
			assertSame(sequential.getClass(), parallel.getClass());
			assertFalse(parallel instanceof java.util.concurrent.CompletionException);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void testTranslatedN5ReaderWithExecutor() throws InterruptedException {

		final ExecutorService executor = Executors.newFixedThreadPool(4);
		final TranslatedN5Reader parallel = new TranslatedN5Reader(n5, ".", ".", executor);
		executor.shutdown();
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

		final TranslatedN5Reader sequential = new TranslatedN5Reader(n5, ".", ".");
		assertEquals(serialize(sequential.translated.getRoot()), serialize(parallel.translated.getRoot()));
		assertTrue(parallel.datasetExists("a/b/c/d/e/f/deep"));
	}
}

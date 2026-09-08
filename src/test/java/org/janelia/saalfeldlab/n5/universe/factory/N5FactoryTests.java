package org.janelia.saalfeldlab.n5.universe.factory;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class N5FactoryTests {

	protected N5Factory newN5Factory() {
		return new N5Factory();
	};

	protected StorageFormat[] reorderPreferedStorageFormat(StorageFormat preferredStorageFormat) {

		final StorageFormat[] defaultOrder = StorageFormat.values();
		final List<StorageFormat> reorder = new ArrayList<>(Arrays.asList(defaultOrder));
		reorder.remove(preferredStorageFormat);
		reorder.add(0, preferredStorageFormat);
		return reorder.toArray(new StorageFormat[0]);
	}

	@Test
	public void testStorageFormatOrderWithPreference() {

		N5Factory factory = newN5Factory();
		final StorageFormat[] defaultPriority = StorageFormat.values();
		assertArrayEquals(defaultPriority, factory.getOptions().format().orderedStorageFormats());

		for (StorageFormat format : StorageFormat.values()) {
			factory.getOptions().format().preferredStorageFormat(format);
			assertArrayEquals(reorderPreferedStorageFormat(format), factory.getOptions().format().orderedStorageFormats());
		}
	}

	/**
	 * The {@code kvr} counterpart of the old {@code StorageFormat.guessStorageFromUri} test:
	 * inference from the path suffix alone, which is {@link InferredStorageFormat.Source#SUFFIX}.
	 * <p>
	 * NB: These are deliberately paths that do not exist. Suffix inference must not touch the
	 * filesystem — a writer is opened for a container that is not there yet.
	 */
	@Test
	public void testStorageFormatGuesses() throws URISyntaxException {

		assertSuffixGuess(null, "file:///tmp/a");
		assertSuffixGuess(StorageFormat.HDF5, "file:///tmp/a.h5");
		assertSuffixGuess(StorageFormat.HDF5, "file:///tmp/a.hdf5");
		assertSuffixGuess(StorageFormat.N5, "file:///tmp/a.n5");
		assertSuffixGuess(StorageFormat.N5, "file:///tmp/a.n5/");
		assertSuffixGuess(StorageFormat.ZARR, "file:///tmp/a.zarr");
		assertSuffixGuess(StorageFormat.ZARR, "file:///tmp/a.zarr/");
		assertSuffixGuess(null, "file:///tmp/a.abc");
	}

	private static void assertSuffixGuess(final StorageFormat expected, final String uri) throws URISyntaxException {

		final InferredStorageFormat inferred = InferredStorageFormat.of(new URI(uri));
		assertEquals(uri, expected, inferred.format());
		assertEquals(uri + " source", expected == null
						? InferredStorageFormat.Source.UNKNOWN
						: InferredStorageFormat.Source.SUFFIX,
				inferred.source());
		assertFalse(uri + " is a guess, not explicit", inferred.isExplicit());
	}

	@Test
	public void testWriterTypeByExtension() throws IOException {

		final N5Factory factory = newN5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final Object[][] extensionToWriterParams = new Object[][]{
					{".h5", "", N5HDF5Writer.class},
					{".hdf5", "", N5HDF5Writer.class},
					{".n5", "", N5KeyValueWriter.class},
					{".n5", "/", N5KeyValueWriter.class},
					{".zarr", "", ZarrV3KeyValueWriter.class},
					{".zarr", "/", ZarrV3KeyValueWriter.class},

			};

			for (int i = 0; i < extensionToWriterParams.length; i++) {
				final Object[] testData = extensionToWriterParams[i];
				final String extension = (String)testData[0];
				final String trailing = (String)testData[1];
				final Class<?> writerType = (Class<?>)testData[2];
				final String uri = tmp.toPath().resolve("foo" + i + extension).normalize().toUri() + trailing;
				checkWriterTypeFromFactory(factory, uri, writerType, " with extension");

			}
		} finally {
			FileUtils.deleteDirectory(tmp);
		}

	}

	@Test
	public void testWriterTypeByPrefix() throws IOException {

		final N5Factory factory = newN5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();
			final Object[][] testData = new Object[][] {
					{"h5:", N5HDF5Writer.class},
					{"hdf5:", N5HDF5Writer.class},
					{"n5:", N5KeyValueWriter.class},
					{"zarr:", ZarrV3KeyValueWriter.class},
					{"zarr2:", ZarrKeyValueWriter.class},
					{"zarr3:", ZarrV3KeyValueWriter.class}
			};

			// ensure that prefix is preferred to extensions
			final String[] extensions = new String[]{".h5", ".hdf5", ".n5", ".zarr", ".zarr3"};

			for (Object[] entry : testData) {
				final String prefix = (String)entry[0];
				final Class<?> writerType = (Class<?>)entry[1];
				final String uri = prefix + tmp.toPath().resolve("foo").normalize().toUri();
				checkWriterTypeFromFactory(factory, uri, writerType, " with prefix");

				for (String extension : extensions) {
					final String prefixUri = prefix + tmp.toPath().resolve("foo" + extension).normalize().toUri();
					checkWriterTypeFromFactory(factory, prefixUri, writerType, " with prefix");
				}
			}
		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}



	@Test
	public void testDefaultForAmbiguousWritersWithPreference() throws IOException {
		final N5Factory factory = newN5Factory();
		factory.getOptions().format().preferredStorageFormat(StorageFormat.N5);

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] paths = new String[]{
					"a_zarr_directory",
					"an_n5_directory",
					"an_empty_directory",
			};

			final Path tmpPath = tmp.toPath();

			factory.openWriter(StorageFormat.ZARR2, tmpPath.resolve(paths[0]).toFile().getCanonicalPath()).close();
			factory.openWriter(StorageFormat.N5, tmpPath.resolve(paths[1]).toFile().getCanonicalPath()).close();

			final File tmpEmptyDir = tmpPath.resolve(paths[2]).toFile();
			tmpEmptyDir.mkdirs();
			tmpEmptyDir.deleteOnExit();

			final Class<?>[] writerTypes = new Class[]{
					ZarrKeyValueWriter.class, // valid zarr, correct by key match
					N5KeyValueWriter.class, // valid n5, correct by key match
					N5KeyValueWriter.class, // empty directory, create new N5 by preference
			};

			for (int i = 0; i < paths.length; i++) {

				final String prefixUri = tmpPath.resolve(paths[i]).normalize().toUri().toString();
				checkWriterTypeFromFactory(factory, prefixUri, writerTypes[i], " with path " + paths[i]);
			}

		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}

	@Test
	public void testDefaultForAmbiguousWriters() throws IOException {

		final N5Factory factory = newN5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] paths = new String[]{
					"a_non_hdf5_file",
					"an_hdf5_file",
					"a_zarr_directory",
					"a_zarr2_directory",
					"an_n5_directory",
					"an_empty_directory",
					"a_non_existent_path"
			};

			final Path tmpPath = tmp.toPath();

			final File tmpNonHdf5File = tmpPath.resolve(paths[0]).toFile();
			tmpNonHdf5File.createNewFile();
			tmpNonHdf5File.deleteOnExit();

			factory.openWriter(StorageFormat.HDF5, tmpPath.resolve(paths[1]).toFile().getCanonicalPath()).close();
			factory.openWriter(StorageFormat.ZARR, tmpPath.resolve(paths[2]).toFile().getCanonicalPath()).close();
			factory.openWriter(StorageFormat.ZARR2, tmpPath.resolve(paths[3]).toFile().getCanonicalPath()).close();
			factory.openWriter(StorageFormat.N5, tmpPath.resolve(paths[4]).toFile().getCanonicalPath()).close();

			final File tmpEmptyDir = tmpPath.resolve(paths[4]).toFile();
			tmpEmptyDir.mkdirs();
			tmpEmptyDir.deleteOnExit();

			final Class<?>[] writerTypes = new Class[]{
					null,
					N5HDF5Writer.class,
					ZarrV3KeyValueWriter.class, // valid zarr, correct by key match
					ZarrKeyValueWriter.class, // valid zarr, correct by key match
					N5KeyValueWriter.class, // valid n5, correct by key match
					ZarrV3KeyValueWriter.class, // empty directory, create new zarr
					ZarrV3KeyValueWriter.class // directory doesn't exist, create new zarr
			};

			for (int i = 0; i < paths.length; i++) {

				final String prefixUri = tmpPath.resolve(paths[i]).normalize().toUri().toString();
				checkWriterTypeFromFactory(factory, prefixUri, writerTypes[i], " with path " + paths[i]);
			}

		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}

	@Test
	public void testAmbiguousZarrFormat() throws IOException {

		final N5Factory factory = newN5Factory();
		File tmpDir = Files.createTempDirectory("factory-test-").toFile();

		/* by default, should be zarr 3 with no other information */
		URI doesntExistsIsZarr3 = tmpDir.toPath().resolve("doesnt_exists_is_zarr3").toUri();
		N5Writer n5 = factory.openWriter(StorageFormat.ZARR, doesntExistsIsZarr3);
		assertEquals(ZarrV3KeyValueWriter.class, n5.getClass());

		/* If a zarr2 container exists, it should correctly return zarr2 */
		URI explicitZarr2 = tmpDir.toPath().resolve("explicit_zarr2").toUri();
		N5Writer explicitZarr2n5 = factory.openWriter(StorageFormat.ZARR2, explicitZarr2);
		assertEquals(ZarrKeyValueWriter.class, explicitZarr2n5.getClass());

		N5Reader guessZarr2Reader = factory.openReader(StorageFormat.ZARR, explicitZarr2);
		assertEquals(ZarrKeyValueReader.class, guessZarr2Reader.getClass());

		N5Reader guessZarr2Writer = factory.openWriter(StorageFormat.ZARR, explicitZarr2);
		assertEquals(ZarrKeyValueWriter.class, guessZarr2Writer.getClass());

		/* If zarr container is valid Zarr2 and Zarr3, should return zarr3 preferentially */
		URI bothZarrs = tmpDir.toPath().resolve("both_zarr2_zarr3").toUri();
		N5Writer bothZarr2 = factory.openWriter(StorageFormat.ZARR2, bothZarrs);
		assertEquals(ZarrKeyValueReader.class, factory.openReader(StorageFormat.ZARR, bothZarrs).getClass());
		assertEquals(ZarrKeyValueWriter.class, factory.openWriter(StorageFormat.ZARR, bothZarrs).getClass());

		N5Writer bothZarr3 = factory.openWriter(StorageFormat.ZARR3, bothZarrs);
		assertEquals(ZarrV3KeyValueReader.class, factory.openReader(StorageFormat.ZARR, bothZarrs).getClass());
		assertEquals(ZarrV3KeyValueWriter.class, factory.openWriter(StorageFormat.ZARR, bothZarrs).getClass());

		assertEquals(ZarrKeyValueReader.class, factory.openReader(StorageFormat.ZARR2, bothZarrs).getClass());
		assertEquals(ZarrKeyValueWriter.class, factory.openWriter(StorageFormat.ZARR2, bothZarrs).getClass());
	}

	@Test
	public void testForExistingWriters() throws IOException {

		final N5Factory factory = newN5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final Object[][] testData = new Object[][] {
					{"h5WithWeirdExtension.zarr", StorageFormat.HDF5, N5HDF5Writer.class},
					{"zarr2WithWeirdExtension.n5", StorageFormat.ZARR2, ZarrKeyValueWriter.class},
					{"zarr3WithWeirdExtension.jpg", StorageFormat.ZARR3, ZarrV3KeyValueWriter.class},
					{"n5WithWeirdExtension.h5", StorageFormat.N5, N5KeyValueWriter.class},
			};

			for (Object[] entry : testData) {
				final String path = (String)entry[0];
				final StorageFormat format = (StorageFormat)entry[1];
				final Class<?> writerType = (Class<?>)entry[2];
				/* create the container with the given format */
				factory.openWriter(format, tmp.toPath().resolve(path).toFile().getCanonicalPath()).close();
				/* verify we open as the same format we created as, via the factory */
				final String uri = tmp.toPath().resolve(path).normalize().toUri().toString();
				checkWriterTypeFromFactory(factory, uri, writerType, " with path " + path);
			}

		} finally {
			tmp.delete();
		}
	}


	@Test
	public void testDefaultForAmbiguousReaders() throws IOException {

		final N5Factory factory = newN5Factory();
		final ArrayList<N5Writer> writers = new ArrayList<>();
		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] paths = new String[]{
					"a_non_hdf5_file",
					"an_hdf5_file",
					"a_zarr_directory",
					"an_n5_directory",
					"an_empty_directory",
					"a_non_existent_path"
			};

			final Path tmpPath = tmp.toPath();

			final File tmpNonHdf5File = tmpPath.resolve(paths[0]).toFile();
			tmpNonHdf5File.createNewFile();
			tmpNonHdf5File.deleteOnExit();

			N5Writer h5 = factory.openWriter(StorageFormat.HDF5, tmpPath.resolve(paths[1]).toFile().getCanonicalPath());
			N5Writer zarr = factory.openWriter(StorageFormat.ZARR2, tmpPath.resolve(paths[2]).toFile().getCanonicalPath());
			N5Writer n5 = factory.openWriter(StorageFormat.N5, tmpPath.resolve(paths[3]).toFile().getCanonicalPath());
			writers.add(h5);
			writers.add(zarr);
			writers.add(n5);

			final File tmpEmptyDir = tmpPath.resolve(paths[4]).toFile();
			tmpEmptyDir.mkdirs();
			tmpEmptyDir.deleteOnExit();

			final Class<?>[] readerTypes = new Class[]{
					null,
					N5HDF5Reader.class,
					ZarrKeyValueReader.class,
					N5KeyValueReader.class,
					N5KeyValueReader.class,
					null
			};

			for (int i = 0; i < paths.length; i++) {
				final String prefixUri = tmpPath.resolve(paths[i]).normalize().toUri().toString();
				checkReaderTypeFromFactory(factory, prefixUri, readerTypes[i], " with path " + paths[i]);
			}

			for (N5Writer writer : writers) {
				try {
					writer.remove();
					writer.close();
				} catch (Exception e) {
				}
			}
		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}

	@Test
	public void testZarr2VsZarr3Disambiguation() throws URISyntaxException {

		/* The suffix only says ZARR; only probing the keys can tell v2 from v3. */
		final URI uri = new URI("src/test/resources/metadata.zarr");
		final KeyValueRoot root = KeyValueRoots.createFileSystem(uri);

		final InferredStorageFormat inferred = InferredStorageFormat.of(uri);
		assertEquals("from the suffix", StorageFormat.ZARR, inferred.format());

		final InferredStorageFormat probed = inferred.resolveFromKeys(root);
		assertEquals("zarr 2", StorageFormat.ZARR2, probed.format());
		assertEquals(InferredStorageFormat.Source.CONTENT, probed.source());
	}

	private void checkWriterTypeFromFactory(N5Factory factory, String uri, Class<?> expected, String messageSuffix) {

		if (expected == null) {
			assertThrows("Should throw exception for " + uri, N5Exception.class, () -> factory.openWriter(uri));
			return;
		}

		try ( final N5Writer n5 = factory.openWriter(uri) ) {
			assertNotNull("null n5 for " + uri, n5);
			assertEquals(expected.getName() + messageSuffix, expected, n5.getClass());
			n5.remove();
		}
	}

	private void checkReaderTypeFromFactory(N5Factory factory, String uri, Class<?> expected, String messageSuffix) {

		if (expected == null) {
			assertThrows(N5Exception.class, () -> factory.openReader(uri));
			return;
		}

		try ( final N5Reader n5 = factory.openReader(uri) ) {
			assertNotNull("null n5 for " + uri, n5);
			assertEquals(expected.getName() + messageSuffix, expected, n5.getClass());
		}
	}

	@Test
	public void testOpenExistingWriterDoesNotCreate() throws IOException {

		final N5Factory factory = newN5Factory();
		final File tmp = Files.createTempDirectory( "factory-existing-writer-" ).toFile();
		try {
			final File container = new File( tmp, "container.n5" );
			final String uri = "n5:" + container.getAbsolutePath();

			/* no container yet: it must throw and must not create anything */
			assertThrows( N5Exception.class, () -> factory.openExistingWriter( uri ) );
			assertFalse( "openExistingWriter must not create a container", container.exists() );

			/* once the container exists, it opens as a writer */
			factory.openWriter( uri ).createGroup( "foo" );
			final N5Writer existing = factory.openExistingWriter( uri );
			assertNotNull( existing );
			assertTrue( existing.exists( "foo" ) );
		} finally {
			FileUtils.deleteDirectory( tmp );
		}
	}

}

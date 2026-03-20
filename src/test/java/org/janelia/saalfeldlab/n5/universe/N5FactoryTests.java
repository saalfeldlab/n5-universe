package org.janelia.saalfeldlab.n5.universe;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class N5FactoryTests {

	private StorageFormat[] reorderPreferedStorageFormat(StorageFormat preferredStorageFormat) {

		final StorageFormat[] defaultOrder = StorageFormat.values();
		final List<StorageFormat> reorder = new ArrayList<>(Arrays.asList(defaultOrder));
		reorder.remove(preferredStorageFormat);
		reorder.add(0, preferredStorageFormat);
		return reorder.toArray(new StorageFormat[0]);
	}

	@Test
	public void testStorageFormatOrderWithPreference() {

		N5Factory factory = new N5Factory();
		final StorageFormat[] defaultPriority = StorageFormat.values();
		assertArrayEquals(defaultPriority, factory.orderedStorageFormats());

		for (StorageFormat format : StorageFormat.values()) {
			factory.preferredStorageFormat(format);
			assertArrayEquals(reorderPreferedStorageFormat(format), factory.orderedStorageFormats());
		}
	}

	@Test
	public void testStorageFormatGuesses() throws URISyntaxException {

		final URI noExt = new URI("file:///tmp/a");
		final URI h5Ext = new URI("file:///tmp/a.h5");
		final URI hdf5Ext = new URI("file:///tmp/a.hdf5");
		final URI n5Ext = new URI("file:///tmp/a.n5");
		final URI n5ExtSlash = new URI("file:///tmp/a.n5/");
		final URI zarrExt = new URI("file:///tmp/a.zarr");
		final URI zarrExtSlash = new URI("file:///tmp/a.zarr/");
		final URI unknownExt = new URI("file:///tmp/a.abc");

		assertNull("no extension null", StorageFormat.guessStorageFromUri(noExt));

		/*
		 * h5 tests fail now because these test whether the file exists. It
		 * should not do that, if, for example, we're making a writer.
		 */
		assertEquals("h5 extension == h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != zarr2", StorageFormat.ZARR2, StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != zarr3", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(h5Ext));

		assertEquals("hdf5 extension == h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != zarr2", StorageFormat.ZARR2, StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != zarr3", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(hdf5Ext));

		assertNotEquals("n5 extension != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(n5Ext));
		assertEquals("n5 extension == n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(n5Ext));
		assertNotEquals("n5 extension != zarr2", StorageFormat.ZARR2, StorageFormat.guessStorageFromUri(n5Ext));
		assertNotEquals("n5 extension != zarr3", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(n5Ext));

		assertNotEquals("n5 extension slash != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertEquals("n5 extension slash == n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertNotEquals("n5 extension slash != zarr2", StorageFormat.ZARR2, StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertNotEquals("n5 extension slash != zarr3", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(n5ExtSlash));

		assertNotEquals("zarr extension != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(zarrExt));
		assertNotEquals("zarr extension != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(zarrExt));
		assertNotEquals("zarr extension != zarr3", StorageFormat.ZARR2, StorageFormat.guessStorageFromUri(zarrExt));
		assertEquals("zarr extension == zarr3", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(zarrExt));

		assertNotEquals("zarr extension slash != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertNotEquals("zarr extension slash != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertNotEquals("zarr extension slash != zarr", StorageFormat.ZARR2, StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertEquals("zarr extension slash == zarr3", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(zarrExtSlash));

		assertNull("unknown extension != h5", StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != n5", StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != zarr2", StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != zarr3", StorageFormat.guessStorageFromUri(unknownExt));
	}

	@Test
	public void testWriterTypeByExtension() throws IOException {

		final N5Factory factory = new N5Factory();

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

		final N5Factory factory = new N5Factory();

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
		final N5Factory factory = new N5Factory();
		factory.preferredStorageFormat(StorageFormat.N5);

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

		final N5Factory factory = new N5Factory();

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

		final N5Factory factory = new N5Factory();
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

		final N5Factory factory = new N5Factory();

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

		final N5Factory factory = new N5Factory();
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
	public void testCachedFactoryKeys() throws IOException {

		final N5FactoryWithCache cachedFactory = new N5FactoryWithCache();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("n5-cachedFactory-test-").toFile();
			final String tmpPath = tmp.getAbsolutePath();

			/* Writers */
			final N5Writer writer1 = cachedFactory.openWriter(tmpPath); // a ZarrKeyValueWriter
			final N5Writer writer2 = cachedFactory.openWriter(tmpPath);
			assertSame(writer2, writer1);

			final N5Writer writerFromStoragePrefix = cachedFactory.openWriter("zarr3:" + tmpPath);
			assertSame(writerFromStoragePrefix, writer1);

			final N5Writer writerFromStoragePrefix2 = cachedFactory.openWriter("zarr3://" + tmpPath);
			assertSame(writerFromStoragePrefix2, writer1);

			final N5Writer writerDifferentStorageFormat = cachedFactory.openWriter(StorageFormat.N5, tmpPath);
			assertNotSame(writerDifferentStorageFormat, writer1);

			final N5Writer writerFromStorageType = cachedFactory.openWriter(StorageFormat.ZARR3, tmpPath);
			assertNotSame(writerFromStorageType, writerDifferentStorageFormat);
			assertNotSame(writerFromStorageType, writer1);


			/* Readers */
			final N5Reader reader1 = cachedFactory.openReader(tmpPath);
			assertNotSame(reader1, writer1);

			final N5Reader reader2 = cachedFactory.openReader(tmpPath);
			assertSame(reader2, reader1);

			final N5Reader readerFromStoragePrefix = cachedFactory.openReader("zarr3:" + tmpPath);
			assertSame(readerFromStoragePrefix, reader1);

			final N5Reader readerFromStoragePrefix2 = cachedFactory.openReader("zarr3://" + tmpPath);
			assertSame(readerFromStoragePrefix2, reader1);

			final N5Reader readerDifferentStorageFormat = cachedFactory.openReader(StorageFormat.N5, tmpPath);
			assertNotSame(readerDifferentStorageFormat, reader1);

			final N5Reader readerFromStorageType = cachedFactory.openReader(StorageFormat.ZARR3, tmpPath);
			assertNotSame(readerFromStorageType, readerDifferentStorageFormat);
			assertNotSame(readerFromStorageType, reader1);


			/* path normalization */
			N5Reader expected = readerFromStorageType;

			final N5Reader readerFromUri = cachedFactory.openReader(tmp.toURI().toString());
			assertSame(readerFromUri, expected);

			final N5Reader readerSlash = cachedFactory.openReader(tmpPath + "/");
			assertSame(readerSlash, expected);

			final N5Reader readerNotNormal = cachedFactory.openReader(tmpPath + "/foo/..");
			assertSame(readerNotNormal, expected);

			/* different methods of URI creation */
			final N5Reader readerFromFileUri = cachedFactory.openReader(tmp.toURI().toString());
			assertSame(readerFromFileUri, expected);

			final N5Reader readerFromPathUri = cachedFactory.openReader(tmp.toPath().toUri().toString());
			assertSame(readerFromPathUri, expected);


			/* relative paths */
			final String rootName = "monkeySee.n5";
			final String absPath = Paths.get(rootName).toFile().getAbsolutePath();

			final N5Writer writerAbs = cachedFactory.openWriter(absPath);
			final N5Writer writerRel = cachedFactory.openWriter("./" + rootName);
			assertSame(String.format("writers not same instance: %s \n%s\n", writerRel.getURI(), writerAbs.getURI()),
					writerRel, writerAbs);

			final N5Writer writerRel2 = cachedFactory.openWriter(rootName);
			assertSame(String.format("writers not same instance: %s \n%s\n", writerRel2.getURI(), writerAbs.getURI()),
					writerRel2, writerAbs);

			final N5Writer writerRel3 = cachedFactory.openWriter(rootName + "/foo/..");
			assertSame(String.format("writers not same instance: %s \n%s\n", writerRel3.getURI(), writerAbs.getURI()),
					writerRel3, writerAbs);

			/*Clean up*/
			writerAbs.remove();


			/* clear and remove */
			cachedFactory.clear();

			final N5Reader readerAfterClear = cachedFactory.openReader(tmpPath);
			assertNotSame(readerAfterClear, expected);
			expected = readerAfterClear;

			final N5Writer writerAfterClear = cachedFactory.openWriter(tmpPath);
			assertNotSame(writerAfterClear, expected);

			// remove
			cachedFactory.remove(tmpPath);
			final N5Reader readerAfterRemove = cachedFactory.openReader(tmpPath);
			assertNotSame(readerAfterRemove, expected);
			expected = readerAfterRemove;

			final N5Writer writerAfterRemove = cachedFactory.openWriter(tmpPath);
			assertNotSame(writerAfterRemove, writerAfterClear);

			// remove normalization
			cachedFactory.remove("zarr:" + tmpPath);
			final N5Reader readerAfterRemovePrefix = cachedFactory.openReader(tmpPath);
			assertNotSame(readerAfterRemovePrefix, expected);

			final N5Writer writerAfterRemovePrefix = cachedFactory.openWriter(tmpPath);
			assertNotSame(writerAfterRemovePrefix, writerAfterRemove);


		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}

	@Test
	public void testZarr2VsZarr3Disambiguation() throws URISyntaxException {

		final URI uri = new URI("src/test/resources/metadata.zarr");
		final FileSystemKeyValueAccess kva = new FileSystemKeyValueAccess();

		final StorageFormat format = StorageFormat.guessStorageFromKeys(uri, kva);
		assertEquals("zarr 2", StorageFormat.ZARR2, format);
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
}

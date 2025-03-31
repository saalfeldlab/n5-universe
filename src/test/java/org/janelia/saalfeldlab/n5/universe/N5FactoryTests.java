package org.janelia.saalfeldlab.n5.universe;

import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class N5FactoryTests {

	@Test
	public void testStorageFormatOrderWithPreference() {

		N5Factory factory = new N5Factory();
		assertArrayEquals(StorageFormat.values(), factory.orderedStorageFormats());

		factory.preferredStorageFormat(StorageFormat.N5);
		assertArrayEquals(new StorageFormat[]{StorageFormat.N5, StorageFormat.ZARR, StorageFormat.HDF5}, factory.orderedStorageFormats());

		factory.preferredStorageFormat(StorageFormat.ZARR);
		assertArrayEquals(StorageFormat.values(), factory.orderedStorageFormats());

		factory.preferredStorageFormat(StorageFormat.HDF5);
		assertArrayEquals(new StorageFormat[]{StorageFormat.HDF5, StorageFormat.ZARR, StorageFormat.N5}, factory.orderedStorageFormats());
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

		/**
		 * h5 tests fail now because these test whether the file exists. It
		 * should not do that, if, for example, we're making a writer.
		 */
		assertEquals("h5 extension == h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != zarr", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != zarr", StorageFormat.ZARR3, StorageFormat.guessStorageFromUri(h5Ext));

		assertEquals("hdf5 extension == h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != zarr", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(hdf5Ext));

		assertNotEquals("n5 extension != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(n5Ext));
		assertEquals("n5 extension == n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(n5Ext));
		assertNotEquals("n5 extension != zarr", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(n5Ext));
		assertNotEquals("n5 extension != zarr3", StorageFormat.ZARR3, StorageFormat.guessStorageFromUri(n5Ext));

		assertNotEquals("n5 extension slash != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertEquals("n5 extension slash == n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertNotEquals("n5 extension slash != zarr", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertNotEquals("n5 extension slash != zarr", StorageFormat.ZARR3, StorageFormat.guessStorageFromUri(n5ExtSlash));

		assertNotEquals("zarr extension != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(zarrExt));
		assertNotEquals("zarr extension != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(zarrExt));
		assertNotEquals("zarr extension == zarr2", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(zarrExt));
		assertEquals("zarr extension == zarr3", StorageFormat.ZARR3, StorageFormat.guessStorageFromUri(zarrExt));

		assertNotEquals("zarr extension slash != h5", StorageFormat.HDF5, StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertNotEquals("zarr extension slash != n5", StorageFormat.N5, StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertNotEquals("zarr extension slash != zarr2", StorageFormat.ZARR, StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertEquals("zarr extension slash == zarr", StorageFormat.ZARR3, StorageFormat.guessStorageFromUri(zarrExtSlash));

		assertNull("unknown extension != h5", StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != n5", StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != zarr", StorageFormat.guessStorageFromUri(unknownExt));
	}

	@Test
	public void testWriterTypeByExtension() throws IOException, URISyntaxException {

		final N5Factory factory = new N5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] ext = new String[]{".h5", ".hdf5", ".n5", ".n5", ".zarr", ".zarr"};

			// necessary because new File() removes trailing separator
			final String separator = FileSystems.getDefault().getSeparator();
			final String[] trailing = new String[]{"", "", "", separator, "", separator};

			final Class<?>[] readerTypes = new Class[]{
					N5HDF5Writer.class,
					N5HDF5Writer.class,
					N5KeyValueWriter.class,
					N5KeyValueWriter.class,
					ZarrV3KeyValueWriter.class,
					ZarrV3KeyValueWriter.class
			};

			for (int i = 0; i < ext.length; i++) {
				final String extUri = tmp.toPath().resolve("foo" + i + ext[i]).normalize().toUri() + trailing[i];
				checkWriterTypeFromFactory(factory, extUri, readerTypes[i], " with extension");
			}

		} finally {
			FileUtils.deleteDirectory(tmp);
		}

	}

	@Test
	public void testWriterTypeByPrefix() throws URISyntaxException, IOException {

		final N5Factory factory = new N5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] prefix = new String[]{"h5", "hdf5", "n5", "zarr", "zarr3"};
			final Class<?>[] readerTypes = new Class[]{
					N5HDF5Writer.class,
					N5HDF5Writer.class,
					N5KeyValueWriter.class,
					ZarrKeyValueWriter.class,
					ZarrV3KeyValueWriter.class
			};

			for (int i = 0; i < prefix.length; i++) {

				final String prefixUri = prefix[i] + ":" + tmp.toPath().resolve("foo" + i).normalize().toUri();
				checkWriterTypeFromFactory(factory, prefixUri, readerTypes[i], " with prefix");

				final String prefixUriSlashes = prefix[i] + "://" + tmp.toPath().resolve("foo" + i).normalize().toUri();
				checkWriterTypeFromFactory(factory, prefixUriSlashes, readerTypes[i], " with prefix slashes");
			}

			// ensure that prefix is preferred to extensions
			final String[] extensions = new String[]{".h5", ".hdf5", ".n5", ".zarr", ".zarr"};

			for (int i = 0; i < prefix.length; i++) {
				for (int j = 0; j < extensions.length; j++) {

					final String prefixUri = prefix[i] + ":" + tmp.toPath().resolve("foo" + i + extensions[i]).normalize().toUri();
					checkWriterTypeFromFactory(factory, prefixUri, readerTypes[i], " with prefix");

					final String prefixUriSlashes = prefix[i] + "://" + tmp.toPath().resolve("foo" + i + extensions[i]).normalize().toUri();
					checkWriterTypeFromFactory(factory, prefixUriSlashes, readerTypes[i], " with prefix slashes");
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

			factory.openWriter(StorageFormat.ZARR, tmpPath.resolve(paths[0]).toFile().getCanonicalPath()).close();
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
			factory.openWriter(StorageFormat.N5, tmpPath.resolve(paths[3]).toFile().getCanonicalPath()).close();

			final File tmpEmptyDir = tmpPath.resolve(paths[4]).toFile();
			tmpEmptyDir.mkdirs();
			tmpEmptyDir.deleteOnExit();

			final Class<?>[] writerTypes = new Class[]{
					null,
					N5HDF5Writer.class,
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
					"a_zarr3_directory",
					"an_n5_directory",
					"an_empty_directory",
					"a_non_existent_path"
			};

			final Path tmpPath = tmp.toPath();

			final File tmpNonHdf5File = tmpPath.resolve(paths[0]).toFile();
			tmpNonHdf5File.createNewFile();
			tmpNonHdf5File.deleteOnExit();

			N5Writer h5 = factory.openWriter(StorageFormat.HDF5, tmpPath.resolve(paths[1]).toFile().getCanonicalPath());
			N5Writer zarr = factory.openWriter(StorageFormat.ZARR, tmpPath.resolve(paths[2]).toFile().getCanonicalPath());
			N5Writer zarr3 = factory.openWriter(StorageFormat.ZARR3, tmpPath.resolve(paths[3]).toFile().getCanonicalPath());
			N5Writer n5 = factory.openWriter(StorageFormat.N5, tmpPath.resolve(paths[4]).toFile().getCanonicalPath());
			writers.add(h5);
			writers.add(zarr);
			writers.add(zarr3);
			writers.add(n5);

			final File tmpEmptyDir = tmpPath.resolve(paths[5]).toFile();
			tmpEmptyDir.mkdirs();
			tmpEmptyDir.deleteOnExit();

			final Class<?>[] readerTypes = new Class[]{
					null,
					N5HDF5Reader.class,
					ZarrKeyValueReader.class,
					ZarrV3KeyValueReader.class,
					N5KeyValueReader.class,
					N5KeyValueReader.class,
					null
			};

			for (int i = 0; i < paths.length; i++) {
				final String prefixUri = tmpPath.resolve(paths[i]).normalize().toUri().toString();;
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
	public void testCachedFactoryKeys() throws IOException, URISyntaxException {

		final N5FactoryWithCache cachedFactory = new N5FactoryWithCache();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("n5-cachedFactory-test-").toFile();
			final String tmpPath = tmp.getAbsolutePath();

			/* Writers */
			final N5Writer writer1 = cachedFactory.openWriter(tmpPath); // a ZarrKeyValueWriter
			final N5Writer writer2 = cachedFactory.openWriter(tmpPath);
			assertSame(writer2, writer1);

			final N5Writer writerFromStoragePrefix = cachedFactory.openWriter("zarr:" + tmpPath);
			assertSame(writerFromStoragePrefix, writer1);

			final N5Writer writerFromStoragePrefix2 = cachedFactory.openWriter("zarr://" + tmpPath);
			assertSame(writerFromStoragePrefix2, writer1);

			final N5Writer writerdifferentStorageFormat = cachedFactory.openWriter(StorageFormat.N5, tmpPath);
			assertNotSame(writerdifferentStorageFormat, writer1);

			final N5Writer writerFromStorageType = cachedFactory.openWriter(StorageFormat.ZARR, tmpPath);
			assertNotSame(writerFromStorageType, writerdifferentStorageFormat);
			assertNotSame(writerFromStorageType, writer1);


			/* Readers */
			final N5Reader reader1 = cachedFactory.openReader(tmpPath);
			assertNotSame(reader1, writer1);

			final N5Reader reader2 = cachedFactory.openReader(tmpPath);
			assertSame(reader2, reader1);

			final N5Reader readerFromStoragePrefix = cachedFactory.openReader("zarr:" + tmpPath);
			assertSame(readerFromStoragePrefix, reader1);

			final N5Reader readerFromStoragePrefix2 = cachedFactory.openReader("zarr://" + tmpPath);
			assertSame(readerFromStoragePrefix2, reader1);

			final N5Reader readerDifferentStorageFormat = cachedFactory.openReader(StorageFormat.N5, tmpPath);
			assertNotSame(readerDifferentStorageFormat, reader1);

			final N5Reader readerFromStorageType = cachedFactory.openReader(StorageFormat.ZARR, tmpPath);
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

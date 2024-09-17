package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class N5FactoryTests {

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

		assertNull("no extension null", N5Factory.StorageFormat.guessStorageFromUri(noExt));

		/**
		 * h5 tests fail now because these test whether the file exists. It
		 * should not do that, if, for example, we're making a writer.
		 */
		assertEquals("h5 extension == h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(h5Ext));
		assertNotEquals("h5 extension != zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(h5Ext));

		assertEquals("hdf5 extension == h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(hdf5Ext));
		assertNotEquals("hdf5 extension != zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(hdf5Ext));

		assertNotEquals("n5 extension != h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(n5Ext));
		assertEquals("n5 extension == n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(n5Ext));
		assertNotEquals("n5 extension != zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(n5Ext));

		assertNotEquals("n5 extension slash != h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertEquals("n5 extension slash == n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(n5ExtSlash));
		assertNotEquals("n5 extension slash != zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(n5ExtSlash));

		assertNotEquals("zarr extension != h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(zarrExt));
		assertNotEquals("zarr extension != n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(zarrExt));
		assertEquals("zarr extension == zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(zarrExt));

		assertNotEquals("zarr extension slash != h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertNotEquals("zarr extension slash != n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(zarrExtSlash));
		assertEquals("zarr extension slash == zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(zarrExtSlash));

		assertNull("unknown extension != h5", N5Factory.StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != n5", N5Factory.StorageFormat.guessStorageFromUri(unknownExt));
		assertNull("unknown extension != zarr", N5Factory.StorageFormat.guessStorageFromUri(unknownExt));
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
					ZarrKeyValueWriter.class,
					ZarrKeyValueWriter.class
			};

			for (int i = 0; i < ext.length; i++) {
				final String extUri = tmp.toPath().resolve("foo" + i + ext[i]).normalize().toUri() + trailing[i];
				checkWriterTypeFromFactory( factory, extUri, readerTypes[i], " with extension");
			}

		} finally {
			tmp.delete();
		}

	}

	@Test
	public void testWriterTypeByPrefix() throws URISyntaxException, IOException {

		final N5Factory factory = new N5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] prefix = new String[]{"h5", "hdf5", "n5", "zarr"};
			final Class<?>[] readerTypes = new Class[]{
					N5HDF5Writer.class,
					N5HDF5Writer.class,
					N5KeyValueWriter.class,
					ZarrKeyValueWriter.class
			};

			for (int i = 0; i < prefix.length; i++) {

				final String prefixUri = prefix[i] + ":" + tmp.toPath().resolve("foo" + i).normalize().toUri();
				checkWriterTypeFromFactory( factory, prefixUri, readerTypes[i], " with prefix");

				final String prefixUriSlashes = prefix[i] + "://" + tmp.toPath().resolve("foo" + i).normalize().toUri();
				checkWriterTypeFromFactory( factory, prefixUriSlashes, readerTypes[i], " with prefix slashes");
			}

			// ensure that prefix is preferred to extensions
			final String[] extensions = new String[]{".h5", ".hdf5", ".n5", ".zarr"};

			for (int i = 0; i < prefix.length; i++) {
				for (int j = 0; j < extensions.length; j++) {

					final String prefixUri = prefix[i] + ":" + tmp.toPath().resolve("foo" + i + extensions[i]).normalize().toUri();
					checkWriterTypeFromFactory( factory, prefixUri, readerTypes[i], " with prefix");

					final String prefixUriSlashes = prefix[i] + "://" + tmp.toPath().resolve("foo" + i + extensions[i]).normalize().toUri();
					checkWriterTypeFromFactory( factory, prefixUriSlashes, readerTypes[i], " with prefix slashes");
				}
			}

		} finally {
			tmp.delete();
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
					ZarrKeyValueWriter.class,
					ZarrKeyValueWriter.class,
					ZarrKeyValueWriter.class,
					ZarrKeyValueWriter.class
			};

			for (int i = 0; i < paths.length; i++) {

				final String prefixUri = tmpPath.resolve(paths[i]).normalize().toUri().toString();
				checkWriterTypeFromFactory( factory, prefixUri, writerTypes[i], " with path " + paths[i]);
			}

		} finally {
			tmp.delete();
		}
	}

	@Test
	public void testDefaultForAmbiguousReaders() throws IOException {

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



			final Class<?>[] readerTypes = new Class[]{
					null,
					N5HDF5Reader.class,
					ZarrKeyValueReader.class,
					N5KeyValueReader.class,
					N5KeyValueReader.class,
					null
			};

			for (int i = 0; i < paths.length; i++) {
				final String prefixUri = tmpPath.resolve(paths[i]).normalize().toUri().toString();;
				checkReaderTypeFromFactory( factory, prefixUri, readerTypes[i], " with path " + paths[i]);
			}

		} finally {
			tmp.delete();
		}
	}
	
	@Test
	public void testZarr2VsZarr3Disambiguation() throws IOException, URISyntaxException {

		// TODO: Diyi!
		final URI uri = new URI("src/test/resources/metadata.zarr?");
		final FileSystemKeyValueAccess kva = new FileSystemKeyValueAccess(FileSystems.getDefault());

		final StorageFormat format = N5Factory.StorageFormat.guessStorageFromUri(uri, kva);
		assertEquals("zarr 2", StorageFormat.ZARR, format);
	}

	private void checkWriterTypeFromFactory(N5Factory factory, String uri, Class<?> expected, String messageSuffix) {

		if (expected == null) {
			assertThrows("Should throw exception for " + uri, N5Exception.class, () -> factory.openWriter(uri));
			return;
		}

		final N5Writer n5 = factory.openWriter(uri);
		assertNotNull(	"null n5 for " + uri, n5);
		assertEquals(expected.getName() + messageSuffix, expected, n5.getClass());
		n5.remove();
	}

	private void checkReaderTypeFromFactory(N5Factory factory, String uri, Class<?> expected, String messageSuffix) {

		if (expected == null) {
			assertThrows(N5Exception.class, () -> factory.openReader(uri));
			return;
		}

		final N5Reader n5 = factory.openReader(uri);
		assertNotNull(	"null n5 for " + uri, n5);
		assertEquals(expected.getName() + messageSuffix, expected, n5.getClass());
	}
}

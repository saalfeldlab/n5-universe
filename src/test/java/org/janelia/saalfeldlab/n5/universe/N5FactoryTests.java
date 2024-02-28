package org.janelia.saalfeldlab.n5.universe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.junit.Test;

public class N5FactoryTests {

	@Test
	public void testStorageFormatGuesses() throws URISyntaxException {

		final URI noExt = new URI("file:///tmp/a");
		final URI h5Ext = new URI("file:///tmp/a.h5");
		final URI hdf5Ext = new URI("file:///tmp/a.hdf5");
		final URI n5Ext = new URI("file:///tmp/a.n5");
		final URI zarrExt = new URI("file:///tmp/a.zarr");
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

		assertNotEquals("zarr extension != h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(zarrExt));
		assertNotEquals("zarr extension != n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(zarrExt));
		assertEquals("zarr extension == zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(zarrExt));

		assertNotEquals("unknown extension != h5", StorageFormat.HDF5, N5Factory.StorageFormat.guessStorageFromUri(unknownExt));
		assertNotEquals("unknown extension != n5", StorageFormat.N5, N5Factory.StorageFormat.guessStorageFromUri(unknownExt));
		assertNotEquals("unknown extension != zarr", StorageFormat.ZARR, N5Factory.StorageFormat.guessStorageFromUri(unknownExt));
	}

	@Test
	public void testWriterTypeByExtension() {

		final N5Factory factory = new N5Factory();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("factory-test-").toFile();

			final String[] ext = new String[]{".h5", ".hdf5", ".n5", ".zarr"};
			final Class<?>[] readerTypes = new Class[]{
					N5HDF5Writer.class,
					N5HDF5Writer.class,
					N5KeyValueWriter.class,
					ZarrKeyValueWriter.class
			};

			for (int i = 0; i < ext.length; i++) {
				final File tmpWithExt = new File(tmp, "foo" + i + ext[i]);
				final String extUri = new URI("file", null, tmpWithExt.toURI().normalize().getPath(), null).toString();
				checkWriterTypeFromFactory( factory, extUri, readerTypes[i], " with extension");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			tmp.delete();
		}

	}

	@Test
	public void testWriterTypeByPrefix() {

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
				final File tmpNoExt = new File(tmp, "foo"+i);

				final String prefixUri = prefix[i] + ":" + new URI("file", null, tmpNoExt.toURI().normalize().getPath(), null).toString();
				checkWriterTypeFromFactory( factory, prefixUri, readerTypes[i], " with prefix");

				final String prefixUriSlashes = prefix[i] + "://" + new URI("file", null, tmpNoExt.toURI().normalize().getPath(), null).toString();
				checkWriterTypeFromFactory( factory, prefixUriSlashes, readerTypes[i], " with prefix slashes");
			}

			// ensure that prefix is preferred to extensions
			final String[] extensions = new String[]{".h5", ".hdf5", ".n5", ".zarr"};

			for (int i = 0; i < prefix.length; i++) {
				for (int j = 0; j < extensions.length; j++) {

					final File tmpWithExt = new File(tmp, "foo"+i+extensions[j]);

					final String prefixUri = prefix[i] + ":" + new URI("file", null, tmpWithExt.toURI().normalize().getPath(), null).toString();
					checkWriterTypeFromFactory( factory, prefixUri, readerTypes[i], " with prefix");

					final String prefixUriSlashes = prefix[i] + "://" + new URI("file", null, tmpWithExt.toURI().normalize().getPath(), null).toString();
					checkWriterTypeFromFactory( factory, prefixUriSlashes, readerTypes[i], " with prefix slashes");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			tmp.delete();
		}
	}
	
	private void checkWriterTypeFromFactory(N5Factory factory, String uri, Class<?> expected, String messageSuffix) {

		final N5Writer n5 = factory.openWriter(uri);
		assertNotNull("null n5 for " + uri, n5);
		assertEquals(expected.getName() + messageSuffix, expected, n5.getClass());
		n5.remove();
	}

}

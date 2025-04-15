package org.janelia.saalfeldlab.n5.universe.storage.zarr;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.HttpKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.http.HttpReaderFsWriter;
import org.janelia.saalfeldlab.n5.http.RunnerWithHttpServer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(RunnerWithHttpServer.class)
public class ZarrHttpFactoryTest extends ZarrStorageTests.ZarrFactoryTest {

	private static class ZarrHttpReaderFsWriter extends HttpReaderFsWriter {

		private final ZarrKeyValueWriter writer;
		private final ZarrKeyValueReader reader;

		public <W extends ZarrKeyValueWriter, R extends ZarrKeyValueReader> ZarrHttpReaderFsWriter(W writer, R reader) {

			super(writer, reader);
			this.writer = writer;
			this.reader = reader;
		}

		public ZarrKeyValueReader getReader() {

			return reader;
		}

		public ZarrKeyValueWriter getWriter() {

			return writer;
		}
	}

	private static final ArrayList<N5Writer> tempClassWriters = new ArrayList<>();

	@Parameterized.Parameter
	public static Path httpServerDirectory;

	@Parameterized.Parameter
	public URI httpServerURI;

	@After
	@Override
	public void removeTempWriters() {

		//For HTTP, don't remove After, remove AfterClass, since we need the server to be shut down first
		// move the writer to a static list
		tempClassWriters.addAll(tempWriters);
		tempWriters.clear();
	}

	@AfterClass
	public static void removeClassTempWriters() {

		for (final N5Writer writer : tempClassWriters) {
			try {
				writer.remove();
			} catch (final Exception e) {
			}
		}
		tempClassWriters.clear();
	}

	@Override public N5Writer getWriter(String uri) {

		final String writerFsPath = httpServerDirectory.resolve(uri).toFile().getAbsolutePath();
		final String uriWithStorageScheme = prependStorageScheme(writerFsPath);
		final GsonKeyValueN5Writer writer = (GsonKeyValueN5Writer)getFactory().openWriter(uriWithStorageScheme);
		final GsonKeyValueN5Reader reader = (GsonKeyValueN5Reader)getReader(uri);
		switch (getStorageFormat()) {
		case ZARR:
			assertTrue(writer instanceof ZarrKeyValueWriter);
			return new ZarrHttpReaderFsWriter((ZarrKeyValueWriter)writer, (ZarrKeyValueReader)reader);
		case N5:
			assertTrue(writer instanceof N5KeyValueWriter);
			break;
		case HDF5:
			assertTrue(writer instanceof N5HDF5Writer);
			break;
		}
		return new HttpReaderFsWriter(writer, reader);
	}

	@Override public N5Reader getReader(String uri) {

		final String readerHttpPath = httpServerURI.resolve(uri).toString();
		return super.getReader(readerHttpPath);
	}

	@Override protected String tempN5Location() {

		try {
			final File tmpFile = Files.createTempFile(httpServerDirectory, "n5-zarr-factory-test-", null).toFile();
			assertTrue(tmpFile.delete());
			return tmpFile.getName();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override public Class<?> getBackendTargetClass() {

		return HttpKeyValueAccess.class;
	}

	@Override
	@Test
	public void testCreateNestedDataset() throws IOException {

		final String datasetName = "/test/nested/data";

		final String testDirPath = tempN5Location();
		final ZarrHttpReaderFsWriter n5Nested = (ZarrHttpReaderFsWriter) createTempN5Writer(testDirPath, "/");

		n5Nested.createDataset(datasetName, dimensions, blockSize, DataType.UINT64, getCompressions()[0]);
		assertEquals("/", n5Nested.getReader().getZArrayAttributes(datasetName).getDimensionSeparator());

		// TODO test that parents of nested dataset are groups
	}



	@Test
	public void testReaderCreation() {

		// non-existent location should fail
		final String location = tempN5Location();
		assertThrows("Non-existent location throws error", N5Exception.N5IOException.class,
				() -> {
					try (N5Reader test = createN5Reader(location)) {
						test.list("/");
					}
				});

		try (N5Writer writer = createTempN5Writer(location)) {
			try (N5Reader n5r = createN5Reader(location)) {
				assertNotNull(n5r);
			}

			// existing directory without attributes is okay;
			// Remove and create to remove attributes store
			writer.removeAttribute("/", "/");
			try (N5Reader na = createN5Reader(location)) {
				assertNotNull(na);
			}

			// existing location with attributes, but no version
			writer.removeAttribute("/", "/");
			writer.setAttribute("/", "mystring", "ms");
			try (N5Reader wa = createN5Reader(location)) {
				assertNotNull(wa);
			}

			// existing directory with incompatible version should fail
			writer.removeAttribute("/", "/");
			final String invalidVersion = new N5Reader.Version(N5Reader.VERSION.getMajor() + 1, N5Reader.VERSION.getMinor(), N5Reader.VERSION.getPatch()).toString();
			writer.setAttribute("/", N5Reader.VERSION_KEY, invalidVersion);
			assertThrows("Incompatible version throws error", N5Exception.class, () -> {
				try (final N5Reader ignored = createN5Reader(location)) {
					/*Only try with resource to ensure `close()` is called.*/
				}
			});
		}
	}

	@Ignore("N5Writer not supported for HTTP")
	@Override
	public void testVersion() throws NumberFormatException {

	}

	@Ignore("N5Writer not supported for HTTP")
	@Override public void testUri() {

	}

	@Ignore("N5Writer not supported for HTTP")
	@Override public void testRemoveGroup() {

	}

	@Ignore("N5Writer not supported for HTTP")
	@Override public void testRemoveAttributes() {

	}

	@Ignore("N5Writer not supported for HTTP")
	@Override public void testRemoveContainer() {

	}

	@Ignore("N5Writer not supported for HTTP")
	@Override public void testDelete() {

	}

	@Ignore("N5Writer not supported for HTTP")
	@Override public void testWriterSeparation() {

	}
}

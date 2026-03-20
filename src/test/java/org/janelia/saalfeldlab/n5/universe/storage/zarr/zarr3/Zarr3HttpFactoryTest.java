package org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3;

import com.google.gson.JsonElement;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
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
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrStorageTests;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.chunks.DefaultChunkKeyEncoding;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3DatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(RunnerWithHttpServer.class)
public class Zarr3HttpFactoryTest extends ZarrStorageTests.Zarr3FactoryTest {

	private static class ZarrV3HttpReaderFsWriter extends HttpReaderFsWriter {

		private final ZarrV3KeyValueWriter writer;
		private final ZarrV3KeyValueReader reader;

		public <W extends ZarrV3KeyValueWriter, R extends ZarrV3KeyValueReader> ZarrV3HttpReaderFsWriter(W writer, R reader) {

			super(writer, reader);
			this.writer = writer;
			this.reader = reader;
		}

		public ZarrV3KeyValueReader getReader() {

			return reader;
		}

		public ZarrV3KeyValueWriter getWriter() {

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

	@Override public N5Factory getFactory() {

		if (factory == null) {
			factory = new N5Factory().cacheAttributes(false);
		}
		return factory;
	}

	@Override public N5Writer getWriter(String uri) {

		final String writerFsPath = httpServerDirectory.resolve(uri).toFile().getAbsolutePath();
		final String uriWithStorageScheme = prependStorageScheme(writerFsPath);
		final GsonKeyValueN5Writer writer = (GsonKeyValueN5Writer)getFactory().openWriter(uriWithStorageScheme);
		final GsonKeyValueN5Reader reader = (GsonKeyValueN5Reader)getReader(uri);
		switch (getStorageFormat()) {
		case ZARR3:
			assertTrue(writer instanceof ZarrV3KeyValueWriter);
			return new ZarrV3HttpReaderFsWriter((ZarrV3KeyValueWriter)writer, (ZarrV3KeyValueReader)reader);
		case ZARR2:
			assertTrue(writer instanceof ZarrKeyValueWriter);
			break;
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
		final ZarrV3HttpReaderFsWriter n5Nested = (ZarrV3HttpReaderFsWriter) createTempN5Writer(testDirPath, "/");

		n5Nested.createDataset(datasetName, dimensions, blockSize, DataType.UINT64, getCompressions()[0]);
		final ZarrV3DatasetAttributes zarrAttrs = (ZarrV3DatasetAttributes)n5Nested.getReader().getDatasetAttributes(datasetName);
		final DefaultChunkKeyEncoding chunkKeyEncoding = (DefaultChunkKeyEncoding)zarrAttrs.getChunkAttributes().getKeyEncoding();
		assertEquals("/", chunkKeyEncoding.getSeparator());
	}



	@Override
	@Test
	public void testCreateDataset()  {

		final DatasetAttributes info;
		try (ZarrV3HttpReaderFsWriter n5 = (ZarrV3HttpReaderFsWriter)createTempN5Writer()) {

			n5.createDataset(datasetName, dimensions, blockSize, DataType.UINT64, getCompressions()[0]);
			assertTrue("Dataset does not exist", n5.exists(datasetName));

			info = n5.getDatasetAttributes(datasetName);
			assertArrayEquals(dimensions, info.getDimensions());
			assertArrayEquals(blockSize, info.getBlockSize());
			assertEquals(DataType.UINT64, info.getDataType());
			assertEquals(
					getCompressions()[0].getClass(),
					info.getCompression().getClass());

			final JsonElement elem = n5.getReader().getRawAttribute(datasetName, "/", JsonElement.class);

			assertTrue(elem.getAsJsonObject().get("fill_value").getAsJsonPrimitive().isNumber());
		}
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

			assertNotNull(createN5Reader(location));

			// existing directory without attributes is okay;
			// Remove and create to remove attributes store
			writer.removeAttribute("/", "/");
			assertNotNull(createN5Reader(location));

			// existing location with attributes, but no version
			writer.removeAttribute("/", "/");
			writer.setAttribute("/", "mystring", "ms");
			assertNotNull(createN5Reader(location));
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

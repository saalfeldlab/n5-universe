package org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.HttpKeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.http.HttpReaderFsWriter;
import org.janelia.saalfeldlab.n5.http.RunnerWithHttpServer;
import org.janelia.saalfeldlab.n5.universe.factory.N5Factory;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrStorageTests;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;

@RunWith(RunnerWithHttpServer.class)
public class Zarr2HttpFactoryTest extends ZarrStorageTests.Zarr2FactoryTest {

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

		// NB: Disable cacheAttributes. The HttpReaderFsWriter is
		// constructed from a (reader, writer) pair. With caching
		// enabled, the reader and writer each have their own cache.
		// After querying attributes from the reader for the first time,
		// subsequent modifications by the writer are invisible to the
		// reader's cache.
		return super.getFactory().options(o -> o.cacheAttributes(false));
	}

	@Override public N5Writer getWriter(String uri) {

		final String writerFsPath = httpServerDirectory.resolve(uri).toFile().getAbsolutePath();
		final String uriWithStorageScheme = prependStorageScheme(writerFsPath);
		final GsonKeyValueN5Writer writer = (GsonKeyValueN5Writer)getFactory().openWriter(uriWithStorageScheme);
		final GsonKeyValueN5Reader reader = (GsonKeyValueN5Reader)getReader(uri);
		assertTrue(writer instanceof ZarrKeyValueWriter);
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

		return HttpKeyValueRoot.class;
	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override
	public void testVersion() throws NumberFormatException {

	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override public void testUri() {

	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override public void testRemoveGroup() {

	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override public void testRemoveAttributes() {

	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override public void testRemoveContainer() {

	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override public void testDelete() {

	}

	@Test
	@Ignore("N5Writer not supported for HTTP")
	@Override public void testWriterSeparation() {

	}
}

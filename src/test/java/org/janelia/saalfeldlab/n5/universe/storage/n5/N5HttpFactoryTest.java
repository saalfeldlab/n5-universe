package org.janelia.saalfeldlab.n5.universe.storage.n5;

import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.HttpKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.http.HttpReaderFsWriter;
import org.janelia.saalfeldlab.n5.http.RunnerWithHttpServer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

@RunWith(RunnerWithHttpServer.class)
public class N5HttpFactoryTest extends N5StorageTests.N5FactoryTest {

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
			break;
		case N5:
			assertTrue(writer instanceof N5KeyValueWriter);
			break;
		case HDF5:
			assertTrue(writer instanceof N5HDF5Writer);
			break;
		}

		final GsonKeyValueN5Writer splitReaderWriter = new HttpReaderFsWriter(writer, reader);
		return splitReaderWriter;
	}

	@Override public N5Reader getReader(String uri) {

		final String readerHttpPath = httpServerURI.resolve(uri).toString();
		return super.getReader(readerHttpPath);
	}

	@Override protected String tempN5Location() {

		try {
			final File tmpFile = Files.createTempFile(httpServerDirectory, "n5-factory-test-", null).toFile();
			assertTrue(tmpFile.delete());
			return tmpFile.getName();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override public Class<?> getBackendTargetClass() {

		return HttpKeyValueAccess.class;
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

package org.janelia.saalfeldlab.n5.universe;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.AbstractN5Test;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

@RunWith(Parameterized.class)
public class N5FactoryTest extends AbstractN5Test {

	final N5Factory factory = new N5Factory();

	@Parameterized.Parameter(0)
	public String testName;

	@Parameterized.Parameter(1)
	public String uri;

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> parameters() {

		return Arrays.asList(new Object[][]{
				{"localPathOnly", "/Users/hulbertc/projects/paintera/test.n5"},
				{"n5StorageSchemeNoNesting", "n5:/Users/hulbertc/projects/paintera/test.n5"},
				{"n5StorageSchemeNoNestingDoubleSlash", "n5:///Users/hulbertc/projects/paintera/test.n5"},
				{"n5StorageSchemeAndFile", "n5:file:///Users/hulbertc/projects/paintera/test.n5"},
				{"n5StorageSchemeAndFileDoubleSlash", "n5://file:///Users/hulbertc/projects/paintera/test.n5"},
				{"n5StorageSchemeAndFileDoubleNoExtension", "n5:file:///Users/hulbertc/projects/paintera/test"},
				{"n5StorageSchemeAndFileDoubleMismatchExtension", "n5:file:///Users/hulbertc/projects/paintera/test.zarr"}
		});
	}


	@Override
	protected String tempN5Location() {

		try {
			return Files.createTempDirectory("n5-factory-test").toUri().toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static String tmpPathName(final String prefix) {

		try {
			final File tmpFile = Files.createTempDirectory(prefix).toFile();
			tmpFile.deleteOnExit();
			return tmpFile.getCanonicalPath();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected N5Writer createN5Writer() throws IOException {

		final String testDirPath = tmpPathName("n5-factory-test-");
		return factory.openWriter(testDirPath);
	}

	@Override
	protected N5Writer createN5Writer(final String location, final GsonBuilder gsonBuilder) throws IOException {

		return createN5Writer(location, gsonBuilder, ".", true);
	}

	protected N5ZarrWriter createN5Writer(final String location, final String dimensionSeparator) throws IOException {

		return createN5Writer(location, new GsonBuilder(), dimensionSeparator, true);
	}

	@Override
	protected N5Reader createN5Reader(final String location, final GsonBuilder gson) throws IOException {

		return new N5ZarrReader(location, gson);
	}

	protected N5ZarrWriter createN5Writer(
			final String location,
			final GsonBuilder gsonBuilder,
			final String dimensionSeparator,
			final boolean mapN5DatasetAttributes) throws IOException {

		return new N5ZarrWriter(location, gsonBuilder, dimensionSeparator, mapN5DatasetAttributes, false);
	}

	@Test
	public void storageSchemeTest() {

		final N5Reader reader = factory.getReader(uri);
	}
}
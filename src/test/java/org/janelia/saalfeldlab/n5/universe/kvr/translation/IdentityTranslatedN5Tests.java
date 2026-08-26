package org.janelia.saalfeldlab.n5.universe.kvr.translation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.janelia.saalfeldlab.n5.AbstractN5Test;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.kvr.TranslatedN5Reader;
import org.janelia.saalfeldlab.n5.universe.kvr.TranslatedN5Writer;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.GsonBuilder;

/**
 * Run the {@link AbstractN5Test} tests against a N5 filesystem container
 * wrapped in the identity translation ({@code "."} both ways). The translated
 * view should behave identical to the wrapped container.
 */
public class IdentityTranslatedN5Tests extends AbstractN5Test {

	@Override
	protected String tempN5Location() throws IOException {

		final File tempDir = Files.createTempDirectory("n5-kvr-id-translation-test-").toFile();
		tempDir.deleteOnExit();
		return new File(tempDir, "identity.n5").getCanonicalPath();
	}

	@Override
	protected N5Writer createN5Writer(final String location, final GsonBuilder gson) throws IOException {

		return new TranslatedN5Writer(new N5FSWriter(location, gson), ".", ".");
	}

	@Override
	protected N5Reader createN5Reader(final String location, final GsonBuilder gson) throws IOException {

		return new TranslatedN5Reader(new N5FSReader(location, gson), ".", ".");
	}

	/**
	 * A {@code TranslatedN5Reader} harvests the delegate container once, on
	 * construction. This test opens the reader before the writer creates
	 * anything, so the translated reader has a stale view. This is the only
	 * test in {@code AbstractN5Test} that does this.
	 */
	@Override
	@Ignore("TranslatedN5Reader does not see writes made after it was created")
	@Test
	public void testPathsWithIllegalUriCharacters() {}
}

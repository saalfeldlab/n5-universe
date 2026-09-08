package org.janelia.saalfeldlab.n5.universe.factory;

import org.janelia.saalfeldlab.n5.HttpKeyValueRoot;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;
import org.janelia.saalfeldlab.n5.universe.factory.options.BackendOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Test that {@link KeyValueRoots} creates {@link KeyValueRoot} instances for the various
 * backends, with and without the {@code readOnly} option.
 * <p>
 * The {@code kvr} replacement for {@code universe.backend.KeyValueAccessBackendTests}: the
 * {@code KeyValueAccessBackend} enum constants become the four named
 * {@code KeyValueRoots.create*} methods, and the paths are
 * {@link N5DirectoryPath}s rather than strings ({@code N5DirectoryPath.of("")} is the root,
 * which the old API spelled both {@code ""} and {@code "/"}).
 * <p>
 * Should only run with the run-backend-tests profile, hence the "Backend" in the name — these
 * talk to real S3 and GCS.
 */
public class KeyValueRootsBackendTests {

	private static final N5DirectoryPath ROOT = N5DirectoryPath.of("");

	static Random random = new Random();

	private static URI s3Uri, gcsUri, fsUri, httpUri;

	private static BackendOptions options() {

		return new BackendOptions();
	}

	@BeforeClass
	public static void before() {

		s3Uri = URI.create("s3://should-not-exist-bucket-" + random.nextLong());
		gcsUri = URI.create("gs://should-not-exist-bucket-" + random.nextLong());
		fsUri = URI.create("file://should-not-exist-path-" + random.nextLong());
		httpUri = URI.create("https://localhost");
	}

	@AfterClass
	public static void after() {

		final KeyValueRoot s3 = KeyValueRoots.createAmazonS3(s3Uri, false, options());
		s3.delete(ROOT); // removes the bucket

		final KeyValueRoot gcs = KeyValueRoots.createGoogleCloud(gcsUri, false, options());
		gcs.delete(ROOT); // removes the bucket
	}

	@Test
	public void testS3KeyValueRootCreation() {

		// throw error when "readOnly" on a non-existing bucket
		assertThrows(N5IOException.class, () -> KeyValueRoots.createAmazonS3(s3Uri, true, options()));

		final KeyValueRoot root = KeyValueRoots.createAmazonS3(s3Uri, false, options());
		assertNotNull(root);
		assertTrue(root.isDirectory(ROOT)); // equivalent to checking if the bucket exists
	}

	@Test
	public void testGcsKeyValueRootCreation() {

		final KeyValueRoot readOnly = KeyValueRoots.createGoogleCloud(gcsUri, true, options());
		assertFalse(readOnly.exists(ROOT)); // bucket should not exist

		final KeyValueRoot root = KeyValueRoots.createGoogleCloud(gcsUri, false, options());
		assertNotNull(root);

		// GCS does not create the bucket on root creation
		assertFalse(root.isDirectory(ROOT));

		root.createDirectories(ROOT);
		assertTrue(root.isDirectory(ROOT));
	}

	@Test
	public void testFSKeyValueRootCreation() {

		// root of file system should always exist
		final KeyValueRoot root = KeyValueRoots.createFileSystem(fsUri);
		assertTrue(root.exists(ROOT));

		// NB: There is no readOnly variant. The old KeyValueAccessBackend.FILE took the flag
		// and ignored it — a FileSystemKeyValueAccess is the same object either way — so
		// createFileSystem does not ask for it.
		assertTrue(KeyValueRoots.create(fsUri, true, options()).exists(ROOT));
	}

	@Test
	public void testHttpKeyValueRootCreation() {

		// plain HTTP is read-only by nature, so createHttp takes no readOnly flag
		final KeyValueRoot root = KeyValueRoots.createHttp(httpUri, options());
		assertNotNull(root);
		assertTrue(root instanceof HttpKeyValueRoot);

		/* Writing through it is what fails, and at the write rather than at creation: every
		 * mutating method of HttpKeyValueRoot throws N5IOException("...is read-only").
		 * The old KeyValueAccessBackend.HTTP threw UnsupportedOperationException from
		 * apply(uri, factory, false), i.e. at creation time. */
		assertThrows(N5IOException.class, () -> root.createDirectories(ROOT));
		assertThrows(N5IOException.class, () -> root.delete(ROOT));
	}

	/**
	 * NB: Deliberate behaviour change from {@code KeyValueAccessBackendTests}, which asserted
	 * that an {@code http(s)} URI for writing throws {@link UnsupportedOperationException}. It
	 * is now an S3 attempt: at that point the URI can only be an S3-compatible endpoint, since
	 * {@code createFromHttpUri} has already handled the Google Cloud Storage hosts. See
	 * CLAUDE.md, "Behaviour changes to flag in the PR".
	 */
	@Test
	public void testHttpUriForWritingGoesToS3() {

		assertThrows(Exception.class, () -> KeyValueRoots.create(httpUri, false, options()));
	}
}

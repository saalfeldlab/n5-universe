package org.janelia.saalfeldlab.n5.universe.backend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Random;

import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.universe.KeyValueAccessBackend;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the behavior of KeyValueAccessBackend to create KeyValueAccess instances
 * for various backends with and without the "readOnly" option.
 * 
 * Should only run with the run-backend-tests profile, hence the "Backend" in
 * the name.
 */
public class KeyValueAccessBackendTests {

	static Random random = new Random();
	private static URI s3Uri, gcsUri, fsUri, httpUri;

	@BeforeClass
	public static void before() {

		s3Uri = URI.create("s3://should-not-exist-bucket-" + random.nextLong());
		gcsUri = URI.create("gs://should-not-exist-bucket-" + random.nextLong());
		fsUri = URI.create("file://should-not-exist-path-" + random.nextLong());
		httpUri = URI.create("https://localhost");
	}

	@AfterClass
	public static void after() {

		final KeyValueAccess s3 = KeyValueAccessBackend.AWS.apply(s3Uri, new N5Factory(), false);
		s3.delete("/"); // removes the bucket

		final KeyValueAccess gcs = KeyValueAccessBackend.GOOGLE_CLOUD.apply(gcsUri, new N5Factory(), false);
		gcs.delete("/"); // removes the bucket
	}

	@Test
	public void testS3KeyValueAccessCreation() {

		// throw error when "readOnly" on a non-existing bucket
		assertThrows(N5IOException.class, () -> KeyValueAccessBackend.AWS.apply(s3Uri, new N5Factory(), true));

		final KeyValueAccess kva = KeyValueAccessBackend.AWS.apply(s3Uri, new N5Factory(), false);
		assertNotNull(kva);
		assertTrue(kva.isDirectory("/")); // equivalent to checking if the
											// bucket exists
	}

	@Test
	public void testGcsKeyValueAccessCreation() {

		final KeyValueAccess kvaReadOnly = KeyValueAccessBackend.GOOGLE_CLOUD.apply(gcsUri, new N5Factory(), true);
		assertFalse(kvaReadOnly.exists("")); // bucket should not exist

		final KeyValueAccess kva = KeyValueAccessBackend.GOOGLE_CLOUD.apply(gcsUri, new N5Factory(), false);
		assertNotNull(kva);

		// GCS does not create the bucket on kva creation
		assertFalse(kva.isDirectory("/"));

		kva.createDirectories("");
		assertTrue(kva.isDirectory("/"));
	}

	@Test
	public void testFSKeyValueAccessCreation() {

		// root of file system should always exist
		final KeyValueAccess kvaReadOnly = KeyValueAccessBackend.FILE.apply(fsUri, new N5Factory(), true);
		assertTrue(kvaReadOnly.exists(""));

		final KeyValueAccess kva = KeyValueAccessBackend.FILE.apply(fsUri, new N5Factory(), false);
		assertTrue(kva.exists(""));
	}

	@Test
	public void testHttpKeyValueAccessCreation() {

		// root of file system should always exist
		final KeyValueAccess kvaReadOnly = KeyValueAccessBackend.HTTP.apply(httpUri, new N5Factory(), true);

		// http does not support writing
		assertThrows(UnsupportedOperationException.class, () -> KeyValueAccessBackend.HTTP.apply(httpUri, new N5Factory(), false));
	}

}

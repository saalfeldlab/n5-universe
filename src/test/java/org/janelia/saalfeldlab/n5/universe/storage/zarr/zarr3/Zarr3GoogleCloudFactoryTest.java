package org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3;

import com.google.cloud.storage.Storage;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageTests;
import org.janelia.saalfeldlab.n5.googlecloud.backend.BackendGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.googlecloud.mock.MockGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrStorageTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.net.URI;
import java.net.URISyntaxException;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;
import static org.junit.Assert.assertTrue;

public abstract class Zarr3GoogleCloudFactoryTest extends ZarrStorageTests.Zarr3FactoryTest {

	protected static String testBucket = N5GoogleCloudStorageTests.tempBucketName();
	protected static Storage storage = null;
	protected static N5Factory FACTORY = new N5Factory() {

		@Override protected Storage createGoogleCloudStorage() {

			return storage;
		}
	};

	@Override public Class<?> getBackendTargetClass() {

		return GoogleCloudStorageKeyValueAccess.class;
	}

	@AfterClass
	public static void removeTestBucket() {

		try {
			FACTORY.openWriter("gs://" + testBucket).remove("");
		} catch (Throwable e) {
			System.err.println("Error during test cleanup. Bucket " + testBucket + " may still exist.");
		}
	}

	@Override public N5Factory getFactory() {

		return factory != null ? factory : (factory = FACTORY);
	}

	@Override protected String tempN5Location() {

		try {
			return new URI("gs", testBucket, tempContainerPath(), null).toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ZarrGoogleCloudMockTest extends Zarr3GoogleCloudFactoryTest {
		public ZarrGoogleCloudMockTest() {

			Zarr3GoogleCloudFactoryTest.storage = MockGoogleCloudStorageFactory.getOrCreateStorage();
		}

		@Override protected String[] illegalChars() {

			// NOTE: This is currently identical to the AbstractN5Test version, except we remove "%" from the array of illegalChars to test.
			// 	Specifically we ONLY remove it in this Mock version of the test. The actual backend has no problem, and the test passes
			//	just fine, but the mock version seems to have some incorrect handling of % in keys. Likely because % is a valid UTF-8 character
			//	but requires special encoding for URL over HTTP, and it seems the mock channels don't handle that case correctly
			return new String[] {" ", "#"};
		}
	}

	public static class ZarrGoogleCloudBackendTest extends Zarr3GoogleCloudFactoryTest {

		@BeforeClass
		public static void ensureBucketExists() {

			final N5Writer writer = N5Factory.createWriter("gs://" + testBucket + "/" + tempContainerPath());
			assertTrue(writer.exists(""));
			writer.remove();
		}

		public ZarrGoogleCloudBackendTest() {

			Zarr3GoogleCloudFactoryTest.storage = BackendGoogleCloudStorageFactory.getOrCreateStorage();
		}
	}
}

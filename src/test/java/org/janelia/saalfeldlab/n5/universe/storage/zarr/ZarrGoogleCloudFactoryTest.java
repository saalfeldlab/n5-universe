package org.janelia.saalfeldlab.n5.universe.storage.zarr;

import com.google.cloud.storage.Storage;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageTests;
import org.janelia.saalfeldlab.n5.googlecloud.backend.BackendGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.googlecloud.mock.MockGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.net.URI;
import java.net.URISyntaxException;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;
import static org.junit.Assert.assertTrue;

public abstract class ZarrGoogleCloudFactoryTest extends ZarrStorageTests.ZarrFactoryTest {

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

	public static class ZarrGoogleCloudMockTest extends ZarrGoogleCloudFactoryTest {
		public ZarrGoogleCloudMockTest() {

			ZarrGoogleCloudFactoryTest.storage = MockGoogleCloudStorageFactory.getOrCreateStorage();
		}
	}

	public static class ZarrGoogleCloudBackendTest extends ZarrGoogleCloudFactoryTest {

		@BeforeClass
		public static void ensureBucketExists() {

			final N5Writer writer = N5Factory.createWriter("gs://" + testBucket + "/" + tempContainerPath());
			assertTrue(writer.exists(""));
			writer.remove();
		}

		public ZarrGoogleCloudBackendTest() {

			ZarrGoogleCloudFactoryTest.storage = BackendGoogleCloudStorageFactory.getOrCreateStorage();
		}
	}
}

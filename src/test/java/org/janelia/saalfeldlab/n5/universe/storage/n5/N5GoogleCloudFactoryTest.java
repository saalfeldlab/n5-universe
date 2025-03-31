package org.janelia.saalfeldlab.n5.universe.storage.n5;

import com.google.cloud.storage.Storage;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageTests;
import org.janelia.saalfeldlab.n5.googlecloud.backend.BackendGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.googlecloud.mock.MockGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;
import static org.junit.Assert.assertTrue;

public abstract class N5GoogleCloudFactoryTest extends N5StorageTests.N5FactoryTest {

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

	public static class N5GoogleCloudMockTest extends N5GoogleCloudFactoryTest {
		public N5GoogleCloudMockTest() {

			N5GoogleCloudFactoryTest.storage = MockGoogleCloudStorageFactory.getOrCreateStorage();
		}
	}

	public static class N5GoogleCloudBackendTest extends N5GoogleCloudFactoryTest {

		@BeforeClass
		public static void ensureBucketExists() {

			final N5Writer writer = N5Factory.createWriter("gs://" + testBucket + "/" + tempContainerPath());
			assertTrue(writer.exists(""));
			writer.remove();
		}

		public N5GoogleCloudBackendTest() {

			N5GoogleCloudFactoryTest.storage = BackendGoogleCloudStorageFactory.getOrCreateStorage();
		}

		@Override public void testVersion() throws NumberFormatException, IOException, URISyntaxException {

			super.testVersion();
		}
	}
}

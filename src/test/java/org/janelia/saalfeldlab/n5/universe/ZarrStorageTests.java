package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageTests;
import org.janelia.saalfeldlab.n5.googlecloud.backend.BackendGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.googlecloud.mock.MockGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests;
import org.janelia.saalfeldlab.n5.s3.mock.MockS3Factory;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempBucketName;
import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;
import static org.junit.Assert.assertTrue;

@RunWith(Suite.class)
@Suite.SuiteClasses({ZarrStorageTests.ZarrFileSystemTest.class, ZarrStorageTests.ZarrAmazonS3MockTest.class, ZarrStorageTests.ZarrGoogleCloudMockTest.class})
public class ZarrStorageTests {

	public static abstract class ZarrFactoryTest extends N5ZarrTest implements StorageSchemeWrappedN5Test {

		protected N5Factory factory;

		public ZarrFactoryTest() {

			this.factory = getFactory();
		}

		@Override abstract protected String tempN5Location();

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory();
			}
			return factory;
		}

		@Override public N5Factory.StorageFormat getStorageFormat() {

			return N5Factory.StorageFormat.ZARR;
		}

		@Override protected N5Writer createN5Writer() {

			return getWriter(tempN5Location());
		}

		@Override protected N5Writer createTempN5Writer(String location, GsonBuilder gsonBuilder, String dimensionSeparator, boolean mapN5DatasetAttributes) {

			factory.gsonBuilder(gsonBuilder);
			factory.zarrDimensionSeparator(dimensionSeparator);
			factory.zarrMapN5Attributes(mapN5DatasetAttributes);
			final N5Writer writer = getWriter(location);
			tempWriters.add(writer);
			return writer;
		}

		@Override protected N5Reader createN5Reader(String location, GsonBuilder gson) {

			factory.gsonBuilder(gson);
			return getReader(location);
		}

		@Override protected N5Writer createN5Writer(String location, GsonBuilder gson) {

			factory.gsonBuilder(gson);
			return getWriter(location);
		}

		@Override protected N5Writer createN5Writer(String location) {

			return getWriter(location);
		}

		@Override protected N5Reader createN5Reader(String location) {

			return getReader(location);
		}

		@Ignore
		@Override public void testReadZarrPython() {

		}

		@Ignore
		@Override public void testReadZarrNestedPython() {

		}
	}

	public static class ZarrFileSystemTest extends ZarrFactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return FileSystemKeyValueAccess.class;
		}

		@Override protected String tempN5Location() {

			try {
				return Files.createTempDirectory("zarr-test").toUri().getPath();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static abstract class ZarrAmazonS3FactoryTest extends ZarrFactoryTest {

		public static AmazonS3 s3 = null;

		final static String testBucket = tempBucketName();

		@Override public Class<?> getBackendTargetClass() {

			return AmazonS3KeyValueAccess.class;
		}

		@AfterClass
		public static void removeTestBucket() {

			if (s3 != null && s3.doesBucketExistV2(testBucket))
				N5Factory.createWriter("s3://" + testBucket).remove();
		}

		@Override public N5Factory getFactory() {

			if (factory != null)
				return factory;
			factory = new N5Factory() {

				@Override AmazonS3 createS3(String uri) {

					if (ZarrAmazonS3FactoryTest.s3 == null)
						ZarrAmazonS3FactoryTest.s3 = super.createS3(uri);
					return s3;
				}
			};
			return factory;
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("s3", testBucket, tempContainerPath(), null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class ZarrAmazonS3MockTest extends ZarrAmazonS3FactoryTest {
		public ZarrAmazonS3MockTest() {

			ZarrAmazonS3FactoryTest.s3 = MockS3Factory.getOrCreateS3();
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("http", "localhost:8001", "/" + testBucket + tempContainerPath(), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class ZarrAmazonS3BackendTest extends ZarrAmazonS3FactoryTest {

		@BeforeClass
		public static void ensureBucketExists() {

			final N5Writer writer = N5Factory.createWriter("s3://" + testBucket + "/" + tempContainerPath());
			assertTrue(writer.exists(""));
			writer.remove();
		}

		@Rule public TestWatcher skipIfErroneousFailure = new N5AmazonS3Tests.SkipErroneousNoSuchBucketFailure();

		public ZarrAmazonS3BackendTest() {

			ZarrAmazonS3FactoryTest.s3 = null;
		}
	}

	public static abstract class ZarrGoogleCloudFactoryTest extends ZarrFactoryTest {

		protected static String testBucket = N5GoogleCloudStorageTests.tempBucketName();
		protected static Storage storage = null;

		@Override public Class<?> getBackendTargetClass() {

			return GoogleCloudStorageKeyValueAccess.class;
		}

		@AfterClass
		public static void removeTestBucket() {

			final Bucket bucket = storage.get(testBucket);
			if (bucket != null && bucket.exists()) {
				storage.delete(testBucket);
			}
		}

		@Override public N5Factory getFactory() {

			if (factory != null)
				return factory;
			factory = new N5Factory() {

				@Override Storage createGoogleCloudStorage() {

					return storage;
				}
			};
			return factory;
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("gs", testBucket, tempContainerPath(), null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
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

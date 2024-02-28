package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.AbstractN5Test;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageTest;
import org.janelia.saalfeldlab.n5.googlecloud.mock.MockGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.mock.MockS3Factory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempBucketName;
import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;

@RunWith(Suite.class)
@Suite.SuiteClasses({N5StorageTests.N5FileSystemTest.class, N5StorageTests.N5AmazonS3MockTest.class, N5StorageTests.N5AmazonS3BackendTest.class})
public class N5StorageTests {

	public static abstract class N5FactoryTest extends AbstractN5Test implements StorageSchemeWrappedN5Test {

		protected N5Factory factory;

		protected final ArrayList<N5Writer> tempWriters = new ArrayList<>();

		public N5FactoryTest() {

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

			return N5Factory.StorageFormat.N5;
		}

		@Override protected N5Reader createN5Reader(String location, GsonBuilder gson) {

			factory.gsonBuilder(gson);
			return createN5Writer(location);
		}

		@Override protected N5Reader createN5Reader(String location) {

			return getReader(location);
		}

		@Override protected N5Writer createN5Writer(String location, GsonBuilder gson) {

			factory.gsonBuilder(gson);
			return createN5Writer(location);
		}

		@Override protected N5Writer createN5Writer(String location) {

			return getWriter(location);
		}

		@After
		public void removeTempWriter() {

			synchronized (tempWriters) {

				for (N5Writer tempWriter : tempWriters) {
					tempWriter.remove();
				}
				tempWriters.clear();
			}
		}
	}

	public static class N5FileSystemTest extends N5FactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return FileSystemKeyValueAccess.class;
		}

		@Override protected String tempN5Location() {

			try {
				return Files.createTempDirectory("n5-test").toUri().getPath();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static abstract class N5AmazonS3FactoryTest extends N5FactoryTest {

		public static AmazonS3 s3 = null;

		final static String testBucket = tempBucketName();

		@Override public Class<?> getBackendTargetClass() {

			return AmazonS3KeyValueAccess.class;
		}

		@AfterClass
		public static void removeTestBucket() {
			if (s3.doesBucketExistV2(testBucket))
				s3.deleteBucket(testBucket);
		}

	}

	public static  class N5AmazonS3MockTest extends N5AmazonS3FactoryTest {
		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory() {

					@Override AmazonS3 createS3(String uri) {

						AmazonS3 s3 = MockS3Factory.getOrCreateS3();
						N5AmazonS3FactoryTest.s3 = s3;
						return s3;
					}
				};
			}
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

	public static class N5AmazonS3BackendTest extends N5AmazonS3FactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return AmazonS3KeyValueAccess.class;
		}

		@Override protected String tempN5Location() {

			try {
				final String s3ContainerUri = new URI("s3", testBucket, tempContainerPath(), null).toString();
				if (s3 == null) {
					s3 = factory.createS3(s3ContainerUri);
				}
				return s3ContainerUri;
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static abstract class N5GoogleCloudFactoryTest extends N5FactoryTest {

		private static String testBucket = N5GoogleCloudStorageTest.tempBucketName();
		private static Storage storage = null;

		protected abstract Storage getOrCreateStorage();

		@Override public Class<?> getBackendTargetClass() {

			return GoogleCloudStorageKeyValueAccess.class;
		}

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory() {

					@Override Storage createGoogleCloudStorage() {

						final Storage storage = getOrCreateStorage();
						N5GoogleCloudFactoryTest.storage = storage;
						return storage;
					}
				};
			}
			return factory;
		}

		@Override
		protected String tempN5Location() {

			try {
				return new URI("gs", testBucket, tempContainerPath(), null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}



		@AfterClass
		public static void removeTestBucket() {

			final Bucket bucket = storage.get(testBucket);
			if (bucket != null && bucket.exists()) {
				storage.delete(testBucket);
			}
		}
	}

	public static class N5GoogleCloudMockTest extends N5GoogleCloudFactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return GoogleCloudStorageKeyValueAccess.class;
		}

		@Override protected Storage getOrCreateStorage() {

			return MockGoogleCloudStorageFactory.getOrCreateStorage();
		}


	}
}

package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempBucketName;
import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;

@RunWith(Suite.class)
@Suite.SuiteClasses({N5StorageTests.N5FileSystemTest.class, N5StorageTests.N5AmazonS3Test.class})
public class N5StorageTests {

	public static abstract class N5FactoryTest extends AbstractN5Test implements StorageSchemeWrappedN5Test {

		protected N5Factory factory;

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

		@Override protected N5Writer createN5Writer() {

			return getWriter(tempN5Location());
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

	public static class N5AmazonS3Test extends N5FactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return AmazonS3KeyValueAccess.class;
		}

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory() {

					@Override AmazonS3 createS3(String uri) {

						return MockS3Factory.getOrCreateS3();
					}
				};
			}
			return factory;
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("http", "localhost:8001", "/" + tempBucketName(factory.createS3(null)) + tempContainerPath(), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class N5GoogleCloudTest extends N5FactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return GoogleCloudStorageKeyValueAccess.class;
		}

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory() {

					@Override Storage createGoogleCloudStorage() {

						return MockGoogleCloudStorageFactory.getOrCreateStorage();
					}
				};
			}
			return factory;
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("gs", N5GoogleCloudStorageTest.tempBucketName(factory.createGoogleCloudStorage()), tempContainerPath(), null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

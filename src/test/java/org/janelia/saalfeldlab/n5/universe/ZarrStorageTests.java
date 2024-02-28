package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageTest;
import org.janelia.saalfeldlab.n5.googlecloud.mock.MockGoogleCloudStorageFactory;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.backend.BackendS3Factory;
import org.janelia.saalfeldlab.n5.s3.mock.MockS3Factory;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrTest;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempBucketName;
import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;

@RunWith(Suite.class)
@Suite.SuiteClasses({ZarrStorageTests.ZarrFileSystemTest.class, ZarrStorageTests.ZarrAmazonS3Test.class})
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
			return getWriter(location);
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
		@Override public void testReadZarrPython() {}

		@Ignore
		@Override public void testReadZarrNestedPython() {}
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

	public static class ZarrAmazonS3Test extends ZarrFactoryTest {

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

		final String testBucket = tempBucketName();

		@Override protected String tempN5Location() {

			try {
//				return new URI("s3", testBucket, tempContainerPath(), null).toString();
				return new URI("http", "localhost:8001", "/" + testBucket + tempContainerPath(), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}



	public static class ZarrAmazonS3BackendTest extends N5StorageTests.N5FactoryTest {

		@Override public Class<?> getBackendTargetClass() {

			return AmazonS3KeyValueAccess.class;
		}

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory(); // {
//
//					@Override AmazonS3 createS3(String uri) {
//
//						return BackendS3Factory.getOrCreateS3();
//					}
//				};
			}
			return factory;
		}

		final String testBucket = tempBucketName();

		@Override protected String tempN5Location() {

			try {
				return new URI("s3", testBucket, tempContainerPath(), null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class ZarrGoogleCloudTest extends ZarrStorageTests.ZarrFactoryTest {

		private String testBucket = N5GoogleCloudStorageTest.tempBucketName();

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
				return new URI("gs", testBucket, tempContainerPath(), null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

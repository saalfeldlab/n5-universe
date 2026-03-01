package org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests;
import org.janelia.saalfeldlab.n5.s3.mock.MockS3Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.storage.AmazonTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrStorageTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestWatcher;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.net.URISyntaxException;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempBucketName;
import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;
import static org.junit.Assert.assertTrue;

@Category({AmazonTest.class})
public abstract class Zarr2AmazonS3FactoryTest extends ZarrStorageTests.Zarr2FactoryTest {

	public static S3Client s3 = null;

	final static String testBucket = tempBucketName();

	final static N5Factory FACTORY = new N5Factory() {

		@Override protected S3Client createS3(String uri) {

			return s3 != null ? s3 : (s3 = super.createS3(uri));
		}
	};

	@Override public Class<?> getBackendTargetClass() {

		return AmazonS3KeyValueAccess.class;
	}

	@AfterClass
	public static void removeTestBucket() {
		try {
			FACTORY.openWriter("s3://" + testBucket).remove("");
		} catch (Throwable e) {
			System.err.println("Error during test cleanup. Bucket " + testBucket + " may still exist.");
		}
	}

	@Override public N5Factory getFactory() {

		return factory != null ? factory : (factory = FACTORY);
	}

	@Override protected String tempN5Location() {

		try {
			return new URI("s3", testBucket, tempContainerPath(), null).toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ZarrAmazonS3MockTest extends Zarr2AmazonS3FactoryTest {
		public ZarrAmazonS3MockTest() {

			Zarr2AmazonS3FactoryTest.s3 = MockS3Factory.getOrCreateS3();
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("http", "localhost:9000", "/" + testBucket + tempContainerPath(), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class ZarrAmazonS3BackendTest extends Zarr2AmazonS3FactoryTest {

		@BeforeClass
		public static void ensureBucketExists() {

			final N5Writer writer = FACTORY.openWriter("s3://" + testBucket + "/" + tempContainerPath());
			assertTrue(writer.exists(""));
			writer.remove();
		}

		@Rule public TestWatcher skipIfErroneousFailure = new N5AmazonS3Tests.SkipErroneousNoSuchBucketFailure();

		public ZarrAmazonS3BackendTest() {

			Zarr2AmazonS3FactoryTest.s3 = null;
		}
	}
}

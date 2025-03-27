package org.janelia.saalfeldlab.n5.universe.storage.n5;

import com.amazonaws.services.s3.AmazonS3;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests;
import org.janelia.saalfeldlab.n5.s3.mock.MockS3Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;

import java.net.URI;
import java.net.URISyntaxException;

import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempBucketName;
import static org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests.tempContainerPath;
import static org.junit.Assert.assertTrue;

public abstract class N5AmazonS3FactoryTest extends N5StorageTests.N5FactoryTest {

	public static AmazonS3 s3 = null;
	final static String testBucket = tempBucketName();

	final static N5Factory FACTORY = new N5Factory() {

		@Override protected AmazonS3 createS3(String uri) {

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

	public static class N5AmazonS3MockTest extends N5AmazonS3FactoryTest {
		public N5AmazonS3MockTest() {

			N5AmazonS3FactoryTest.s3 = MockS3Factory.getOrCreateS3();
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("http", "localhost:8001", "/" + testBucket + tempContainerPath(), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class N5AmazonS3BackendTest extends N5AmazonS3FactoryTest {

		@BeforeClass
		public static void ensureBucketExists() {

			final N5Writer writer = FACTORY.createWriter("s3://" + testBucket + "/" + tempContainerPath());
			assertTrue(writer.exists(""));
			writer.remove();
		}

		@Rule public TestWatcher skipIfErroneousFailure = new N5AmazonS3Tests.SkipErroneousNoSuchBucketFailure();

		public N5AmazonS3BackendTest() {

			N5AmazonS3FactoryTest.s3 = null;
		}
	}
}

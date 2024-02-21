package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.s3.AbstractN5AmazonS3Test;
import org.janelia.saalfeldlab.n5.s3.mock.MockS3Factory;
import org.janelia.saalfeldlab.n5.s3.mock.N5AmazonS3ContainerPathMockTest;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

@RunWith(Suite.class)
@Suite.SuiteClasses({ZarrStorageTests.ZarrFileSystemTest.class, ZarrStorageTests.ZarrAwsS3MockTest.class})
public class ZarrStorageTests {

	public static class ZarrFileSystemTest extends N5ZarrTest implements StorageSchemeWrappedN5Test {

		private final N5Factory factory;

		public ZarrFileSystemTest() {

			this.factory = new N5Factory();
		}

		@Override public N5Factory getFactory() {

			return factory;
		}

		@Override public N5Factory.StorageFormat getStorageFormat() {

			return N5Factory.StorageFormat.ZARR;
		}

		@Override protected N5Writer createN5Writer() {

			return getWriter(tempN5Location());
		}

		@Override protected N5Writer createN5Writer(String location, GsonBuilder gsonBuilder, String dimensionSeparator, boolean mapN5DatasetAttributes) {

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
	}

	public static class ZarrAwsS3MockTest extends N5ZarrTest implements StorageSchemeWrappedN5Test {
		private static String bucketName;
		private final N5Factory factory;

		public ZarrAwsS3MockTest() {

			this.factory = new N5Factory() {

				@Override AmazonS3 createS3(String uri) {

					return MockS3Factory.getOrCreateS3();
				}

				@Override String getS3Bucket(String uri) {

					try {
						final String path = new URI(uri).getPath().replaceFirst("^/", "");
						return path.substring(0, path.indexOf('/'));
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
				}
			};
		}


		@BeforeClass
		public static void setup() {
			bucketName = AbstractN5AmazonS3Test.tempBucketName();
		}

		@AfterClass
		public static void cleanup() {

			MockS3Factory.getOrCreateS3().deleteBucket(bucketName);
		}

		@Override
		protected N5Writer createN5Writer(final String location, final String dimensionSeparator) {

			factory.zarrDimensionSeparator(dimensionSeparator);
			return getWriter(location);
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("http", "localhost:8001", "/" + bucketName + AbstractN5AmazonS3Test.tempContainerPath(), null, null).toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

		@Override public N5Factory getFactory() {

			return factory;
		}

		@Override public N5Factory.StorageFormat getStorageFormat() {

			return N5Factory.StorageFormat.ZARR;
		}

		@Override protected N5Writer createN5Writer() {

			return getWriter(tempN5Location());
		}

		@Override protected N5Writer createN5Writer(String location, GsonBuilder gson) {

			factory.gsonBuilder(gson);
			return getWriter(location);
		}

		@Override protected N5Reader createN5Reader(String location, GsonBuilder gson) {

			factory.gsonBuilder(gson);
			return getReader(location);
		}

		@Override protected N5Writer createN5Writer(String location) {

			return getWriter(location);
		}

		@Override protected N5Reader createN5Reader(String location) {

			return getReader(location);

		}


		/**
		 * Currently, {@code N5AmazonS3Reader#exists(String)} is implemented by listing objects under that group.
		 * This test case specifically tests its correctness.
		 *
		 * @throws IOException
		 */
		@Test
		public void testExistsUsingListingObjects() {

			try (N5Writer n5 = createN5Writer()) {
				n5.createGroup("/one/two/three");

				Assert.assertTrue(n5.exists(""));
				Assert.assertTrue(n5.exists("/"));

				Assert.assertTrue(n5.exists("one"));
				Assert.assertTrue(n5.exists("one/"));
				Assert.assertTrue(n5.exists("/one"));
				Assert.assertTrue(n5.exists("/one/"));

				Assert.assertTrue(n5.exists("one/two"));
				Assert.assertTrue(n5.exists("one/two/"));
				Assert.assertTrue(n5.exists("/one/two"));
				Assert.assertTrue(n5.exists("/one/two/"));

				Assert.assertTrue(n5.exists("one/two/three"));
				Assert.assertTrue(n5.exists("one/two/three/"));
				Assert.assertTrue(n5.exists("/one/two/three"));
				Assert.assertTrue(n5.exists("/one/two/three/"));

				Assert.assertFalse(n5.exists("one/tw"));
				Assert.assertFalse(n5.exists("one/tw/"));
				Assert.assertFalse(n5.exists("/one/tw"));
				Assert.assertFalse(n5.exists("/one/tw/"));

				Assert.assertArrayEquals(new String[]{"one"}, n5.list("/"));
				Assert.assertArrayEquals(new String[]{"two"}, n5.list("/one"));
				Assert.assertArrayEquals(new String[]{"three"}, n5.list("/one/two"));

				Assert.assertArrayEquals(new String[]{}, n5.list("/one/two/three"));
				assertThrows(N5Exception.N5IOException.class, () -> n5.list("/one/tw"));

				Assert.assertTrue(n5.remove("/one/two/three"));
				Assert.assertFalse(n5.exists("/one/two/three"));
				Assert.assertTrue(n5.exists("/one/two"));
				Assert.assertTrue(n5.exists("/one"));

				Assert.assertTrue(n5.remove("/one"));
				Assert.assertFalse(n5.exists("/one/two"));
				Assert.assertFalse(n5.exists("/one"));
			}
		}
	}
}

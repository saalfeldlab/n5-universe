package org.janelia.saalfeldlab.n5.universe.storage.n5;

import com.google.cloud.storage.Storage;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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

		@Override public void testPathsWithIllegalUriCharacters()  {

			// Omit '%' from illegal char test for mock test only. Works fine and is tested correctly against backend tests. See
			// n5-google-cloud test for more detailed info

			try (N5Writer writer = createTempN5Writer()) {
				try (N5Reader reader = createN5Reader(writer.getURI().toString())) {

					final String[] illegalChars = {" ", "#"};
					for (final String illegalChar : illegalChars) {
						final String groupWithIllegalChar = "test" + illegalChar + "group";
						assertThrows("list over group should throw prior to create", N5Exception.N5IOException.class, () -> writer.list(groupWithIllegalChar));
						writer.createGroup(groupWithIllegalChar);
						assertTrue("Newly created group should exist", writer.exists(groupWithIllegalChar));
						assertArrayEquals("list over empty group should be empty list", new String[0], writer.list(groupWithIllegalChar));
						writer.setAttribute(groupWithIllegalChar, "/a/b/key1", "value1");
						final String attrFromWriter = writer.getAttribute(groupWithIllegalChar, "/a/b/key1", String.class);
						final String attrFromReader = reader.getAttribute(groupWithIllegalChar, "/a/b/key1", String.class);
						assertEquals("value1", attrFromWriter);
						assertEquals("value1", attrFromReader);


						final String datasetWithIllegalChar = "test" + illegalChar + "dataset";
						final DatasetAttributes datasetAttributes = new DatasetAttributes(dimensions, blockSize, DataType.UINT64, new RawCompression());
						writer.createDataset(datasetWithIllegalChar, datasetAttributes);
						final DatasetAttributes datasetFromWriter = writer.getDatasetAttributes(datasetWithIllegalChar);
						final DatasetAttributes datasetFromReader = reader.getDatasetAttributes(datasetWithIllegalChar);
						assertDatasetAttributesEquals(datasetAttributes, datasetFromWriter);
						assertDatasetAttributesEquals(datasetAttributes, datasetFromReader);
					}
				}
			}
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

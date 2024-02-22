package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.reflect.TypeToken;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.StringDataBlock;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Tests;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrTest;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrStringDataBlock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(Suite.class)
@Suite.SuiteClasses({ZarrStorageTests.ZarrFileSystemTest.class, ZarrStorageTests.ZarrAwsS3Tests.class})
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

	public static class ZarrAwsS3Tests extends N5AmazonS3Tests implements StorageSchemeWrappedN5Test {
		private final N5Factory factory;

		public ZarrAwsS3Tests() {

			this.factory = new N5Factory() {

				@Override AmazonS3 createS3(String uri) {

					return getS3();
				}

				@Override String getS3Bucket(String uri) {

					if (useBackend) {
						return super.getS3Bucket(uri);
					}
					try {
						final String path = new URI(uri).getPath().replaceFirst("^/", "");
						return path.substring(0, path.indexOf('/'));
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
				}
			};
		}

		@Override protected String tempN5Location() {

			try {
				return new URI("http", "localhost:8001", "/" + tempBucketName(getS3()) + tempContainerPath(), null, null).toString();
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

		@Override
		@Test
		public void testCreateDataset() {

			final DatasetAttributes info;
			try (N5Writer n5 = createN5Writer()) {
				n5.createDataset(datasetName, dimensions, blockSize, DataType.UINT64, getCompressions()[0]);

				assertTrue("Dataset does not exist", n5.exists(datasetName));

				info = n5.getDatasetAttributes(datasetName);
				assertArrayEquals(dimensions, info.getDimensions());
				assertArrayEquals(blockSize, info.getBlockSize());
				assertEquals(DataType.UINT64, info.getDataType());
				assertEquals(getCompressions()[0].getClass(), info.getCompression().getClass());
			}
		}

		@Override
		@Test
		public void testVersion() throws NumberFormatException {

			try (final N5Writer writer = createN5Writer()) {

				final ZarrKeyValueWriter zarr = (ZarrKeyValueWriter)writer;
				final N5Reader.Version n5Version = writer.getVersion();
				final N5Reader.Version expectedVersion = new N5Reader.Version(2, 0, 0);
				assertEquals(n5Version, expectedVersion);
			}
		}

		@Override
		@Test
		@Ignore("Zarr does not currently support mode 1 data blocks.")
		public void testMode1WriteReadByteBlock() {

		}

		@Override
		@Test
		@Ignore("Zarr does not currently support mode 2 data blocks and serialized objects.")
		public void testWriteReadSerializableBlock() {

		}

		@Test
		@Override
		public void testAttributes() {

			try (final N5Writer n5 = createN5Writer()) {
				n5.createGroup(groupName);

				n5.setAttribute(groupName, "key1", "value1");
				// length 2 because it includes "zarr_version"
				Assert.assertEquals(2, n5.listAttributes(groupName).size());
				/* class interface */
				Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", String.class));
				/* type interface */
				Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", new TypeToken<String>() {

				}.getType()));

				final Map<String, String> newAttributes = new HashMap<>();
				newAttributes.put("key2", "value2");
				newAttributes.put("key3", "value3");
				n5.setAttributes(groupName, newAttributes);

				Assert.assertEquals(4, n5.listAttributes(groupName).size());
				/* class interface */
				Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", String.class));
				Assert.assertEquals("value2", n5.getAttribute(groupName, "key2", String.class));
				Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", String.class));
				/* type interface */
				Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", new TypeToken<String>() {

				}.getType()));
				Assert.assertEquals("value2", n5.getAttribute(groupName, "key2", new TypeToken<String>() {

				}.getType()));
				Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", new TypeToken<String>() {

				}.getType()));

				n5.setAttribute(groupName, "key1", 1);
				n5.setAttribute(groupName, "key2", 2);

				Assert.assertEquals(4, n5.listAttributes(groupName).size());
				/* class interface */
				Assert.assertEquals(new Integer(1), n5.getAttribute(groupName, "key1", Integer.class));
				Assert.assertEquals(new Integer(2), n5.getAttribute(groupName, "key2", Integer.class));
				Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", String.class));
				/* type interface */
				Assert
						.assertEquals(
								new Integer(1),
								n5.getAttribute(groupName, "key1", new TypeToken<Integer>() {

								}.getType()));
				Assert
						.assertEquals(
								new Integer(2),
								n5.getAttribute(groupName, "key2", new TypeToken<Integer>() {

								}.getType()));
				Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", new TypeToken<String>() {

				}.getType()));

				n5.removeAttribute(groupName, "key1");
				n5.removeAttribute(groupName, "key2");
				n5.removeAttribute(groupName, "key3");
				Assert.assertEquals(1, n5.listAttributes(groupName).size());
			}
		}

		@Test
		@Override
		@Ignore
		public void testNullAttributes() throws IOException {

			// serializeNulls must be on for Zarr to be able to write datasets with raw compression

			/* serializeNulls*/
			try (N5Writer writer = createN5Writer(tempN5Location(), new GsonBuilder().serializeNulls())) {

				writer.createGroup(groupName);
				writer.setAttribute(groupName, "nullValue", null);
				assertNull(writer.getAttribute(groupName, "nullValue", Object.class));
				assertEquals(JsonNull.INSTANCE, writer.getAttribute(groupName, "nullValue", JsonElement.class));
				final HashMap<String, Object> nulls = new HashMap<>();
				nulls.put("anotherNullValue", null);
				nulls.put("structured/nullValue", null);
				nulls.put("implicitNulls[3]", null);
				writer.setAttributes(groupName, nulls);

				assertNull(writer.getAttribute(groupName, "anotherNullValue", Object.class));
				assertEquals(JsonNull.INSTANCE, writer.getAttribute(groupName, "anotherNullValue", JsonElement.class));

				assertNull(writer.getAttribute(groupName, "structured/nullValue", Object.class));
				assertEquals(JsonNull.INSTANCE, writer.getAttribute(groupName, "structured/nullValue", JsonElement.class));

				assertNull(writer.getAttribute(groupName, "implicitNulls[3]", Object.class));
				assertEquals(JsonNull.INSTANCE, writer.getAttribute(groupName, "implicitNulls[3]", JsonElement.class));

				assertNull(writer.getAttribute(groupName, "implicitNulls[1]", Object.class));
				assertEquals(JsonNull.INSTANCE, writer.getAttribute(groupName, "implicitNulls[1]", JsonElement.class));

				/* Negative test; a value that truly doesn't exist will still return `null` but will also return `null` when querying as a `JsonElement` */
				assertNull(writer.getAttribute(groupName, "implicitNulls[10]", Object.class));
				assertNull(writer.getAttribute(groupName, "implicitNulls[10]", JsonElement.class));

				assertNull(writer.getAttribute(groupName, "keyDoesn'tExist", Object.class));
				assertNull(writer.getAttribute(groupName, "keyDoesn'tExist", JsonElement.class));

				/* check existing value gets overwritten */
				writer.setAttribute(groupName, "existingValue", 1);
				assertEquals((Integer)1, writer.getAttribute(groupName, "existingValue", Integer.class));
				writer.setAttribute(groupName, "existingValue", null);
				assertThrows(N5Exception.N5ClassCastException.class, () -> writer.getAttribute(groupName, "existingValue", Integer.class));
				assertEquals(JsonNull.INSTANCE, writer.getAttribute(groupName, "existingValue", JsonElement.class));

				writer.remove();
			}
		}

		@Test
		@Override
		@Ignore
		public void testRootLeaves() {

			// This tests serializing primitives, and arrays at the root of an n5's attributes,
			// since .zattrs must be a json object, this would test invalide behavior for zarr,
			// therefore this test is ignored.
		}

		@Test
		@Override
		public void testReaderCreation() {

			final String canonicalPath = tempN5Location();
			try (N5Writer writer = createN5Writer(canonicalPath)) {

				final N5Reader n5r = createN5Reader(canonicalPath);
				assertNotNull(n5r);

				// existing directory without attributes is okay;
				// Remove and create to remove attributes store
				writer.remove("/");
				writer.createGroup("/");
				final N5Reader na = createN5Reader(canonicalPath);
				assertNotNull(na);

				// existing location with attributes, but no version
				writer.remove("/");
				writer.createGroup("/");
				writer.setAttribute("/", "mystring", "ms");
				final N5Reader wa = createN5Reader(canonicalPath);
				assertNotNull(wa);

				// non-existent directory should fail
				writer.remove("/");
				assertThrows(
						"Non-existant location throws error",
						N5Exception.N5IOException.class,
						() -> {
							final N5Reader test = createN5Reader(canonicalPath);
							test.list("/");
						});
			}
		}

		@Test
		@Override
		public void testWriteReadStringBlock() {

			DataType dataType = DataType.STRING;
			int[] blockSize = new int[]{3, 2, 1};
			String[] stringBlock = new String[]{"", "a", "bc", "de", "fgh", ":-þ"};
			Compression[] compressions = this.getCompressions();

			for (Compression compression : compressions) {
				System.out.println("Testing " + compression.getType() + " " + dataType);

				try (final N5Writer n5 = createN5Writer()) {
					n5.createDataset("/test/group/dataset", dimensions, blockSize, dataType, compression);
					DatasetAttributes attributes = n5.getDatasetAttributes("/test/group/dataset");
					StringDataBlock dataBlock = new ZarrStringDataBlock(blockSize, new long[]{0L, 0L, 0L}, stringBlock);
					n5.writeBlock("/test/group/dataset", attributes, dataBlock);
					DataBlock<?> loadedDataBlock = n5.readBlock("/test/group/dataset", attributes, 0L, 0L, 0L);
					assertArrayEquals(stringBlock, (String[])loadedDataBlock.getData());
					assertTrue(n5.remove("/test/group/dataset"));
				}
			}
		}
	}
}

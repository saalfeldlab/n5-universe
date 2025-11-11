package org.janelia.saalfeldlab.n5.universe.storage.zarr;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.StorageSchemeWrappedN5Test;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2.Zarr2AmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2.Zarr2FileSystemTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2.Zarr2GoogleCloudFactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2.Zarr2HttpFactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3.Zarr3AmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3.Zarr3FileSystemTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3.Zarr3GoogleCloudFactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3.Zarr3HttpFactoryTest;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrTest;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3Test;
import org.junit.AssumptionViolatedException;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@Suite.SuiteClasses({
		Zarr2FileSystemTest.class,
		Zarr2AmazonS3FactoryTest.ZarrAmazonS3MockTest.class,
		Zarr2GoogleCloudFactoryTest.ZarrGoogleCloudMockTest.class,
		Zarr2HttpFactoryTest.class,
		Zarr3FileSystemTest.class,
		Zarr3AmazonS3FactoryTest.ZarrAmazonS3MockTest.class,
		Zarr3GoogleCloudFactoryTest.ZarrGoogleCloudMockTest.class,
		Zarr3HttpFactoryTest.class
})
@RunWith(Suite.class)
public class ZarrStorageTests {

	public static abstract class Zarr3FactoryTest extends ZarrV3Test implements StorageSchemeWrappedN5Test {

		protected N5Factory factory;

		public Zarr3FactoryTest() {

			this.factory = getFactory();
		}

		@Override abstract protected String tempN5Location();

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory();
			}
			return factory;
		}

		@Override public StorageFormat getStorageFormat() {

			return StorageFormat.ZARR;
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
		@Override
		public void serializationTest() {

			throw new AssumptionViolatedException("skip zarr python compatibility tests in n5-universe");

		}
	}

	public static abstract class Zarr2FactoryTest extends N5ZarrTest implements StorageSchemeWrappedN5Test {

		protected N5Factory factory;

		public Zarr2FactoryTest() {

			this.factory = getFactory();
		}

		@Override abstract protected String tempN5Location();

		@Override public N5Factory getFactory() {

			if (factory == null) {
				factory = new N5Factory();
			}
			return factory;
		}

		@Override public StorageFormat getStorageFormat() {

			return StorageFormat.ZARR2;
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

			throw new AssumptionViolatedException("skip zarr python compatibility tests in n5-universe");
		}

		@Ignore
		@Override public void testReadZarrNestedPython() {

			throw new AssumptionViolatedException("skip zarr python compatibility tests in n5-universe");
		}

	}

}

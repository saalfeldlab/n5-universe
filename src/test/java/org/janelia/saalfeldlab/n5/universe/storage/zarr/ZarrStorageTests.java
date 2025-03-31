package org.janelia.saalfeldlab.n5.universe.storage.zarr;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.StorageSchemeWrappedN5Test;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrTest;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@Suite.SuiteClasses({
		ZarrFileSystemTest.class,
		ZarrAmazonS3FactoryTest.ZarrAmazonS3MockTest.class,
		ZarrGoogleCloudFactoryTest.ZarrGoogleCloudMockTest.class,
		ZarrHttpFactoryTest.class
})
@RunWith(Suite.class)
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
		@Override public void testReadZarrPython() {

		}

		@Ignore
		@Override public void testReadZarrNestedPython() {

		}
	}

}

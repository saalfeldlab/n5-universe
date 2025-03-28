package org.janelia.saalfeldlab.n5.universe.storage.n5;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.AbstractN5Test;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.StorageSchemeWrappedN5Test;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.ArrayList;

@RunWith(Suite.class)
@Suite.SuiteClasses({N5FileSystemTest.class, N5AmazonS3FactoryTest.N5AmazonS3MockTest.class, N5GoogleCloudFactoryTest.N5GoogleCloudMockTest.class, N5HttpFactoryTest.class})
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

		@Override public StorageFormat getStorageFormat() {

			return StorageFormat.N5;
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

}

package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.universe.storage.n5.N5AmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.n5.N5GoogleCloudFactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2.Zarr2AmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr2.Zarr2GoogleCloudFactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3.Zarr3AmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3.Zarr3GoogleCloudFactoryTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		N5AmazonS3FactoryTest.N5AmazonS3BackendTest.class,
		N5GoogleCloudFactoryTest.N5GoogleCloudBackendTest.class,
		Zarr2GoogleCloudFactoryTest.ZarrGoogleCloudBackendTest.class,
		Zarr3GoogleCloudFactoryTest.ZarrGoogleCloudBackendTest.class,
		Zarr2AmazonS3FactoryTest.ZarrAmazonS3BackendTest.class,
		Zarr3AmazonS3FactoryTest.ZarrAmazonS3BackendTest.class
})
public class BackendStorageFormatTests {
}

package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.universe.storage.n5.N5AmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.n5.N5GoogleCloudFactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrAmazonS3FactoryTest;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrGoogleCloudFactoryTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({N5AmazonS3FactoryTest.N5AmazonS3BackendTest.class, N5GoogleCloudFactoryTest.N5GoogleCloudBackendTest.class, ZarrAmazonS3FactoryTest.ZarrAmazonS3BackendTest.class, ZarrGoogleCloudFactoryTest.ZarrGoogleCloudBackendTest.class})
public class BackendStorageFormatTests {
}

package org.janelia.saalfeldlab.n5.universe;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({N5StorageTests.N5AmazonS3BackendTest.class, N5StorageTests.N5GoogleCloudBackendTest.class, ZarrStorageTests.ZarrAmazonS3BackendTest.class, ZarrStorageTests.ZarrGoogleCloudBackendTest.class})
public class BackendStorageFormatTests {
}

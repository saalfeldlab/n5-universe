package org.janelia.saalfeldlab.n5.universe.storage.n5;

import java.io.IOException;
import java.nio.file.Files;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueRoot;

public class N5FileSystemTest extends N5StorageTests.N5FactoryTest {

	@Override public Class<?> getBackendTargetClass() {

		return FileSystemKeyValueRoot.class;
	}

	@Override protected String tempN5Location() {

		try {
			return Files.createTempDirectory("n5-test").toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

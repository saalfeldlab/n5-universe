package org.janelia.saalfeldlab.n5.universe.storage.n5;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;

import java.io.IOException;
import java.nio.file.Files;

public class N5FileSystemTest extends N5StorageTests.N5FactoryTest {

	@Override public Class<?> getBackendTargetClass() {

		return FileSystemKeyValueAccess.class;
	}

	@Override protected String tempN5Location() {

		try {
			return Files.createTempDirectory("n5-test").toUri().getPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

package org.janelia.saalfeldlab.n5.universe.storage.zarr;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;

import java.io.IOException;
import java.nio.file.Files;

public class ZarrFileSystemTest extends ZarrStorageTests.ZarrFactoryTest {

	@Override public Class<?> getBackendTargetClass() {

		return FileSystemKeyValueAccess.class;
	}

	@Override protected String tempN5Location() {

		try {
			return Files.createTempDirectory("zarr-test").toUri().getPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

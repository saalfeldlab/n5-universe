package org.janelia.saalfeldlab.n5.universe.storage.zarr.zarr3;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.universe.storage.zarr.ZarrStorageTests;

import java.io.IOException;
import java.nio.file.Files;

public class Zarr3FileSystemTest extends ZarrStorageTests.Zarr3FactoryTest {

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

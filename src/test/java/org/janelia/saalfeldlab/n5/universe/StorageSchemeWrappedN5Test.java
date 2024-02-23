package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;

import static org.junit.Assert.assertTrue;

public interface StorageSchemeWrappedN5Test {

	N5Factory getFactory();

	N5Factory.StorageFormat getStorageFormat();

	Class<?> getBackendTargetClass();

	default N5Writer getWriter(String uri) {

		final String uriWithStorageScheme = prependStorageScheme(uri);
		final GsonKeyValueN5Writer writer = (GsonKeyValueN5Writer)getFactory().getWriter(uriWithStorageScheme);
		switch (getStorageFormat()){
		case ZARR:
			assertTrue(writer instanceof ZarrKeyValueWriter);
			break;
		case N5:
			assertTrue(writer instanceof N5KeyValueWriter);
			break;
		case HDF5:
			assertTrue(writer instanceof N5HDF5Writer);
			break;
		}
		assertTrue(getBackendTargetClass().isAssignableFrom(writer.getKeyValueAccess().getClass()));
		return writer;
	}

	default N5Reader getReader(String uri) {

		final String uriWithStorageScheme = prependStorageScheme(uri);
		final GsonKeyValueN5Reader reader = (GsonKeyValueN5Reader)getFactory().getReader(uriWithStorageScheme);
		switch (getStorageFormat()){
		case ZARR:
			assertTrue(reader instanceof ZarrKeyValueReader);
			break;
		case N5:
			assertTrue(reader instanceof N5KeyValueReader);
			break;
		case HDF5:
			assertTrue(reader instanceof N5HDF5Reader);
			break;
		}
		assertTrue(getBackendTargetClass().isAssignableFrom(reader.getKeyValueAccess().getClass()));
		return reader;
	}

	default String prependStorageScheme(String uri) {

		final String schemePattern = getStorageFormat().schemePattern.pattern();
		final String doesntHaveStorageScheme = new StringBuilder("^(?:(?!(?i)")
				.append(schemePattern)
				.append(":))(?<remaining>.*)")
				.toString();

		final String prependStorageScheme = new StringBuilder(getStorageFormat().toString().toLowerCase())
				.append(":${remaining}")
				.toString();

		return uri.replaceFirst(doesntHaveStorageScheme, prependStorageScheme);
	}
}
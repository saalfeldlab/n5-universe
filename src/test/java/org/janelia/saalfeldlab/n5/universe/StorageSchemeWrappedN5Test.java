package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.janelia.saalfeldlab.n5.universe.factory.InferredStorageFormat;
import org.janelia.saalfeldlab.n5.universe.factory.N5Factory;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;

import static org.junit.Assert.assertTrue;

public interface StorageSchemeWrappedN5Test {

	N5Factory getFactory();

	StorageFormat getStorageFormat();

	Class<?> getBackendTargetClass();

	default N5Writer getWriter(String uri) {

		final String uriWithStorageScheme = prependStorageScheme(uri);
		final GsonKeyValueN5Writer writer = (GsonKeyValueN5Writer)getFactory().openWriter(uriWithStorageScheme);
		switch (getStorageFormat()){
		case ZARR3:
			assertTrue(writer instanceof ZarrV3KeyValueWriter);
			break;
		case ZARR2:
			assertTrue(writer instanceof ZarrKeyValueWriter);
			break;
		case N5:
			assertTrue(writer instanceof N5KeyValueWriter);
			break;
		}

		assertTrue(getBackendTargetClass().isAssignableFrom(writer.getKeyValueRoot().getClass()));
		return writer;
	}

	default N5Reader getReader(String uri) {

		final String uriWithStorageScheme = prependStorageScheme(uri);
		final GsonKeyValueN5Reader reader = (GsonKeyValueN5Reader)getFactory().openReader(uriWithStorageScheme);
		switch (getStorageFormat()){
		case ZARR3:
			assertTrue(reader instanceof ZarrV3KeyValueReader);
			break;
		case ZARR2:
			assertTrue(reader instanceof ZarrKeyValueReader);
			break;
		case N5:
			assertTrue(reader instanceof N5KeyValueReader);
			break;
		}
		assertTrue(getBackendTargetClass().isAssignableFrom(reader.getKeyValueRoot().getClass()));
		return reader;
	}

	default String prependStorageScheme(String uri) {

		final StorageFormat format = getStorageFormat();
		final InferredStorageFormat inferred = InferredStorageFormat.of(uri);
		if (inferred.format() == format && inferred.isExplicit()) {
			return uri;
		} else {
			return format.scheme() + ":" + uri;
		}
	}
}
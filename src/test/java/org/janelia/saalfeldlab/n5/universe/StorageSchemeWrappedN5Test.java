package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.AbstractN5Test;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.junit.Before;

public interface StorageSchemeWrappedN5Test {

	abstract N5Factory getFactory();

	N5Factory.StorageFormat getStorageFormat();

	default N5Writer getWriter(String uri) {

		final String uriWithStorageScheme = prependStorageScheme(uri);
		return getFactory().getWriter(uriWithStorageScheme);
	}

	default N5Reader getReader(String uri) {

		final String uriWithStorageScheme = prependStorageScheme(uri);
		return getFactory().getReader(uriWithStorageScheme);
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
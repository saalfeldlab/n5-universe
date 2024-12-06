package org.janelia.saalfeldlab.n5.universe;

import com.amazonaws.services.s3.AmazonS3;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudUtils;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3Utils;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Enum to discover and provide {@link KeyValueAccess} for {@link N5Reader}s and {@link N5Writer}s.
 * IMPORTANT: If ever new {@link KeyValueAccess} backends are adding, they MUST be re-ordered
 * such that the earliest predicates are the most restrictive, and the later predicates
 * are the least restrictive. This ensures that when iterating through the values of
 * {@link KeyValueAccessBackend} you can test them in order, and stop at the first
 * {@link KeyValueAccess} that is generated.
 */
public enum KeyValueAccessBackend implements Predicate<URI>, BiFunction<URI, N5Factory, KeyValueAccess> {
	GOOGLE_CLOUD(uri -> {
		final String scheme = uri.getScheme();
		final boolean hasScheme = scheme != null;
		return hasScheme && GoogleCloudUtils.GS_SCHEME.asPredicate().test(scheme)
				|| hasScheme && N5Factory.HTTPS_SCHEME.asPredicate().test(scheme)
				&& uri.getHost() != null && GoogleCloudUtils.GS_HOST.asPredicate().test(uri.getHost());
	}, KeyValueAccessBackend::newGoogleCloudKeyValueAccess),
	AWS(uri -> {
		final String scheme = uri.getScheme();
		final boolean hasScheme = scheme != null;
		return hasScheme && AmazonS3Utils.S3_SCHEME.asPredicate().test(scheme)
				|| uri.getHost() != null && hasScheme && N5Factory.HTTPS_SCHEME.asPredicate().test(scheme);
	}, KeyValueAccessBackend::newAmazonS3KeyValueAccess),
	FILE(uri -> {
		final String scheme = uri.getScheme();
		final boolean hasScheme = scheme != null;
		return !hasScheme || N5Factory.FILE_SCHEME.asPredicate().test(scheme);
	}, KeyValueAccessBackend::newFileSystemKeyValueAccess);

	private final Predicate<URI> backendTest;
	private final BiFunction<URI, N5Factory, KeyValueAccess> backendGenerator;

	KeyValueAccessBackend(Predicate<URI> test, BiFunction<URI, N5Factory, KeyValueAccess> generator) {

		backendTest = test;
		backendGenerator = generator;
	}

	@Override public KeyValueAccess apply(final URI uri, final N5Factory factory) {

		if (test(uri))
			return backendGenerator.apply(uri, factory);
		return null;
	}

	@Override public boolean test(URI uri) {

		return backendTest.test(uri);
	}

	/**
	 * Test the provided {@link URI} to and return the appropriate {@link KeyValueAccess}.
	 * If no appropriate {@link KeyValueAccess} is found, may be null
	 *
	 * @param uri to create a {@link KeyValueAccess} from.
	 * @return the {@link KeyValueAccess} and container path, or null if none are valid
	 */
	@Nullable
	public static KeyValueAccess getKeyValueAccess(final URI uri) {

		return getKeyValueAccess(uri, N5Factory.FACTORY);
	}

	@Nullable
	static KeyValueAccess getKeyValueAccess(final URI uri, final N5Factory factory) {

		/*NOTE: The order of these tests is very important, as the predicates for each
		 * backend take into account reasonable defaults when possible.
		 * Here we test from most to least restrictive.
		 * See the Javadoc for more details.  */
		for (final KeyValueAccessBackend backend : KeyValueAccessBackend.values()) {
			final KeyValueAccess kva = backend.apply(uri, factory);
			if (kva != null)
				return kva;
		}
		return null;
	}

	private static GoogleCloudStorageKeyValueAccess newGoogleCloudKeyValueAccess(final URI uri, final N5Factory factory) {

		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(uri);
		return new GoogleCloudStorageKeyValueAccess(factory.createGoogleCloudStorage(), googleCloudUri, true);
	}

	private static AmazonS3KeyValueAccess newAmazonS3KeyValueAccess(final URI uri, final N5Factory factory) {

		final String uriString = uri.toString();
		final AmazonS3 s3 = factory.createS3(uriString);

		return new AmazonS3KeyValueAccess(s3, uri, true);
	}

	private static FileSystemKeyValueAccess newFileSystemKeyValueAccess(final URI uri, final N5Factory factory) {

		return new FileSystemKeyValueAccess(FileSystems.getDefault());
	}
}

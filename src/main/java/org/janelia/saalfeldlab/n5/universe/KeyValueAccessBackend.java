package org.janelia.saalfeldlab.n5.universe;

import org.apache.commons.lang3.function.TriFunction;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudUtils;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.HttpKeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import software.amazon.awssdk.services.s3.S3Client;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.function.Predicate;

/**
 * Enum to discover and provide {@link KeyValueAccess} for {@link N5Reader}s and {@link N5Writer}s.
 * IMPORTANT: If ever new {@link KeyValueAccess} backends are adding, they MUST be re-ordered
 * such that the earliest predicates are the most restrictive, and the later predicates
 * are the least restrictive. This ensures that when iterating through the values of
 * {@link KeyValueAccessBackend} you can test them in order, and stop at the first
 * {@link KeyValueAccess} that is generated.
 */
public enum KeyValueAccessBackend implements Predicate<URI>, TriFunction<URI, N5Factory, Boolean, KeyValueAccess> {
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
	HTTP(uri -> {
		final String scheme = uri.getScheme();
		final boolean hasScheme = scheme != null;
		return hasScheme && N5Factory.HTTPS_SCHEME.asPredicate().test(scheme);
	}, KeyValueAccessBackend::newHttpKeyValueAccess),
	FILE(uri -> {
		final String scheme = uri.getScheme();
		final boolean hasScheme = scheme != null;
		return !hasScheme || N5Factory.FILE_SCHEME.asPredicate().test(scheme);
	}, KeyValueAccessBackend::newFileSystemKeyValueAccess);

	private final Predicate<URI> backendTest;
	private final TriFunction<URI, N5Factory, Boolean, KeyValueAccess> backendGenerator;

	KeyValueAccessBackend(Predicate<URI> test, TriFunction<URI, N5Factory, Boolean, KeyValueAccess> generator) {

		backendTest = test;
		backendGenerator = generator;
	}

	@Override public KeyValueAccess apply(final URI uri, final N5Factory factory, Boolean readOnly) {

		return backendGenerator.apply(uri, factory, readOnly);
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
	public static KeyValueAccess getKeyValueAccess(final URI uri, boolean readOnly) {

		return getKeyValueAccess(uri, N5Factory.FACTORY, readOnly);
	}

	@Nullable
	static KeyValueAccess getKeyValueAccess(final URI uri, final N5Factory factory, final boolean readOnly) {

		/*NOTE: The order of these tests is very important, as the predicates for each
		 * backend take into account reasonable defaults when possible.
		 * Here we test from most to least restrictive.
		 * See the Javadoc for more details.  */
		N5Exception.N5IOException testPassedButThrown = null;
		for (final KeyValueAccessBackend backend : KeyValueAccessBackend.values()) {
			if (backend.test(uri)) {
				try {
					return backend.apply(uri, factory, readOnly);
				} catch (final Exception e) {
					String reason = String.format("Inferred Backend was %s but could not create KeyValueAccess", backend);
					testPassedButThrown = new N5Exception.N5IOException(reason, e);
				}
			}
		}
		if (testPassedButThrown != null) {
			throw testPassedButThrown;
		}

		return null;
	}

	private static GoogleCloudStorageKeyValueAccess newGoogleCloudKeyValueAccess(final URI uri, final N5Factory factory, final boolean readOnly) {

		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(uri);
		return new GoogleCloudStorageKeyValueAccess(factory.createGoogleCloudStorage(), googleCloudUri, !readOnly);
	}

	private static AmazonS3KeyValueAccess newAmazonS3KeyValueAccess(final URI uri, final N5Factory factory, final boolean readOnly) {

		final String uriString = uri.toString();

		// throw exception if s3 endpoint is not reachable
		if (readOnly && !uri.getScheme().equals("s3"))
			isUriS3Endpoint(uriString);

		final S3Client s3 = factory.createS3(uriString);
		return new AmazonS3KeyValueAccess(s3, uri, !readOnly);
	}

	private static boolean isUriS3Endpoint( String uriString ) {

		URL url;
		try {

			// An http requests to an S3 endpoint performs a list
			// limit the number of returned entries
			url = new URL(uriString + "?max-keys=1");

			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setReadTimeout(5000);
			connection.setConnectTimeout(5000);
			connection.setRequestMethod(HttpKeyValueAccess.GET);

			final int code = connection.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				if (!isS3ListResponse(connection.getInputStream()))
					throw new N5Exception("Not an S3 endpoint");
				else
					return true;
			} else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
				if (!isS3ErrorResponse(connection.getErrorStream()))
					throw new N5Exception("Not an S3 endpoint");
				else
					return true;
			}
		} catch (IOException e) {
			throw new N5Exception.N5IOException("Could not reach S3 endpoint", e);
		}
		throw new N5Exception.N5IOException("Could not reach S3 endpoint");
	}

	private static boolean isS3ListResponse(InputStream is) {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Element root = builder.parse(is).getDocumentElement();
			if (root.getLocalName().equals("ListAllMyBucketsResult")) {
				return true;
			} else if (root.getLocalName().equals("ListBucketResult")) {
				return true;
			}
		} catch (Exception e) {}
		return false;
	}
	
	private static boolean isS3ErrorResponse(InputStream is) {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			final Document doc = factory.newDocumentBuilder().parse(is);
			final Element root = doc.getDocumentElement();
			if (!root.getTagName().matches("Error"))
				return false;

			final NodeList codeNodes = root.getElementsByTagName("Code");
			if (codeNodes.getLength() == 0 || !codeNodes.item(0).getTextContent().equals("AccessDenied"))
				return false;

			return true;

		} catch (Exception e) {}
		return false;
	}

	private static FileSystemKeyValueAccess newFileSystemKeyValueAccess(final URI uri, final N5Factory factory, final boolean readOnly) {

		return new FileSystemKeyValueAccess();
	}

	private static HttpKeyValueAccess newHttpKeyValueAccess(final URI uri, final N5Factory factory, final boolean readOnly) {

		if (!readOnly)
			throw new UnsupportedOperationException("HttpKeyValueAccess only supports reader actions");
		return new HttpKeyValueAccess();
	}
}

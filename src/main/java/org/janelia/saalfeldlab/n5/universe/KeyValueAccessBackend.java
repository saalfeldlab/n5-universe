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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
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
		S3Client s3 = factory.createS3(uriString);
		assertS3Endpoint(uri, readOnly, s3);
		return new AmazonS3KeyValueAccess(s3, uri, !readOnly);
	}

	/**
	 * Throws an N5Exception if the URI does not correspond to an S3 compatible
	 * endpoint.
	 *
	 * @param uri
	 *            a uri
	 * @param readOnly
	 *            is the backend read only
	 * @param s3
	 *            an s3 client
	 */
	private static void assertS3Endpoint(URI uri, boolean readOnly, S3Client s3) {

		/*
		 * 1) The URI is an s3 endpoint if the uri scheme is "s3"
		 * 2) If the backend requries write (not reqdOnly), assume the 
		 * 	  client is an s3 backend, since an http fallback is useless (http is read-only)
		 *
		 * Otherwise to some expensive validation
		 */
		if (uri.getScheme().equals("s3") || !readOnly)
			return;

		final boolean isS3 = checkS3EndpointHttp(uri) || checkS3EndpointClient(s3);
		if (!isS3) {
			// throw exception if s3 endpoint is not reachable
			throw new N5Exception.N5IOException("S3 endpoint is not reachable at " + uri);
		}
	}

	private static boolean checkS3EndpointClient(S3Client s3) {
		try {
			s3.getBucketAcl(builder -> builder.bucket("" + System.nanoTime()));
			return true;
		} catch (Throwable e) {
			return false;
		}

	}

	/**
	 * Tries to determine if the specified uri is an s3-compatible backend.
	 * <p>
	 * This implementation sents an http request and parses the result
	 *
	 * @param uriString
	 *            A string representing an absolute URI
	 * @return true if the replies resemble those of an s3 backend.
	 *
	 */
	private static boolean checkS3EndpointHttp(URI uriIn) {

		/**
		 * Response parsing sometimes prints:
		 * 		"[Fatal Error] strict.dtd:81:5: The declaration for the entity "ContentType" must end with '>'."
		 * and we can't seem to control it. So temporarily redirect the System.out and System.err
		 */
		final PrintStream origOut = System.out;
		final PrintStream origErr = System.err;
		System.setOut(dummyPrintStream());
		System.setErr(dummyPrintStream());

		URL url;
		try {

			// An http request to an S3 endpoint performs a list; limit the number of returned entries.
			// Replace any existing query string with ?max-keys=1 so we don't accumulate parameters.
			url = new URI(
					uriIn.getScheme(),
					uriIn.getUserInfo(),
					uriIn.getHost(),
					uriIn.getPort(),
					uriIn.getPath(),
					"max-keys=1",
					null)
				.toURL();

			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setReadTimeout(5000);
			connection.setConnectTimeout(5000);
			connection.setRequestMethod(HttpKeyValueAccess.GET);

			// The header may indicate the server is s3-compatible
			if( isS3Header(connection))
				return true;

			final int code = connection.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				if (isS3ListResponse(connection.getInputStream()))
					return true;
			} else if (code == HttpURLConnection.HTTP_FORBIDDEN|| code == HttpURLConnection.HTTP_NOT_FOUND) {
				if (isS3ErrorResponse(connection.getErrorStream()))
					return true;
			}
		} catch (Exception ignored) { }
		finally {
			System.setOut(origOut);
			System.setErr(origErr);
		}

		return false;
	}

	private static PrintStream DUMMY = null;
	private static PrintStream dummyPrintStream() {
		if( DUMMY == null )
			DUMMY = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					// No-op
				}
			});

		return DUMMY;
	}

	/**
	 * Checks whether the http response header appears to be from an
	 * s3-compatible backend.
	 * <p>
	 * This implementation returns true if any header fields start with "x-amz-"
	 * (case insensitive).
	 *
	 * @param headerFields
	 *            The collection of header fields
	 *
	 * @return true if the response appears to be from an s3-backend
	 */
	private static boolean isS3Header(HttpURLConnection connection) {

		for (String header : connection.getHeaderFields().keySet()) {
			if (header != null && header.toLowerCase().matches("^x-amz-.*"))
				return true;
		}
		return false;
	}

	/**
	 * Checks whether the http response appears to be from an s3-compatible
	 * backend.
	 * <p>
	 * This implementation looks for xml of the form
	 * "<ListAllMyBucketsResult>" returned by AWS or
	 * "<ListBucketResult>" returned by MinIO
	 *
	 * @param is
	 *            the input stream from an HttpURLConnection
	 * @return true if the response appears to be from an s3-backend
	 */
	private static boolean isS3ListResponse(InputStream is) {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Element root = builder.parse(is).getDocumentElement();
			return EXPECTED_S3_LIST_RESPONSE.contains(root.getLocalName());
		} catch (Exception ignored) {}
		return false;
	}

	private static final HashSet<String> EXPECTED_S3_LIST_RESPONSE = new HashSet<>();
	static {
		// the list buckets response from an Amazon S3 container
		EXPECTED_S3_LIST_RESPONSE.add("ListAllMyBucketsResult");

		// the list response from a MinIO server
		EXPECTED_S3_LIST_RESPONSE.add("ListBucketResult");
	}
	
	/**
	 * Checks whether the http error response appears to be from an s3-compatible
	 * backend.
	 * <p>
	 * This implementation looks for xml of the form "<Error><Code>AccessDenied</Code>..."
	 * which is what the form returned by AWS and SeaweedFS.
	 *
	 * @param is
	 *            the input stream from an HttpURLConnection
	 * @return true if the error response appears to be from an s3-backend
	 */
	private static boolean isS3ErrorResponse(InputStream is) {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			final Document doc = factory.newDocumentBuilder().parse(is);
			final String errorCode = xPath.evaluate("/Error/Code/text()", doc);

			return EXPECTED_ERROR_CODES.contains(errorCode);
        } catch (Exception ignored) {}
		return false;
	}


	private static final HashSet<String> EXPECTED_ERROR_CODES = new HashSet<>();
	static {
		EXPECTED_ERROR_CODES.add("NoSuchKey");
		EXPECTED_ERROR_CODES.add("AccessDenied");
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

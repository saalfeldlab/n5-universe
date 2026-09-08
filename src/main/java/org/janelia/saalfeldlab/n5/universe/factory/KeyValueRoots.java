package org.janelia.saalfeldlab.n5.universe.factory;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueRoot;
import org.janelia.saalfeldlab.n5.HttpKeyValueRoot;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Provider;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Options;
import org.janelia.saalfeldlab.n5.universe.factory.options.BackendOptions;
import org.janelia.saalfeldlab.n5.universe.factory.options.HttpOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.util.regex.Pattern;

/**
 * To open a reader or writer, {@link N5Factory} needs to choose a
 * <em>backend</em> and a container <em>format</em>.
 * <p>
 * {@link KeyValueRoots} handles the <em>backend</em> axis: Make data at
 * a {@link URI} available through a {@link KeyValueRoot}. This is a
 * package-private helper class, split out for clarity, and only used by
 * {@link N5Factory}.
 * <p>
 * {@link #create} is the entry point, and it dispatches on the URI scheme in
 * straight-line code. That makes the ambiguous cases decidable one at a time.
 * Most importantly, a read-only {@code http(s)} URI is resolved with a cheap
 * HTTP request instead first, instead of building an S3 client and probing it.
 * <p>
 * NB: The other axis (<em>format</em>) is handled by {@link Containers}:
 * {@code KeyValueRoots} turns a URI into a {@code KeyValueRoot}, and
 * {@code Containers} turns that root into a container.
 */
public final class KeyValueRoots {

	// Declared here rather than taken from AmazonS3Utils / GoogleCloudUtils:
	// knowing which URIs *could* belong to a backend is core's job, and must
	// not require that backend's dependency.
	private final static Pattern HTTP_SCHEME = Pattern.compile("http(s)?", Pattern.CASE_INSENSITIVE);
	private final static Pattern FILE_SCHEME = Pattern.compile("file", Pattern.CASE_INSENSITIVE);
	private final static Pattern S3_SCHEME = Pattern.compile("s3", Pattern.CASE_INSENSITIVE);
	private final static Pattern GS_SCHEME = Pattern.compile("gs", Pattern.CASE_INSENSITIVE);
	private final static Pattern GS_HOST = Pattern.compile("(cloud\\.google|storage\\.googleapis)\\.com", Pattern.CASE_INSENSITIVE);

	private KeyValueRoots() {}

	/**
	 * Test the provided {@link URI} and return the appropriate
	 * {@link KeyValueRoot}. If no appropriate {@link KeyValueRoot} is found,
	 * may be null.
	 *
	 * @param uri to create a {@link KeyValueRoot} from
	 * @param readOnly whether the root will only be read from
	 * @param options the configuration to reach the backend with
	 * @return the {@link KeyValueRoot}, or null if none are valid
	 */
	@Nullable
	public static KeyValueRoot create(
			final URI uri,
			final boolean readOnly,
			final BackendOptions options) {

		final String scheme = uri.getScheme();

		// No scheme at all is a bare path, i.e. the local file system.
		if (scheme == null || FILE_SCHEME.asPredicate().test(scheme))
			return createFileSystem(uri);

		if (GS_SCHEME.asPredicate().test(scheme))
			return createGoogleCloud(uri, readOnly, options);

		if (S3_SCHEME.asPredicate().test(scheme))
			return createAmazonS3(uri, readOnly, options);

		if (HTTP_SCHEME.asPredicate().test(scheme))
			return createFromHttpUri(uri, readOnly, options);

		return null;
	}

	/**
	 * Create a {@link KeyValueRoot} on the local file system.
	 *
	 * @param uri a {@code file:} URI, or a bare path
	 * @return the root
	 */
	public static KeyValueRoot createFileSystem(final URI uri) {

		return new FileSystemKeyValueRoot(uri.getPath());
	}

	/**
	 * Create a read-only {@link KeyValueRoot} that fetches over HTTP.
	 * <p>
	 * Note this is <em>plain</em> HTTP: for an {@code http(s)} URI that may be served
	 * by S3 or Google Cloud Storage, use {@link #create(URI, boolean, BackendOptions)}
	 * and let it decide.
	 *
	 * @param uri an {@code http(s)} URI
	 * @param options supplies the timeouts
	 * @return the root
	 */
	public static KeyValueRoot createHttp(final URI uri, final BackendOptions options) {

		return newHttpKeyValueRoot(uri, options.http());
	}

	/**
	 * Create a {@link KeyValueRoot} on Amazon S3, or any S3-compatible
	 * endpoint.
	 *
	 * @param uri an {@code s3://} or {@code http(s)} URI of an S3 endpoint
	 * @param readOnly whether the root will only be read from
	 * @param options the backend configuration
	 * @return the root
	 * @throws N5Exception if there is no S3 backend on the classpath
	 */
	public static KeyValueRoot createAmazonS3(final URI uri, final boolean readOnly, final BackendOptions options) {

		return Providers.amazonS3().endpoint(uri, options.s3()).createKeyValueRoot(readOnly);
	}

	/**
	 * Create a {@link KeyValueRoot} on Google Cloud Storage.
	 *
	 * @param uri a {@code gs://} URI, or an {@code http(s)} URI of a Google
	 * 		Cloud Storage host
	 * @param readOnly whether the root will only be read from
	 * @param options the backend configuration
	 * @return the root
	 * @throws N5Exception if there is no Google Cloud Storage backend on the
	 * 		classpath
	 */
	public static KeyValueRoot createGoogleCloud(final URI uri, final boolean readOnly, final BackendOptions options) {

		return Providers.googleCloud().createKeyValueRoot(uri, readOnly, options.googleCloud());
	}

	/**
	 * Create the {@link KeyValueRoot} for a URI with an {@code http(s)} scheme.
	 * Such a URI may address any of three backends, so this returns a Google
	 * Cloud Storage root, an Amazon S3 root, or an
	 * {@link org.janelia.saalfeldlab.n5.HttpKeyValueRoot} — the scheme
	 * describes how the URI is written, not which backend serves it.
	 */
	private static KeyValueRoot createFromHttpUri(
			final URI uri,
			final boolean readOnly,
			final BackendOptions options) {

		// Google Cloud Storage is also reachable over https, and its hosts are
		// known, so that case is decided without any request.
		final String host = uri.getHost();
		if (host != null && GS_HOST.asPredicate().test(host))
			return createGoogleCloud(uri, readOnly, options);

		// What is left is genuinely ambiguous: an S3-compatible endpoint, or a
		// plain HTTP server.
		final AmazonS3Options s3Options = options.s3();

		// A writable container over http(s) can only be S3 at this
		// point (GCS was already handled above). No probing needed.
		if (!readOnly)
			return createAmazonS3(uri, false, options);

		// Read-only. HttpKeyValueRoot is the cheap answer and the common case,
		// so decide with a single HTTP request rather than by building an S3
		// client first (that resolves the credential chain, and probing the
		// client can hang for seconds). Creating AmazonS3Provider.Endpoint is
		// cheap, it does create a S3Client lazily.
		final AmazonS3Provider.Endpoint s3 = Providers.hasAmazonS3() ? Providers.amazonS3().endpoint(uri, s3Options) : null;
		if (looksLikeS3Endpoint(uri, s3, options)) {
			if (s3 != null) {
				try {
					return s3.createKeyValueRoot(true);
				} catch (final Exception e) {
					// The endpoint speaks S3, but we cannot open the bucket
					// through it -- typically because listing or heading the
					// bucket is denied without credentials. Anonymous reads over
					// plain HTTP may still work, so fall through.
				}
			} else {
				// NB: This will throw an exception saying that n5-aws-s3 is not available
				Providers.amazonS3();
			}
		}

		return newHttpKeyValueRoot(uri, options.http());
	}

	private static HttpKeyValueRoot newHttpKeyValueRoot(final URI uri, final HttpOptions httpOptions) {

		final HttpKeyValueRoot root = new HttpKeyValueRoot(uri);
		root.setConnectionTimeout(httpOptions.connectionTimeoutMillis());
		root.setReadTimeout(httpOptions.readTimeoutMillis());
		return root;
	}

	// ------------------------------------------------------------------------
	//
	// -- telling an S3-compatible endpoint from a plain HTTP server --
	//

	/**
	 * Tries to determine whether the specified {@code uri} appears be
	 * an S3-compatible endpoint rather than a plain HTTP server.
	 * <p>
	 * Sends one HTTP request and parses the result.
	 * <p>
	 * If that is inconclusive and {@link BackendOptions#probeHttpWithS3Client()}
	 * is set and {@code s3 != null} (meaning n5-aws-s3 backend is available), then
	 * probe through a {@code S3Client} (via {@link AmazonS3Provider.Endpoint#isS3}).
	 */
	private static boolean looksLikeS3Endpoint(
			final URI uri,
			final AmazonS3Provider.Endpoint s3,
			final BackendOptions options) {

		return isS3EndpointHttp(uri, options.http())
				|| options.probeHttpWithS3Client() && s3 != null && s3.isS3();
	}

	/**
	 * Tries to determine whether {@code uri} points at an S3-compatible
	 * backend, by sending a single HTTP request and inspecting the reply.
	 *
	 * @param uriIn an absolute URI
	 * @param httpOptions supplies the timeouts for the request
	 * @return true if the reply resembles that of an s3 backend
	 */
	private static boolean isS3EndpointHttp(final URI uriIn, final HttpOptions httpOptions) {

		/*
		 * Response parsing sometimes prints:
		 * 		"[Fatal Error] strict.dtd:81:5: The declaration for the entity "ContentType" must end with '>'."
		 * and we can't seem to control it. So temporarily redirect the System.out and System.err
		 */
		final PrintStream origOut = System.out;
		final PrintStream origErr = System.err;
		System.setOut(dummyPrintStream());
		System.setErr(dummyPrintStream());

		try {
			// An http request to an S3 endpoint performs a list; limit the number of returned entries.
			// Replace any existing query string with ?max-keys=1 so we don't accumulate parameters.
			final URL url = new URI(
					uriIn.getScheme(),
					uriIn.getUserInfo(),
					uriIn.getHost(),
					uriIn.getPort(),
					uriIn.getPath(),
					"max-keys=1",
					null)
				.toURL();

			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setReadTimeout(httpOptions.readTimeoutMillis());
			connection.setConnectTimeout(httpOptions.connectionTimeoutMillis());
			connection.setRequestMethod("GET");

			// The header may indicate the server is s3-compatible
			if (isS3Header(connection))
				return true;

			final int code = connection.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				if (isS3ListResponse(connection.getInputStream()))
					return true;
			} else if (code == HttpURLConnection.HTTP_FORBIDDEN || code == HttpURLConnection.HTTP_NOT_FOUND) {
				if (isS3ErrorResponse(connection.getErrorStream()))
					return true;
			}
		} catch (final Exception ignored) {
		} finally {
			System.setOut(origOut);
			System.setErr(origErr);
		}

		return false;
	}

	private static PrintStream DUMMY = null;

	private static PrintStream dummyPrintStream() {

		if (DUMMY == null)
			DUMMY = new PrintStream(new OutputStream() {

				@Override
				public void write(final int b) throws IOException {
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
	 * @param connection the connection whose header fields to check
	 * @return true if the response appears to be from an s3-backend
	 */
	private static boolean isS3Header(final HttpURLConnection connection) {

		for (final String header : connection.getHeaderFields().keySet()) {
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
	 * "&lt;ListAllMyBucketsResult&gt;" returned by AWS or
	 * "&lt;ListBucketResult&gt;" returned by MinIO
	 *
	 * @param is the input stream from an HttpURLConnection
	 * @return true if the response appears to be from an s3-backend
	 */
	private static boolean isS3ListResponse(final InputStream is) {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Element root = builder.parse(is).getDocumentElement();
			return EXPECTED_S3_LIST_RESPONSE.contains(root.getLocalName());
		} catch (final Exception ignored) {}
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
	 * This implementation looks for xml of the form
	 * "&lt;Error&gt;&lt;Code&gt;AccessDenied&lt;/Code&gt;..." which is what is
	 * returned by AWS and SeaweedFS.
	 *
	 * @param is the input stream from an HttpURLConnection
	 * @return true if the error response appears to be from an s3-backend
	 */
	private static boolean isS3ErrorResponse(final InputStream is) {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			final XPath xPath = XPathFactory.newInstance().newXPath();
			final Document doc = factory.newDocumentBuilder().parse(is);
			final String errorCode = xPath.evaluate("/Error/Code/text()", doc);

			return EXPECTED_ERROR_CODES.contains(errorCode);
		} catch (final Exception ignored) {}
		return false;
	}

	private static final HashSet<String> EXPECTED_ERROR_CODES = new HashSet<>();
	static {
		EXPECTED_ERROR_CODES.add("NoSuchKey");
		EXPECTED_ERROR_CODES.add("AccessDenied");
	}
}

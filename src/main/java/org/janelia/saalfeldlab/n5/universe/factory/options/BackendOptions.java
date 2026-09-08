package org.janelia.saalfeldlab.n5.universe.factory.options;

import java.util.function.Consumer;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Options;
import org.janelia.saalfeldlab.n5.universe.factory.spi.GoogleCloudOptions;

/**
 * How the bytes are fetched: one options object per backend.
 * <p>
 * This is the half of {@link N5FactoryOptions} that
 * {@code KeyValueRoots} needs; it never sees {@link FormatOptions}.
 * There is no fallback chain here — unlike the storage formats, the backends
 * share no settings.
 */
public class BackendOptions {

	private final AmazonS3Options amazonS3Options = new AmazonS3Options();
	private final GoogleCloudOptions googleCloudOptions = new GoogleCloudOptions();
	private final HttpOptions httpOptions = new HttpOptions();

	private boolean probeHttpWithS3Client = false;

	/**
	 * Configure the options used to reach an S3 (or S3-compatible) backend.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public BackendOptions s3(final Consumer<AmazonS3Options> configure) {

		configure.accept(amazonS3Options);
		return this;
	}

	public AmazonS3Options s3() {

		return amazonS3Options;
	}

	/**
	 * Configure the options used to reach a Google Cloud Storage backend.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public BackendOptions googleCloud(final Consumer<GoogleCloudOptions> configure) {

		configure.accept(googleCloudOptions);
		return this;
	}

	public GoogleCloudOptions googleCloud() {

		return googleCloudOptions;
	}

	/**
	 * Configure the options used to reach a plain HTTP backend. The timeouts
	 * also bound the single request that tells an S3-compatible endpoint from
	 * an ordinary HTTP server.
	 *
	 * @param configure performs custom configuration on the options
	 * @return this
	 */
	public BackendOptions http(final Consumer<HttpOptions> configure) {

		configure.accept(httpOptions);
		return this;
	}

	public HttpOptions http() {

		return httpOptions;
	}

	/**
	 * How hard to work at resolving an ambiguous {@code http(s)} URI — one
	 * that is read-only, not on a known Google Cloud host, and that the cheap
	 * HTTP probe (an {@code x-amz-*} header, a {@code ListBucketResult} or an
	 * {@code AccessDenied} body) could not settle.
	 * <p>
	 * When set, such a URI is additionally probed with an S3 client. That costs
	 * real time — building the client resolves the credential chain, and the
	 * bucket request may be retried before it fails — which is why it is off by
	 * default. With it off, an unrecognised endpoint falls through to plain
	 * HTTP; with it on, an S3-compatible endpoint that answers only signed
	 * requests is still found. {@code master} always probed.
	 * <p>
	 * This sits on {@code BackendOptions} rather than on
	 * {@link AmazonS3Options} because it arbitrates <em>between</em> two
	 * backends; it says nothing about how to build an S3 client.
	 *
	 * @param probeHttpWithS3Client whether to probe ambiguous http(s) URIs with an S3 client
	 * @return this
	 */
	public BackendOptions probeHttpWithS3Client(final boolean probeHttpWithS3Client) {

		this.probeHttpWithS3Client = probeHttpWithS3Client;
		return this;
	}

	public boolean probeHttpWithS3Client() {

		return probeHttpWithS3Client;
	}
}

package org.janelia.saalfeldlab.n5.universe.factory.spi;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Configuration for the Amazon S3 (and S3-compatible) backend.
 * <p>
 * This is pure data and contains nothing that requires {@code
 * n5-aws-s3} or the AWS SDK at runtime. {@code S3Client} and {@code
 * S3ClientBuilder} appear only as generic parameters, which erase to
 * {@link Consumer} / {@link Function} in the method descriptors.
 */
public class AmazonS3Options {

	private Consumer<S3ClientBuilder> builderConfig = null;

	private Function<URI, S3Client> clientProvider = null;

	/**
	 * Use {@code builderConfig} to configure the {@link S3ClientBuilder},
	 * and thereby the {@link S3Client}.
	 *
	 * @param builderConfig configures the s3 client builder
	 * @return {@code this}
	 */
	public AmazonS3Options builderConfig(final Consumer<S3ClientBuilder> builderConfig) {

		this.builderConfig = builderConfig;
		return this;
	}

	public Consumer<S3ClientBuilder> builderConfig() {

		return builderConfig;
	}

	/**
	 * Build {@link S3Client}s with {@code clientProvider} instead of with
	 * the default mechanism. This replaces client creation entirely, so
	 * {@link #builderConfig} is then ignored. Set it to pin an
	 * existing client (for example for testing).
	 *
	 * @param clientProvider builds a client for a given container URI
	 * @return {@code this}
	 */
	public AmazonS3Options clientProvider(final Function<URI, S3Client> clientProvider) {

		this.clientProvider = clientProvider;
		return this;
	}

	public Function<URI, S3Client> clientProvider() {

		return clientProvider;
	}
}

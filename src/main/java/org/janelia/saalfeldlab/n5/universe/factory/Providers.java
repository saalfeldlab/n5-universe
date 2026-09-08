package org.janelia.saalfeldlab.n5.universe.factory;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Provider;
import org.janelia.saalfeldlab.n5.universe.factory.spi.GoogleCloudProvider;
import org.janelia.saalfeldlab.n5.universe.factory.spi.Hdf5Provider;
import org.janelia.saalfeldlab.n5.universe.kvr.split.DefaultAmazonS3Provider;
import org.janelia.saalfeldlab.n5.universe.kvr.split.DefaultGoogleCloudProvider;
import org.janelia.saalfeldlab.n5.universe.kvr.split.DefaultHdf5Provider;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * The service implementations (aws-s3, google-cloud, hdf5) that are available.
 * <p>
 * Each backend is resolved independently and lazily, on first use, by its own
 * holder class. That isolation is deliberate: resolution can fail (a malformed
 * {@code META-INF/services} file, a provider that cannot be instantiated), and
 * one failing backend must not take the others down with it — which is exactly
 * what would happen if both were resolved in this class's own static
 * initializer.
 */
public final class Providers {

	private Providers() {}

	/**
	 * @return whether the S3 backend is available
	 */
	public static boolean hasAmazonS3() {

		return AmazonS3Holder.INSTANCE != null;
	}

	/**
	 * @return the S3 backend
	 * @throws N5Exception if none is available
	 */
	public static AmazonS3Provider amazonS3() {

		return require(AmazonS3Holder.INSTANCE, "S3", "n5-aws-s3");
	}

	/**
	 * @return whether the Google Cloud Storage backend is available
	 */
	public static boolean hasGoogleCloud() {

		return GoogleCloudHolder.INSTANCE != null;
	}

	/**
	 * @return the Google Cloud Storage backend
	 * @throws N5Exception if none is available
	 */
	public static GoogleCloudProvider googleCloud() {

		return require(GoogleCloudHolder.INSTANCE, "Google Cloud Storage", "n5-google-cloud");
	}

	/**
	 * @return whether the HDF5 format is available
	 */
	public static boolean hasHdf5() {

		return Hdf5Holder.INSTANCE != null;
	}

	/**
	 * @return the HDF5 format
	 * @throws N5Exception if none is available
	 */
	public static Hdf5Provider hdf5() {

		return require(Hdf5Holder.INSTANCE, "HDF5", "n5-hdf5");
	}

	private static final class AmazonS3Holder {

		static final AmazonS3Provider INSTANCE = find(AmazonS3Provider.class, DefaultAmazonS3Provider::new);
	}

	private static final class GoogleCloudHolder {

		static final GoogleCloudProvider INSTANCE = find(GoogleCloudProvider.class, DefaultGoogleCloudProvider::new);
	}

	private static final class Hdf5Holder {

		static final Hdf5Provider INSTANCE = find(Hdf5Provider.class, DefaultHdf5Provider::new);
	}

	/*
	 * SPLIT: the single seam. Today the implementations ship in this artifact,
	 * so we just instantiate the one we know. When they move to
	 * n5-aws-s3 / n5-google-cloud / n5-hdf5 this becomes a ServiceLoader lookup and
	 * the `bundled` argument goes away:
	 *
	 *     final Iterator<T> it = ServiceLoader
	 *             .load(service, service.getClassLoader())   // never the TCCL
	 *             .iterator();
	 *     return it.hasNext() ? it.next() : null;
	 *
	 * Verified against real discovery before being reduced to this: with
	 * META-INF/services entries in place, the providers were found through
	 * ServiceLoader from the same artifact. See CLAUDE.md.
	 *
	 * Open decision for then: what two providers claiming one service means.
	 * Throwing beats letting classpath order pick silently.
	 */
	@Nullable
	private static <T> T find(final Class<T> service, final Supplier<T> bundled) {

		return bundled.get();
	}

	private static <T> T require(final T provider, final String what, final String artifact) {

		if (provider == null)
			throw new N5Exception("No " + what + " support on the classpath. Add the " + artifact + " artifact.");
		return provider;
	}
}

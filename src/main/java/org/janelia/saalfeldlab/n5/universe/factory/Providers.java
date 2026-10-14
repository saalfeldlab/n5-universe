package org.janelia.saalfeldlab.n5.universe.factory;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Provider;
import org.janelia.saalfeldlab.n5.universe.factory.spi.GoogleCloudProvider;
import org.janelia.saalfeldlab.n5.universe.factory.spi.Hdf5Provider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * The service implementations (aws-s3, google-cloud, hdf5) that are available.
 * <p>
 * The implementations are in {@code n5-aws-s3}, {@code n5-google-cloud} and
 * {@code n5-hdf5}, and are discovered through {@link ServiceLoader}. A backend
 * whose artifact is not on the classpath is simply absent: {@code has*()}
 * returns {@code false}, and the accessor throws an {@link N5Exception} naming
 * the artifact to add.
 * <p>
 * Each backend is resolved independently and lazily, on first use, by its own
 * holder class (to avoid instantiation failure of one provider to affect the
 * others).
 */
final class Providers {

	private Providers() {}

	/**
	 * @return whether the S3 backend is available
	 */
	static boolean hasAmazonS3() {

		return AmazonS3Holder.LOOKUP.provider != null;
	}

	/**
	 * @return the S3 backend
	 * @throws N5Exception if none is available
	 */
	static AmazonS3Provider amazonS3() {

		return AmazonS3Holder.LOOKUP.require("S3", "n5-aws-s3");
	}

	/**
	 * @return whether the Google Cloud Storage backend is available
	 */
	static boolean hasGoogleCloud() {

		return GoogleCloudHolder.LOOKUP.provider != null;
	}

	/**
	 * @return the Google Cloud Storage backend
	 * @throws N5Exception if none is available
	 */
	static GoogleCloudProvider googleCloud() {

		return GoogleCloudHolder.LOOKUP.require("Google Cloud Storage", "n5-google-cloud");
	}

	/**
	 * @return whether the HDF5 format is available
	 */
	static boolean hasHdf5() {

		return Hdf5Holder.LOOKUP.provider != null;
	}

	/**
	 * @return the HDF5 format
	 * @throws N5Exception if none is available
	 */
	static Hdf5Provider hdf5() {

		return Hdf5Holder.LOOKUP.require("HDF5", "n5-hdf5");
	}

	private static final class AmazonS3Holder {

		static final Lookup<AmazonS3Provider> LOOKUP = find(AmazonS3Provider.class);
	}

	private static final class GoogleCloudHolder {

		static final Lookup<GoogleCloudProvider> LOOKUP = find(GoogleCloudProvider.class);
	}

	private static final class Hdf5Holder {

		static final Lookup<Hdf5Provider> LOOKUP = find(Hdf5Provider.class);
	}

	/**
	 * The outcome of looking up one service: a provider, or nothing, and if
	 * nothing then possibly why.
	 */
	private static final class Lookup<T> {

		final T provider;

		/** why the lookup failed, or {@code null} if there simply is no provider */
		final N5Exception failure;

		Lookup(final T provider, final N5Exception failure) {

			this.provider = provider;
			this.failure = failure;
		}

		T require(final String what, final String artifact) {

			if (provider != null)
				return provider;
			if (failure != null) // a fresh exception, so the stack trace shows this caller
				throw new N5Exception(failure.getMessage(), failure.getCause());
			throw new N5Exception("No " + what + " support on the classpath. Add the " + artifact + " artifact.");
		}
	}

	/**
	 * Load through the {@code service} type's class loader, not the
	 * thread-context class loader: a wrong ClassLoader may silently
	 * find nothing.
	 * <p>
	 * Currently, finding more than one implementation of a service is
	 * treated as an error. Randomly using the first one might be
	 * dangerous. (ServiceLoader already ignores a provider class named
	 * more than once (e.g. the same jar on the classpath twice.)
	 */
	private static <T> Lookup<T> find(final Class<T> service) {

		try {
			final Iterator<T> it = ServiceLoader.load(service, service.getClassLoader()).iterator();
			final List<T> found = new ArrayList<>();
			while (it.hasNext())
				found.add(it.next());

			if (found.isEmpty())
				return new Lookup<>(null, null);
			if (found.size() > 1) {
				final List<String> names = new ArrayList<>();
				for (final T provider : found)
					names.add(provider.getClass().getName());
				return new Lookup<>(null, new N5Exception(
						"Found " + found.size() + " implementations of " + service.getName() + " on the classpath: "
								+ String.join(", ", names) + ". Remove all but one."));
			}
			return new Lookup<>(found.get(0), null);

		} catch (final ServiceConfigurationError | LinkageError | RuntimeException e) {
			return new Lookup<>(null, new N5Exception("Could not load an implementation of " + service.getName(), e));
		}
	}
}

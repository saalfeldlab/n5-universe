package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * An {@link N5Factory} that caches opened readers and writers by normalized URI.
 * <p>
 * This class is kept for compatibility, and forwards to
 * {@link org.janelia.saalfeldlab.n5.universe.factory.N5FactoryWithCache}, which is
 * where the caching is implemented.
 */
public class N5FactoryWithCache extends N5Factory {

	public N5FactoryWithCache() {

		super(new org.janelia.saalfeldlab.n5.universe.factory.N5FactoryWithCache());
	}

	/**
	 * @return the {@link org.janelia.saalfeldlab.n5.universe.factory.N5FactoryWithCache} this forwards to
	 */
	@Override
	public org.janelia.saalfeldlab.n5.universe.factory.N5FactoryWithCache getFactory() {

		return (org.janelia.saalfeldlab.n5.universe.factory.N5FactoryWithCache)super.getFactory();
	}

	/**
	 * Open a reader for the container. If a cached reader is present, return it.
	 * If no cached reader, but a cached writer, return it as an N5Reader.
	 * Finally, open a new reader if not present in the cache.
	 *
	 * @param storage the storage format, or null
	 * @param root root of the container
	 * @return the N5Reader
	 */
	public N5Reader openReaderAllowCachedWriter(@Nullable final StorageFormat storage, final KeyValueRoot root) {

		return getFactory().openReaderAllowCachedWriter(storage, root);
	}

	/**
	 * Open a reader for the container. If a cached reader is present, return it.
	 * If no cached reader, but a cached writer, return it as an N5Reader.
	 * Finally, open a new reader if not present in the cache.
	 *
	 * @param uri the container location, optionally prefixed with a storage format
	 * @return the N5Reader
	 */
	public N5Reader openReaderAllowCachedWriter(final String uri) {

		return getFactory().openReaderAllowCachedWriter(uri);
	}

	/**
	 * Drop all cached readers and writers.
	 */
	public void clear() {

		getFactory().clear();
	}

	/**
	 * Drop the cached reader and writer for {@code uri}, if any.
	 *
	 * @return whether anything was removed
	 */
	public boolean remove(final URI uri) {

		return getFactory().remove(uri);
	}

	/**
	 * Drop the cached reader and writer for {@code uri}, if any.
	 *
	 * @param uri the container location, optionally prefixed with a storage format
	 * @return whether anything was removed
	 */
	public boolean remove(final String uri) {

		return getFactory().remove(uri);
	}
}

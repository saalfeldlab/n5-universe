package org.janelia.saalfeldlab.n5.universe.factory;

import com.google.gson.JsonElement;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An {@link N5Factory} that caches opened readers and writers by normalized URI.
 * <p>
 * {@code openReader}/{@code openWriter} calls on cached containers are non-blocking and
 * thread-safe. If the container is not cached yet, the open call synchronizes on {@code this}
 * instance to ensure thread safety.
 * <p>
 * The cache is inserted at {@link N5Factory#openReaderHelper} /
 * {@link N5Factory#openWriterHelper}, the single point every {@code openReader}/{@code openWriter}
 * overload funnels through. It is keyed by the {@link KeyValueRoot#uri() root URI}, normalized,
 * and an entry is only handed out if it still matches the requested {@link StorageFormat} and
 * still reads (resp. writes).
 */
public class N5FactoryWithCache extends N5Factory {

	private final ConcurrentHashMap<URI, N5Reader> readerCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<URI, N5Writer> writerCache = new ConcurrentHashMap<>();

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

		final N5Reader cachedReader = getCachedReaderAllowWriter(storage, root.uri());
		if (cachedReader != null)
			return cachedReader;

		return openReader(storage, root);
	}

	/**
	 * Open a reader for the container. If a cached reader is present, return it.
	 * If no cached reader, but a cached writer, return it as an N5Reader.
	 * Finally, open a new reader if not present in the cache.
	 * <p>
	 * NB: On a cache hit no {@link KeyValueRoot} is created, which is the point of taking the
	 * URI as a {@code String} here — for an {@code s3://} container, building the root means
	 * building a client.
	 *
	 * @param uri the container location, optionally prefixed with a storage format
	 * @return the N5Reader
	 */
	public N5Reader openReaderAllowCachedWriter(final String uri) {

		/* Only an explicit storage-scheme prefix counts here: a guess from the path suffix is
		 * not what openReaderHelper() would have keyed the entry under. */
		final InferredStorageFormat inferred = InferredStorageFormat.of(uri);
		final StorageFormat storage = inferred.isExplicit() ? inferred.format() : null;

		final N5Reader cachedReader = getCachedReaderAllowWriter(storage, inferred.uri());
		if (cachedReader != null)
			return cachedReader;

		return openReader(uri);
	}

	@Nullable
	private N5Reader getCachedReaderAllowWriter(@Nullable final StorageFormat storage, final URI location) {

		/* try to get a cached reader */
		final N5Reader readerFromCache = getReaderFromCache(storage, location);
		if (readerFromCache != null)
			return readerFromCache;

		/* else see if we have a cached writer we can return as an N5Reader */
		final boolean dontTestWrite = false;
		return getWriterFromCache(storage, location, dontTestWrite);
	}

	@Override
	protected N5Reader openReaderHelper(@Nullable final StorageFormat format, final KeyValueRoot root, final URI uri) {

		/* if possible, return cached reader without synchronizing */
		final N5Reader cached = getReaderFromCache(format, root.uri());
		if (cached != null)
			return cached;

		synchronized (this) {

			/* another thread may have opened it while we waited */
			final N5Reader reopened = getReaderFromCache(format, root.uri());
			if (reopened != null)
				return reopened;

			final N5Reader reader = super.openReaderHelper(format, root, uri);
			if (reader != null)
				readerCache.put(normalizeUri(root.uri()), reader);
			return reader;
		}
	}

	@Nullable
	protected N5Reader getReaderFromCache(@Nullable final StorageFormat format, final URI location) {

		final URI uri = normalizeUri(location);
		final N5Reader reader = readerCache.get(uri);
		if (reader == null)
			return null;

		final boolean readerIsValid = n5MatchesFormat(reader, format) && canRead(reader);
		if (!readerIsValid) {
			readerCache.remove(uri, reader);
			return null;
		}
		return reader;
	}

	@Override
	protected N5Writer openWriterHelper(@Nullable final StorageFormat format, final KeyValueRoot root, final URI uri) {

		final boolean testWrite = true;
		final N5Writer cached = getWriterFromCache(format, root.uri(), testWrite);
		if (cached != null)
			return cached;

		synchronized (this) {
			/* see if someone opened this while we were waiting. */
			final N5Writer reopened = getWriterFromCache(format, root.uri(), testWrite);
			if (reopened != null)
				return reopened;

			final N5Writer writer = super.openWriterHelper(format, root, uri);
			if (writer != null)
				writerCache.put(normalizeUri(root.uri()), writer);
			return writer;
		}
	}

	@Nullable
	protected N5Writer getWriterFromCache(@Nullable final StorageFormat format, final URI location, final boolean testWrite) {

		final URI uri = normalizeUri(location);
		final N5Writer writer = writerCache.get(uri);
		if (writer == null)
			return null;

		final boolean writerIsValid = n5MatchesFormat(writer, format) && (!testWrite || canWrite(writer));

		if (!writerIsValid) {
			writerCache.remove(uri, writer);
			return null;
		}
		return writer;
	}

	/**
	 * Test if a read attempt on this {@link N5Reader} will succeed.
	 *
	 * @return if read was successful
	 */
	private boolean canRead(final N5Reader reader) {

		try {
			/* NB: Ask the KeyValueRoot, not the reader. The root sits *below* the
			 * HierarchyCache, so it is live by construction, where a cached reader would
			 * answer getAttribute() from its own cache and report a container that has since
			 * vanished as readable — which is exactly what this check is for. It is also
			 * cheaper than reading the root attributes.
			 *
			 * N5DirectoryPath.of("") is how HierarchyCache's own constructor names the root. */
			if (reader instanceof GsonKeyValueN5Reader)
				return ((GsonKeyValueN5Reader)reader).getKeyValueRoot().isDirectory(N5DirectoryPath.of(""));

			/* fallback for readers with no KeyValueRoot, i.e. HDF5 */
			reader.getAttribute("/", "/", JsonElement.class);
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * Test if a write attempt on this {@link N5Writer} will succeed.
	 * Writes a dummy placeholder metadata key/value that is immediately removed if successful.
	 *
	 * @return if write was successful
	 */
	private boolean canWrite(final N5Writer writer) {

		/* If we already have a writer, and we `canRead`, it means we passed the `canWrite` check the first time.
		 * In this case, that's enough evidence that we `canWrite`. It doesn't guarantee, e.g someone didn't change
		 * permissions underneath us, but we never guarded against that anyway. */
		if (canRead(writer))
			return true;

		synchronized (this) {
			final String uuid = UUID.randomUUID().toString();
			try {
				/* Not ideal, but we want to guarantee that the writer is write-able.
				 * Normally this is guaranteed at construction, but it isn't being constructed here. */
				writer.setAttribute("/", uuid, "CACHE_CHECK");
				return true;
			} catch (final Exception e) {
				return false;
			} finally {
				try {
					/* If it fails, defer to the result of the outer try/catch*/
					writer.removeAttribute("/", uuid);
				} catch (final Exception ignored) {
				}
			}
		}
	}

	@Override
	protected void requireContainerExists(final Supplier<N5Reader> getReader) throws N5IOException {
		N5Reader reader = null;
		try {
			reader = getReader.get();
		} catch (final Exception e) {
			throw new N5IOException("Existing container could not be opened, or does not exist.", e);
		} finally {
			/* For the N5FactoryWithCache, we don't want to close the reader necessarily, since
			 * we may have it cached for later re-use. However, for HDF5 specifically, we must close it
			 * since it is not valid to have a reader and writer open at the same time. */
			if (isHdf5(reader))
				reader.close();
		}
	}

	private boolean n5MatchesFormat(final N5Reader reader, @Nullable final StorageFormat format) {

		// succeed if no format to compare against
		if (format == null)
			return true;

		switch (format) {
		case N5:
			return reader instanceof N5KeyValueReader && !(reader instanceof ZarrV3KeyValueReader);
		case ZARR2:
			return reader instanceof ZarrKeyValueReader;
		case ZARR3:
			return reader instanceof ZarrV3KeyValueReader;
		case ZARR:
			return reader instanceof ZarrKeyValueReader || reader instanceof ZarrV3KeyValueReader;
		case HDF5:
			return isHdf5(reader);
		default:
			return true; // StorageFormat has no other values, but Java 8 doesn't know that
		}
	}

	/*
	 * Whether a reader *the factory opened* is HDF5. No n5-hdf5 type is named here, since
	 * this package must not depend on n5-hdf5: HDF5 is the one format Containers builds that
	 * is not a key-value container (canRead() relies on the same dichotomy), and only readers
	 * the factory opened ever reach this cache. Revisit if Containers ever builds another
	 * non-key-value format.
	 */
	private static boolean isHdf5(@Nullable final N5Reader reader) {

		return reader != null && !(reader instanceof GsonKeyValueN5Reader);
	}

	/**
	 * Drop all cached readers and writers.
	 */
	public void clear() {
		readerCache.clear();
		writerCache.clear();
	}

	/**
	 * Drop the cached reader and writer for {@code uri}, if any.
	 *
	 * @return whether anything was removed
	 */
	public boolean remove(final URI uri) {

		final URI normalUri = normalizeUri(uri);
		boolean removed = readerCache.remove(normalUri) != null;
		removed |= writerCache.remove(normalUri) != null;
		return removed;
	}

	/**
	 * Drop the cached reader and writer for {@code uri}, if any.
	 *
	 * @param uri the container location, optionally prefixed with a storage format
	 * @return whether anything was removed
	 */
	public boolean remove(final String uri) {

		return remove(InferredStorageFormat.of(uri).uri());
	}

	private static URI normalizeUri(final URI uri) {
		if (uri.isAbsolute() && !uri.getScheme().equals("file"))
			return uri.normalize();

		final URI uriFromPath;
		if (uri.isAbsolute())
			uriFromPath = Paths.get(uri).normalize().toUri();
		else
			uriFromPath = Paths.get(uri.getPath()).normalize().toUri();

		/* By Default, Path.toUri() will add a trailing `/` if it is a directory.
		 * The problem is if it is a directory that doesn't exist yet (e.g. creating a new writer).
		 * then the first time the URI will have no trailing `/` and the next time it will. This
		 * causes the cache to consider them different keys.
		 *
		 * We remove the leading slash in all cases, to avoid this issue.  */
		final String uriWithoutTrailingSlash = uriFromPath.toString().replaceAll("/$", "");
		return URI.create(uriWithoutTrailingSlash).normalize();
	}
}

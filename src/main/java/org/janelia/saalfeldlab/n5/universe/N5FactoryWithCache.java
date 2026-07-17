package org.janelia.saalfeldlab.n5.universe;

import com.google.gson.JsonElement;
import net.imglib2.util.Pair;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;

import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An {@link N5Factory} that caches opened readers and writers by normalized URI.
 * <p>
 * `openReader`/`openWriter` calls on cached containers is non-blocking and thread-safe.
 * If the container is not cached yet, the open call syncronizes on `this` instance to ensure
 * thread safe.
 */
public class N5FactoryWithCache extends N5Factory {

	private final ConcurrentHashMap<URI, N5Reader> readerCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<URI, N5Writer> writerCache = new ConcurrentHashMap<>();

	/**
	 * Open a reader for the container. If a cached reader is present, return it.
	 * If no cached reader, but a cached writer, return it as an N5Reader.
	 * Finally, open a new reader if not present in the cache.
	 */
	public N5Reader openReaderAllowCachedWriter(StorageFormat storage, KeyValueAccess access, URI location) {

		N5Reader cachedReader = getCachedReaderAllowWriter(storage, location);
		if (cachedReader != null)
			return cachedReader;

		return cachedOpenReader(storage, access, location);
	}

	/**
	 * Open a reader for the container. If a cached reader is present, return it.
	 * If no cached reader, but a cached writer, return it as an N5Reader.
	 * Finally, open a new reader if not present in the cache.
	 */
	public N5Reader openReaderAllowCachedWriter(String uri) {

		final Pair<StorageFormat, URI> parsed = StorageFormat.parseUri(uri);
		final StorageFormat storage = parsed.getA();
		final URI location = parsed.getB();

		N5Reader cachedReader = getCachedReaderAllowWriter(storage, location);
		if (cachedReader != null)
			return cachedReader;

		return openReader(uri);
	}

	private N5Reader getCachedReaderAllowWriter(StorageFormat storage, URI location) {
		/* try to get a cached reader*/
		final N5Reader readerFromCache = getReaderFromCache(storage, location);
		if (readerFromCache != null)
			return readerFromCache;

		/* else see if we have a cached writer we can return as an N5Reader*/
		final boolean dontTestWrite = false;
        return getWriterFromCache(storage, location, dontTestWrite);
    }

	private N5Reader cachedOpenReader(StorageFormat storage, KeyValueAccess access, URI location) {

		/* if possible, return cached reader without synchronizing */
		final N5Reader cached = getReaderFromCache(storage, location);
		if (cached != null)
			return cached;

		synchronized (this) {

			/* another thread may have opened it while we waited */
			final N5Reader reopened = getReaderFromCache(storage, location);
			if (reopened != null)
				return reopened;

			final N5Reader reader = super.openReader(storage, access, location);
			if (reader != null)
				readerCache.put(normalizeUri(location), reader);
			return reader;
		}
	}

	@Override
	public N5Reader openReader(StorageFormat storage, KeyValueAccess access, URI location) {

		return cachedOpenReader(storage, access, location);
	}

	protected N5Reader getReaderFromCache(StorageFormat format, URI location) {

		final URI uri = normalizeUri(location);
		final N5Reader reader = readerCache.get(uri);
		if (reader == null)
			return null;

		boolean readerIsValid = n5MatchesFormat(reader, format) && canRead(reader);
		if (!readerIsValid) {
			readerCache.remove(uri, reader);
			return null;
		}
		return reader;
	}

	@Override
	public N5Writer openWriter(StorageFormat storage, KeyValueAccess access, URI location) {

		final boolean testWrite = true;
		final N5Writer cached = getWriterFromCache(storage, location, testWrite);
		if (cached != null)
			return cached;

        synchronized (this) {
			/* see if someone opened this while we were waiting. */
			final N5Writer reopened = getWriterFromCache(storage, location, testWrite);
			if (reopened != null)
				return reopened;

			final N5Writer writer = super.openWriter(storage, access, location);
			if (writer != null)
				writerCache.put(normalizeUri(location), writer);
			return writer;
		}
	}

	protected N5Writer getWriterFromCache(StorageFormat format, URI location, boolean testWrite) {

		final URI uri = normalizeUri(location);
		final N5Writer writer = writerCache.get(uri);
		if (writer == null)
			return null;

		boolean writerIsValid = n5MatchesFormat(writer, format) && (!testWrite || canWrite(writer));

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
	private boolean canRead(N5Reader reader) {

		try {
			if (reader instanceof CachedGsonKeyValueN5Reader) {
				((CachedGsonKeyValueN5Reader)reader).getAttributesFromContainer("/", "/");
			} else {
				reader.getAttribute("/", "/", JsonElement.class);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Test if a write attempt on this {@link N5Writer} will succeed.
	 * Writes a dummy placeholder metadata key/value that is immediately removed if successful.
	 *
	 * @return if write was successful
	 */

	private boolean canWrite(N5Writer writer) {

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
			} catch (Exception e) {
				return false;
			} finally {
				try {
					/* If it fails, defer to the result of the outer try/catch*/
					writer.removeAttribute("/", uuid);
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Override
	protected void requireContainerExists(Supplier<N5Reader> getReader) throws N5Exception.N5IOException {
		N5Reader reader = null;
		try {
			reader = getReader.get();
		} catch (final Exception e) {
			throw new N5Exception.N5IOException("Existing container could not be opened, or does not exist.", e);
		} finally {
			/* For the N5FactoryWithCache, we don't want to close the reader necessarily, since w
			* we may have it cached for later re-use. However, for HDF5 specifically, we must close it
			* since it is not valid to have a reader and writer open at the same time. */
			if (reader instanceof N5HDF5Reader)
				reader.close();
		}
	}

	private boolean n5MatchesFormat(N5Reader reader, StorageFormat format) {

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
			return reader instanceof N5HDF5Reader;
		default:
			return true; // StorageFormat has no other values, but Java 8 doesn't know that
		}
	}

	public void clear() {
		readerCache.clear();
		writerCache.clear();
	}

	public boolean remove(URI uri) {

		final URI normalUri = normalizeUri(uri);
		boolean removed = readerCache.remove(normalUri) != null;
		removed |= writerCache.remove(normalUri) != null;
		return removed;
	}

	public boolean remove(String uri) {
		final Pair<StorageFormat, URI> storageFormatURIPair = StorageFormat.parseUri(uri);
		return remove(storageFormatURIPair.getB());
	}

	private static URI normalizeUri(URI uri) {
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

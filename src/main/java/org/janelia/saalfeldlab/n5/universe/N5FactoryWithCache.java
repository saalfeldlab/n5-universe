package org.janelia.saalfeldlab.n5.universe;

import com.google.gson.JsonElement;
import net.imglib2.util.Pair;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

public class N5FactoryWithCache extends N5Factory {

	private final HashMap<URI, N5Reader> readerCache = new HashMap<>();
	private final HashMap<URI, N5Writer> writerCache = new HashMap<>();

	@Override
	public N5Reader openReader(StorageFormat storage, KeyValueAccess access, URI location) {

		final N5Reader reader = getReaderFromCache(storage, location);
		if (reader != null)
			return reader;
		return openAndCacheReader(storage, access, location);
	}

	private N5Reader openAndCacheReader(StorageFormat storageFormat, KeyValueAccess access, URI uri) {

		final N5Reader reader = super.openReader(storageFormat, access, uri);
		if (reader == null)
			return null;

		URI normalUri = normalizeUri(uri);
		readerCache.put(normalUri, reader);
		return reader;
	}

	protected synchronized N5Reader getReaderFromCache(StorageFormat format, URI location) {

		final URI uri = normalizeUri(location);
		final N5Reader reader = readerCache.get(uri);
		if (reader == null)
			return null;

		if (!n5MatchesFormat(reader, format) || !canRead(reader)) {
			readerCache.remove(uri);
			return null;
		}

		return reader;
	}

	@Override
	public N5Writer openWriter(StorageFormat storage, KeyValueAccess access, URI location) {

		final N5Writer writer = getWriterFromCache(storage, location);
		if (writer != null)
			return writer;
		return openAndCacheWriter(storage, access, location);
	}

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

	private N5Writer openAndCacheWriter(StorageFormat storageFormat, KeyValueAccess access, URI uri) {

		final N5Writer writer = super.openWriter(storageFormat, access, uri);
		if (writer == null)
			return null;

		URI normalUri = normalizeUri(uri);
		writerCache.put(normalUri, writer);
		return writer;
	}

	protected synchronized N5Writer getWriterFromCache(StorageFormat format, URI uri) {

		URI normalUri = normalizeUri(uri);
		final N5Writer writer = writerCache.get(normalUri);
		if (writer == null)
			return null;

		if (!n5MatchesFormat(writer, format) || !canWrite(writer)) {
			writerCache.remove(normalUri);
			return null;
		}

		return writer;
	}

	private boolean canWrite(N5Writer writer) {

		if (canRead(writer))
			return true;

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

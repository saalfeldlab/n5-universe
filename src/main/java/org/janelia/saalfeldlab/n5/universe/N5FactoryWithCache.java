package org.janelia.saalfeldlab.n5.universe;

import com.google.gson.JsonElement;
import net.imglib2.util.Pair;
import org.janelia.saalfeldlab.n5.CachedGsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

public class N5FactoryWithCache extends N5Factory {

	private final HashMap<URI, N5Reader> readerCache = new HashMap<>();
	private final HashMap<URI, N5Writer> writerCache = new HashMap<>();

	@Override public N5Reader openReader(String uri) {

		final Pair<StorageFormat, URI> storageFormatURIPair = StorageFormat.parseUri(uri);
		return openReader(storageFormatURIPair.getA(), storageFormatURIPair.getB());
	}

	@Override public N5Reader openReader(StorageFormat format, String uri) {

		final URI asUri = parseUriFromString(uri);
		return openReader(format, asUri);
	}

	@Override public N5Reader openReader(StorageFormat format, URI uri) {

		final URI normalUri = normalizeUri(uri);
		final N5Reader reader = getReaderFromCache(format, normalUri);
		if (reader != null)
			return reader;
		return openAndCacheReader(format, normalUri);
	}

	private N5Reader openAndCacheReader(StorageFormat storageFormat, URI uri) {

		final N5Reader reader = super.openReader(storageFormat, uri);
		if (reader == null)
			return null;

		readerCache.put(uri, reader);
		return reader;
	}

	protected synchronized N5Reader getReaderFromCache(StorageFormat format, URI uri) {

		final N5Reader reader = readerCache.get(uri);
		if (reader == null)
			return null;

		if (!n5MatchesFormat(reader, format) || !canRead(reader)) {
			readerCache.remove(uri);
			return null;
		}

		return reader;
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

	@Override public N5Writer openWriter(String uri) {

		final Pair<StorageFormat, URI> storageFormatURIPair = StorageFormat.parseUri(uri);
		return openWriter(storageFormatURIPair.getA(), storageFormatURIPair.getB());
	}

	@Override public N5Writer openWriter(StorageFormat format, String uri) {

		final URI asUri = parseUriFromString(uri);
		return openWriter(format, asUri);
	}

	@Override public N5Writer openWriter(StorageFormat format, URI uri) {

		final URI normalUri = normalizeUri(uri);
		final N5Writer writer = getWriterFromCache(format, normalUri);
		if (writer != null)
			return writer;
		return openAndCacheWriter(format, normalUri);
	}

	private N5Writer openAndCacheWriter(StorageFormat storageFormat, URI uri) {

		final N5Writer writer = super.openWriter(storageFormat, uri);
		if (writer == null)
			return null;

		writerCache.put(uri, writer);
		return writer;
	}

	protected synchronized N5Writer getWriterFromCache(StorageFormat format, URI uri) {


		final N5Writer writer = writerCache.get(uri);
		if (writer == null)
			return null;

		if (!n5MatchesFormat(writer, format) || !canWrite(writer)) {
			writerCache.remove(uri);
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
			return reader instanceof N5KeyValueReader;
		case ZARR:
			return reader instanceof ZarrKeyValueReader;
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
		if (uri.isAbsolute())
			return uri.normalize();
		return Paths.get(uri.toString()).normalize().toUri().normalize();
	}
}

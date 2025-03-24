package org.janelia.saalfeldlab.n5.universe;

import com.google.gson.JsonElement;
import net.imglib2.util.Pair;
import org.janelia.saalfeldlab.n5.CachedGsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import java.net.URI;
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

		N5Reader reader = getReaderFromCache(uri);
		if (reader != null)
			reader = openAndCacheReader(format, uri);
		return reader;
	}

	private N5Reader openAndCacheReader(StorageFormat storageFormat, URI uri) {

		final N5Reader reader = super.openReader(storageFormat, uri);
		if (reader == null)
			return null;

		readerCache.put(uri.normalize(), reader);
		return reader;
	}

	private synchronized N5Reader getReaderFromCache(URI uri) {

		final URI normalUri = uri.normalize();
		final N5Reader reader = readerCache.get(normalUri);
		if (reader != null && !canRead(reader)) {
			/* Can't from cached reader, clear from the cache and return null */
			readerCache.remove(normalUri);
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

		N5Writer writer = getWriterFromCache(uri);
		if (writer != null)
			writer = openAndCacheWriter(format, uri);
		return writer;
	}

	private N5Writer openAndCacheWriter(StorageFormat storageFormat, URI uri) {

		final N5Writer writer = super.openWriter(storageFormat, uri);
		if (writer == null)
			return null;

		writerCache.put(uri, writer);
		return writer;
	}

	private synchronized N5Writer getWriterFromCache(URI uri) {

		final N5Writer writer = writerCache.get(uri);
		if (writer != null && !canWrite(writer)) {
			/* If we can't write, remove from the cache*/
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
}

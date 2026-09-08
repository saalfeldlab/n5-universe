package org.janelia.saalfeldlab.n5.universe.factory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.factory.options.FormatOptions;
import org.janelia.saalfeldlab.n5.universe.factory.options.N5FactoryOptions;

/**
 * Factory for various N5 readers and writers. Implementation-specific
 * parameters can be provided to the factory instance and will be used when such
 * implementations are generated and ignored otherwise. Reasonable defaults are
 * provided.
 *
 * @author Stephan Saalfeld
 * @author John Bogovic
 * @author Igor Pisarev
 */
public class N5Factory {

	private static final N5Factory FACTORY = new N5Factory();

	/**
	 * Creates an N5 writer for the specified container URI with default N5Factory configuration.
	 *
	 * @param containerUri location of the N5 container
	 * @return an N5Writer instance for the given containerURI
	 */
	public static N5Writer createWriter(String containerUri) {

		return FACTORY.openWriter(containerUri);
	}

	/**
	 * Creates an N5Reader at containerURI with default N5Factory configuration.
	 *
	 * @param containerUri location of the N5 container
	 * @return an N5Reader instance for the given containerURI
	 */
	public static N5Reader createReader(String containerUri) {

		return FACTORY.openReader(containerUri);
	}

	private N5FactoryOptions options = new N5FactoryOptions();


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Options                                                              │
	// └──────────────────────────────────────────────────────────────────────┘

	public N5FactoryOptions getOptions() {
        return options;
    }

    public N5Factory setOptions(N5FactoryOptions options) {
        this.options = options;
		return this;
    }

	public N5Factory options( Consumer<N5FactoryOptions> configureOptions) {
		configureOptions.accept(options);
		return this;
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Create KeyValueRoot                                                  │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Return the {@link KeyValueRoot} appropriate for the given {@link
	 * URI}, or {@code null} if no backend can handle it. The backend is
	 * resolved from the URI scheme (and potentially probing the content
	 * if that is inconclusive).
	 *
	 * @param uri to create a {@link KeyValueRoot} from
	 * @param readOnly whether the root will only be read from
	 * @return the {@link KeyValueRoot}, or null if none are valid
	 */
	@Nullable
	public KeyValueRoot getKeyValueRoot(final URI uri, final boolean readOnly) {
		return KeyValueRoots.create(uri, readOnly, options.backend());
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Create Reader                                                        │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Open an {@link N5Reader} at the given URL, with best attempts to infer
	 * the appropriate storage backend and {@link StorageFormat}.
	 *
	 * @param uri the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final String uri) {
		return openReader((StorageFormat) null, uri);
	}

	/**
	 * Open an {@link N5Reader} at the given URL, with best attempts to infer
	 * the appropriate storage backend.
	 *
	 * @param format of the data store this N5Reader is accessing
	 * @param uri the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final StorageFormat format, final String uri) {
		return openReader(format, parseUriFromString(uri));
	}

	/**
	 * Open an {@link N5Reader} at the given URL, with best attempts to infer
	 * the appropriate storage backend.
	 *
	 * @param format of the data store this N5Reader is accessing
	 * @param uri the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(StorageFormat format, final URI uri) {

		final URI rootUri = stripStorageSchemePrefix(uri);
		final KeyValueRoot root = getKeyValueRoot(rootUri, true);
		if (root == null)
			throw new N5Exception("Cannot get KeyValueRoot at " + rootUri);

		return openReaderHelper(format, root, uri);
	}

	/**
	 * Create an N5Reader at {@code root} requiring the specified {@link StorageFormat}.
	 * <p>
	 * NB: If {@code format==null}, all formats are attempted, starting with the
	 * {@link FormatOptions#preferredStorageFormat(StorageFormat) preferredStorageFormat}.
	 *
	 * @param format of the underlying data store this N5Reader is over
	 * @param root root of the container
	 * @return the N5Reader
	 */
	public N5Reader openReader(@Nullable final StorageFormat format, final KeyValueRoot root) {
		return openReaderHelper(format, root, root.uri());
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Create Writer                                                        │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Open an {@link N5Writer} at the given URL, with best attempts to infer
	 * the appropriate storage backend and {@link StorageFormat}.
	 *
	 * @param uri the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final String uri) {
		return openWriter((StorageFormat) null, uri);
	}

	/**
	 * Open an {@link N5Writer} at the given URL, with best attempts to infer
	 * the appropriate storage backend.
	 *
	 * @param format of the data store for this N5Writer
	 * @param uri the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final StorageFormat format, final String uri) {
		return openWriter(format, parseUriFromString(uri));
	}

	/**
	 * Open an {@link N5Writer} at the given URL, with best attempts to infer
	 * the appropriate storage backend.
	 *
	 * @param format of the data store for this N5Writer
	 * @param uri the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final StorageFormat format, final URI uri) {

		final URI rootUri = stripStorageSchemePrefix(uri);
		final KeyValueRoot root = getKeyValueRoot(rootUri, false);
		if (root == null)
			throw new N5Exception("Cannot get KeyValueRoot at " + rootUri);

		return openWriterHelper(format, root, uri);
	}

	/**
	 * Create an N5Writer at {@code root} requiring the specified {@link StorageFormat}.
	 * <p>
	 * NB: If {@code format==null}, all formats are attempted, starting with the
	 * {@link FormatOptions#preferredStorageFormat(StorageFormat) preferredStorageFormat}.
	 *
	 * @param format of the underlying data store this N5Writer is over
	 * @param root root of the container
	 * @return the N5Writer
	 */
	public N5Writer openWriter(@Nullable final StorageFormat format, final KeyValueRoot root) {

		return openWriterHelper(format, root, root.uri());
	}

	/**
	 * Get a new {@link N5Writer} IFF the container already exists. If one does not already exist,
	 * a new one will NOT be created.
	 * This is achieved by first opening a reader at the location, and only then opening
	 * a writer if the reader successfully opens.
	 *
	 * @param storage the storage format, or null
	 * @param root root of the container
	 * @return a writer for the existing container
	 *
	 * @throws N5IOException if the container does not exist, no attempt will be made to create it
	 */
	public N5Writer openExistingWriter(@Nullable final StorageFormat storage, final KeyValueRoot root) throws N5IOException {

		requireContainerExists(() -> openReader(storage, root));
		return openWriter(storage, root);
	}

	/**
	 * Get a new {@link N5Writer} IFF the container already exists. If one does not already exist,
	 * a new one will NOT be created.
	 * This is achieved by first opening a reader at the location, and only then opening
	 * a writer if the reader successfully opens.
	 *
	 * @param uri the container location, optionally prefixed with a storage format
	 * @return a writer for the existing container
	 *
	 * @throws N5IOException if the container does not exist, no attempt will be made to create it
	 */
	public N5Writer openExistingWriter(final String uri) throws N5IOException {

		requireContainerExists(() -> openReader(uri));
		return openWriter(uri);
	}

	/**
	 * Test if a reader can be opened from given supplier
	 *
	 * @throws N5IOException if the container does not exist
	 */
	protected void requireContainerExists(Supplier<N5Reader> getReader) throws N5IOException {
		//noinspection EmptyTryBlock
		try (N5Reader ignored = getReader.get()) {
			/* Just want to verify we can open, then we close to get the writer*/
		} catch (final Exception e) {
			throw new N5IOException("Existing container could not be opened, or does not exist.", e);
		}
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Helpers                                                              │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Open an {@link N5Reader} at {@code root} requiring the specified {@link StorageFormat}.
	 * <p>
	 * If {@code format==null} the format is inferred from the {@code uri} and, if that is not
	 * authoritative, by probing {@code root} (for presence of attributes files, etc.)
	 * <p>
	 * Why the additional {@code uri} argument?
	 * {@code uri} is the {@link KeyValueRoot#uri() uri} of {@code root}, but possibly with an
	 * additional storage-scheme prefix, which is important for format inference.
	 *
	 * @param format format of the container, or {@code null} to infer format
	 * @param root root of the container
	 * @param uri root URI, possibly with an additional storage-scheme prefix
	 * @return the N5Reader
	 */
	// NB: This method is protected so that it can be overridden by
	// N5FactoryWithCache to inject its readerCache. Every openReader()
	// overload funnels through here.
	protected N5Reader openReaderHelper(@Nullable final StorageFormat format, final KeyValueRoot root, final URI uri) {

		return Containers.openReader(root, format, uri, options.format());
	}

	/**
	 * Open an {@link N5Writer} at {@code root} requiring the specified {@link StorageFormat}.
	 * <p>
	 * If {@code format==null} the format is inferred from the {@code uri} and, if that is not
	 * authoritative, by probing {@code root} (for presence of attributes files, etc.)
	 * <p>
	 * Why the additional {@code uri} argument?
	 * {@code uri} is the {@link KeyValueRoot#uri() uri} of {@code root}, but possibly with an
	 * additional storage-scheme prefix, which is important for format inference.
	 *
	 * @param format format of the container, or {@code null} to infer format
	 * @param root root of the container
	 * @param uri root URI, possibly with an additional storage-scheme prefix
	 * @return the N5Writer
	 */
	// NB: This method is protected so that it can be overridden by
	// N5FactoryWithCache to inject its writerCache. Every openWriter()
	// overload funnels through here.
	protected N5Writer openWriterHelper(@Nullable final StorageFormat format, final KeyValueRoot root, final URI uri) {

		return Containers.openWriter(root, format, uri, options.format());
	}

	/**
	 * Strip storage-scheme prefix from {@code uri}, if there is one.
	 * <p>
	 * For example:<br/>
	 * {@code zarr3://s3://my-bucket/path} becomes {@code s3://my-bucket/path}<br/>
	 * {@code zarr2:https://example.org/data} becomes {@code https://example.org/data}
	 */
	private static URI stripStorageSchemePrefix(final URI uri) {
		return InferredStorageFormat.of(uri.toString()).uri();
	}

	static URI parseUriFromString(String uri) {
		try {
			return URI.create(uri);
		} catch (final Throwable ignore) {}

		try {
			return Paths.get(uri).toUri();
		} catch (final Throwable ignore) {}

		try {
			return N5URI.encodeAsUri(uri);
		} catch (final URISyntaxException e) {
			throw new N5Exception(e);
		}
	}

}
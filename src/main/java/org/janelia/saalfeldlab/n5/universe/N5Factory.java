package org.janelia.saalfeldlab.n5.universe;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.factory.InferredStorageFormat;
import org.janelia.saalfeldlab.n5.universe.factory.KeyValueRoots;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.factory.options.N5FactoryOptions;
import org.janelia.saalfeldlab.n5.universe.factory.options.Zarr2Options;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Factory for various N5 readers and writers.
 * <p>
 * This class is kept for compatibility, and forwards to
 * {@link org.janelia.saalfeldlab.n5.universe.factory.N5Factory}, which is where the
 * factory now lives. What this class adds are the deprecated methods, which
 * the new factory does not carry.
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

	private final org.janelia.saalfeldlab.n5.universe.factory.N5Factory factory;

	public N5Factory() {

		this(new org.janelia.saalfeldlab.n5.universe.factory.N5Factory());
	}

	protected N5Factory(final org.janelia.saalfeldlab.n5.universe.factory.N5Factory factory) {

		this.factory = factory;
	}

	/**
	 * @return the {@link org.janelia.saalfeldlab.n5.universe.factory.N5Factory} this forwards to
	 */
	public org.janelia.saalfeldlab.n5.universe.factory.N5Factory getFactory() {

		return factory;
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Options                                                              │
	// └──────────────────────────────────────────────────────────────────────┘

	public N5FactoryOptions getOptions() {

		return factory.getOptions();
	}

	public N5Factory setOptions(N5FactoryOptions options) {

		factory.setOptions(options);
		return this;
	}

	public N5Factory options(Consumer<N5FactoryOptions> configureOptions) {

		factory.options(configureOptions);
		return this;
	}

	/**
	 * @deprecated configure with {@link N5Factory#getOptions()} and {@link N5FactoryOptions#hdf5(Consumer)}
	 */
	@Deprecated
	public N5Factory hdf5DefaultBlockSize(final int... blockSize) {

		getOptions().format().hdf5().defaultBlockSize(blockSize);
		return this;
	}

	/**
	 * @deprecated configure with {@link N5Factory#getOptions()} and {@link N5FactoryOptions#hdf5(Consumer)}
	 */
	@Deprecated
	public N5Factory hdf5OverrideBlockSize(final boolean override) {

		getOptions().format().hdf5().overrideBlockSize(override);
		return this;
	}

	/**
	 * @deprecated configure with {@link N5Factory#getOptions()} and {@link N5FactoryOptions#gsonBuilder(GsonBuilder)}
	 */
	@Deprecated
	public N5Factory gsonBuilder(final GsonBuilder gsonBuilder) {

		getOptions().gsonBuilder(gsonBuilder);
		return this;
	}

	/**
	 * @deprecated configure with {@link N5Factory#getOptions()} and {@link N5FactoryOptions#cacheAttributes(boolean)}
	 */
	@Deprecated
	public N5Factory cacheAttributes(final boolean cacheAttributes) {

		getOptions().cacheAttributes(cacheAttributes);
		return this;
	}

	/**
	 * Deprecated method to set dimensions separator for zarr 2 writers.
	 *
	 * @param separator to use for Zarr2 dimension separator
	 * @deprecated configure with {@link N5Factory#getOptions()}, {@link N5FactoryOptions#zarr2(Consumer)}, {@link Zarr2Options#dimensionSeparator(String)}
	 */
	@Deprecated
	public N5Factory zarrDimensionSeparator(final String separator) {

		getOptions().format().zarr2().dimensionSeparator(separator);
		return this;
	}

	/**
	 * Value to set for the [mapN5DatasetAttributes] parameter of ZarrKeyValueReader.
	 * NOTE: this is only used for Zarr2 readers, not Zarr3
	 *
	 * @param mapAttributes see {@link ZarrKeyValueReader#getAttributes(String)}
	 * @deprecated configure with {@link N5Factory#getOptions()}, {@link N5FactoryOptions#zarr2(Consumer)}, {@link Zarr2Options#mapN5DatasetAttributes(boolean)}
	 */
	@Deprecated
	public N5Factory zarrMapN5Attributes(final boolean mapAttributes) {

		getOptions().format().zarr2().mapN5DatasetAttributes(mapAttributes);
		return this;
	}

	/**
	 * Value to set for the [mergeAttributes] parameter of ZarrKeyValueReader.
	 * NOTE: this is only used for Zarr2 readers, not Zarr3
	 *
	 * @param mergeAttributes see {@link ZarrKeyValueReader#getAttributes(String)}
	 * @deprecated configure with {@link N5Factory#getOptions()}, {@link N5FactoryOptions#zarr2(Consumer)}, {@link Zarr2Options#mergeAttributes(boolean)}
	 */
	@Deprecated
	public N5Factory zarrMergeAttributes(final boolean mergeAttributes) {

		getOptions().format().zarr2().mergeAttributes(mergeAttributes);
		return this;
	}

	/**
	 * When N5Factory cannot determine the StorageFormat from the URI, it will attempt to
	 * try and create a reader/writer with all known StorageFormat values.
	 * The order of th StorageFormat will dictate which will formats will be attempted first.
	 * Some URI's may be valid for multiple StorageFormat types. (e.g. N5 and Zarr Writer
	 * at a new location). This format will be tried first if specified.
	 *
	 * @param format the storage format to try first.
	 * @return this N5Factory
	 */
	public N5Factory preferredStorageFormat(final StorageFormat format) {

		getOptions().format().preferredStorageFormat(format);
		return this;
	}

	/**
	 * This factory will use the builderConfig to configure the {@link S3ClientBuilder},
	 * and thereby the {@link S3Client}.
	 *
	 * @return this N5Factory
	 */
	public N5Factory s3Configuration(final Consumer<S3ClientBuilder> builderConfig) {

		getOptions().backend().s3().builderConfig(builderConfig);
		return this;
	}

	public N5Factory googleCloudProjectId(final String projectId) {

		getOptions().backend().googleCloud().projectId(projectId);
		return this;
	}

	/**
	 * This factory will use the gcsBuilderConfig to configure the Google Cloude Store {@link StorageOptions.Builder},
	 * and thereby the Google Cloud {@link Storage} client.
	 *
	 * @return this N5Factory
	 */
	public N5Factory googleCloudConfiguration(final Consumer<StorageOptions.Builder> builderConfig) {

		getOptions().backend().googleCloud().builderConfig(builderConfig);
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

		return factory.getKeyValueRoot(uri, readOnly);
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Create Reader                                                        │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Open an {@link N5Reader} over an N5 Container.
	 *
	 * @param path path to the n5 root folder
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Reader openFSReader(final String path) {

		return openReader(StorageFormat.N5, path);
	}

	/**
	 * Open an {@link N5Reader} for Zarr.
	 *
	 * @param path path to the zarr directory
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Reader openZarrReader(final String path) {

		return openReader(StorageFormat.ZARR, path);
	}

	/**
	 * Open an {@link N5Reader} for HDF5. Close the reader when you do not need
	 * it any more.
	 *
	 * @param path path to the hdf5 file
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Reader openHDF5Reader(final String path) {

		return openReader(StorageFormat.HDF5, path);
	}

	/**
	 * Open an {@link N5Reader} for Google Cloud.
	 *
	 * @param uri uri to the google cloud object
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(String)} instead
	 */
	@Deprecated
	public N5Reader openGoogleCloudReader(final String uri) {

		final KeyValueRoot root = KeyValueRoots.createGoogleCloud(stripStorageSchemePrefix(uri), true, getOptions().backend());
		return openReader(explicitStorageFormat(uri), root);
	}

	/**
	 * Open an {@link N5Reader} for AWS S3.
	 *
	 * @param uri uri to the amazon s3 object
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(String)} instead
	 */
	@Deprecated
	public N5Reader openAWSS3Reader(final String uri) {

		final KeyValueRoot root = KeyValueRoots.createAmazonS3(stripStorageSchemePrefix(uri), true, getOptions().backend());
		return openReader(explicitStorageFormat(uri), root);
	}

	/**
	 * Open an {@link N5Reader} over a FileSytem.
	 *
	 * @param uri uri to the N5Reader
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(String)} instead
	 */
	@Deprecated
	public N5Reader openFileSystemReader(final String uri) {

		final KeyValueRoot root = KeyValueRoots.createFileSystem(stripStorageSchemePrefix(uri));
		return openReader(explicitStorageFormat(uri), root);
	}

	/**
	 * Open an {@link N5Reader} at the given URL, with best attempts to infer
	 * the appropriate storage backend and {@link StorageFormat}.
	 *
	 * @param uri the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final String uri) {

		return factory.openReader(uri);
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

		return factory.openReader(format, uri);
	}

	/**
	 * Open an {@link N5Reader} at the given URL, with best attempts to infer
	 * the appropriate storage backend.
	 *
	 * @param format of the data store this N5Reader is accessing
	 * @param uri the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final StorageFormat format, final URI uri) {

		return factory.openReader(format, uri);
	}

	/**
	 * Open a N5Reader at the given URI given the specified {@code KeyValueAccessBackend}.
	 *
	 * @param backend key-value access
	 * @param uri location of reader
	 * @return the N5Reader
	 */
	@Deprecated
	public N5Reader openReader(final KeyValueAccessBackend backend, final String uri) {

		return openReader(backend, parseUriFromString(uri));
	}

	/**
	 * Open a N5Reader at the given URI given the specified {@code KeyValueAccessBackend}.
	 *
	 * @param backend key-value access
	 * @param uri location of reader
	 * @return the N5Reader
	 */
	@Deprecated
	public N5Reader openReader(final KeyValueAccessBackend backend, final URI uri) {

		final KeyValueRoot root = createKeyValueRoot(backend, uri, true);
		if (root == null)
			return null;

		try {
			return openReader(explicitStorageFormat(uri.toString()), root);
		} catch (final N5Exception e) {
			throw new N5Exception("Unable to open " + uri + " as N5Reader", e);
		}
	}

	/**
	 * Create an N5Reader at {@code root} requiring the specified {@link StorageFormat}.
	 * <p>
	 * NB: If {@code format==null}, all formats are attempted, starting with the
	 * {@link #preferredStorageFormat(StorageFormat) preferredStorageFormat}.
	 *
	 * @param format of the underlying data store this N5Reader is over
	 * @param root root of the container
	 * @return the N5Reader
	 */
	public N5Reader openReader(@Nullable final StorageFormat format, final KeyValueRoot root) {

		return factory.openReader(format, root);
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Create Writer                                                        │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * Open an {@link N5Writer} for N5 Container.
	 *
	 * @param path path to the n5 directory
	 * @return the N5Writer
	 * @deprecated use {@link N5Factory#openWriter(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Writer openFSWriter(final String path) {

		return openWriter(StorageFormat.N5, path);
	}

	/**
	 * Open an {@link N5Writer} for Zarr.
	 *
	 * @param path path to the zarr directory
	 * @return the N5Writer
	 * @deprecated use {@link N5Factory#openWriter(StorageFormat, URI)}) instead
	 */
	@Deprecated
	public N5Writer openZarrWriter(final String path) {

		return openWriter(StorageFormat.ZARR, path);
	}

	/**
	 * Open an {@link N5Writer} for HDF5. Don't forget to close the writer after
	 * writing to close the file and make it available to other processes.
	 *
	 * @param path path to the hdf5 file
	 * @return the N5HDF5Writer
	 * @deprecated use {@link N5Factory#openWriter(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Writer openHDF5Writer(final String path) {

		return openWriter(StorageFormat.HDF5, path);
	}

	/**
	 * Open an {@link N5Writer} for Google Cloud.
	 *
	 * @param uri uri to the google cloud object
	 * @return the N5GoogleCloudStorageWriter
	 * @deprecated use {@link #openWriter(String)} instead.
	 */
	@Deprecated
	public N5Writer openGoogleCloudWriter(final String uri) {

		final KeyValueRoot root = KeyValueRoots.createGoogleCloud(stripStorageSchemePrefix(uri), false, getOptions().backend());
		return openWriter(explicitStorageFormat(uri), root);
	}

	/**
	 * Open an {@link N5Writer} for AWS S3.
	 *
	 * @param uri uri to the s3 object
	 * @return the N5Writer
	 * @deprecated use {@link #openWriter(String)} instead.
	 */
	@Deprecated
	public N5Writer openAWSS3Writer(final String uri) {

		final KeyValueRoot root = KeyValueRoots.createAmazonS3(stripStorageSchemePrefix(uri), false, getOptions().backend());
		return openWriter(explicitStorageFormat(uri), root);
	}

	/**
	 * Open an {@link N5Writer} at the given URL, with best attempts to infer
	 * the appropriate storage backend and {@link StorageFormat}.
	 *
	 * @param uri the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final String uri) {

		return factory.openWriter(uri);
	}

	/**
	 * Create or Open an N5Writer as the given uri and backend.
	 *
	 * @param backend key-value access
	 * @param uri location of writer
	 * @return the N5Writer
	 */
	@Deprecated
	public N5Writer openWriter(final KeyValueAccessBackend backend, final URI uri) {

		final KeyValueRoot root = createKeyValueRoot(backend, uri, false);
		if (root == null)
			return null;

		try {
			return openWriter(explicitStorageFormat(uri.toString()), root);
		} catch (final N5Exception e) {
			throw new N5Exception("Unable to open " + uri + " as N5Writer", e);
		}
	}

	/**
	 * Create or Open an N5Writer as the given uri and backend.
	 *
	 * @param backend key-value access
	 * @param uri location of writer
	 * @return the N5Writer
	 */
	@Deprecated
	public N5Writer openWriter(final KeyValueAccessBackend backend, final String uri) {

		return openWriter(backend, parseUriFromString(uri));
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

		return factory.openWriter(format, uri);
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

		return factory.openWriter(format, uri);
	}

	/**
	 * Create an N5Writer at {@code root} requiring the specified {@link StorageFormat}.
	 * <p>
	 * NB: If {@code format==null}, all formats are attempted, starting with the
	 * {@link #preferredStorageFormat(StorageFormat) preferredStorageFormat}.
	 *
	 * @param format of the underlying data store this N5Writer is over
	 * @param root root of the container
	 * @return the N5Writer
	 */
	public N5Writer openWriter(@Nullable final StorageFormat format, final KeyValueRoot root) {

		return factory.openWriter(format, root);
	}

	/**
	 * Get a new {@link N5Writer} IFF the container already exists. If one does not already exist,
	 * a new one will NOT be created.
	 *
	 * @param storage the storage format, or null
	 * @param root root of the container
	 * @return a writer for the existing container
	 *
	 * @throws N5IOException if the container does not exist, no attempt will be made to create it
	 */
	public N5Writer openExistingWriter(@Nullable final StorageFormat storage, final KeyValueRoot root) throws N5IOException {

		return factory.openExistingWriter(storage, root);
	}

	/**
	 * Get a new {@link N5Writer} IFF the container already exists. If one does not already exist,
	 * a new one will NOT be created.
	 *
	 * @param uri the container location, optionally prefixed with a storage format
	 * @return a writer for the existing container
	 *
	 * @throws N5IOException if the container does not exist, no attempt will be made to create it
	 */
	public N5Writer openExistingWriter(final String uri) throws N5IOException {

		return factory.openExistingWriter(uri);
	}


	// ┌──────────────────────────────────────────────────────────────────────┐
	// │ Helpers                                                              │
	// └──────────────────────────────────────────────────────────────────────┘

	/**
	 * The root for {@code uri} on the given {@code backend}, whatever the URI
	 * looks like; {@code null} for an unknown backend.
	 */
	@Nullable
	private KeyValueRoot createKeyValueRoot(final KeyValueAccessBackend backend, final URI uri, final boolean readOnly) {

		final URI rootUri = stripStorageSchemePrefix(uri.toString());
		switch (backend) {
		case GOOGLE_CLOUD:
			return KeyValueRoots.createGoogleCloud(rootUri, readOnly, getOptions().backend());
		case AWS:
			return KeyValueRoots.createAmazonS3(rootUri, readOnly, getOptions().backend());
		case HTTP:
			return KeyValueRoots.createHttp(rootUri, getOptions().backend());
		case FILE:
			return KeyValueRoots.createFileSystem(rootUri);
		default:
			return null;
		}
	}

	/**
	 * Return explicit {@code StorageFormat} if {@code uri} has a storage-scheme prefix, {@code null} otherwise.
	 */
	@Nullable
	private static StorageFormat explicitStorageFormat(final String uri) {

		final InferredStorageFormat inferred = InferredStorageFormat.of(uri);
		return inferred.isExplicit() ? inferred.format() : null;
	}

	/**
	 * Strip storage-scheme prefix from {@code uri}, if there is one.
	 */
	private static URI stripStorageSchemePrefix(final String uri) {

		return InferredStorageFormat.of(uri).uri();
	}

	// NB: A copy of the package-private factory.N5Factory.parseUriFromString,
	// needed only by the deprecated KeyValueAccessBackend overloads.
	private static URI parseUriFromString(String uri) {

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

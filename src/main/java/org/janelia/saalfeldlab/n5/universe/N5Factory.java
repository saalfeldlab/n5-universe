/**
 * Copyright (c) 2017-2021, Saalfeld lab, HHMI Janelia
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.universe;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang3.function.TriFunction;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudUtils;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.hdf5.HDF5Utils;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * Factory for various N5 readers and writers. Implementation specific
 * parameters can be provided to the factory instance and will be used when such
 * implementations are generated and ignored otherwise. Reasonable defaults are
 * provided.
 *
 * @author Stephan Saalfeld
 * @author John Bogovic
 * @author Igor Pisarev
 */
public class N5Factory implements Serializable {

	private static final N5Factory FACTORY = new N5Factory();

	private static final long serialVersionUID = -6823715427289454617L;
	private final static Pattern HTTPS_SCHEME = Pattern.compile("http(s)?", Pattern.CASE_INSENSITIVE);
	private final static Pattern FILE_SCHEME = Pattern.compile("file", Pattern.CASE_INSENSITIVE);
	private int[] hdf5DefaultBlockSize = {64, 64, 64, 1, 1};
	private boolean hdf5OverrideBlockSize = false;
	private GsonBuilder gsonBuilder = new GsonBuilder();
	private boolean cacheAttributes = true;
	private String zarrDimensionSeparator = ".";
	private boolean zarrMapN5DatasetAttributes = true;
	private boolean zarrMergeAttributes = true;
	private String googleCloudProjectId = null;
	private String s3Region = null;
	private AWSCredentials s3Credentials = null;
	private ClientConfiguration s3ClientConfiguration = null;
	private boolean s3Anonymous = true;
	private String s3Endpoint;

	private static GoogleCloudStorageKeyValueAccess newGoogleCloudKeyValueAccess(final URI uri, final N5Factory factory) {

		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(uri);
		return new GoogleCloudStorageKeyValueAccess(factory.createGoogleCloudStorage(), googleCloudUri, true);
	}

	private static AmazonS3KeyValueAccess newAmazonS3KeyValueAccess(final URI uri, final N5Factory factory) {

		final String uriString = uri.toString();
		final AmazonS3 s3 = factory.createS3(uriString);

		return new AmazonS3KeyValueAccess(s3, uri, true);
	}

	private static FileSystemKeyValueAccess newFileSystemKeyValueAccess(final URI uri, final N5Factory factory) {

		return new FileSystemKeyValueAccess(FileSystems.getDefault());
	}

	public N5Factory hdf5DefaultBlockSize(final int... blockSize) {

		hdf5DefaultBlockSize = blockSize;
		return this;
	}

	public N5Factory hdf5OverrideBlockSize(final boolean override) {

		hdf5OverrideBlockSize = override;
		return this;
	}

	public N5Factory gsonBuilder(final GsonBuilder gsonBuilder) {

		this.gsonBuilder = gsonBuilder;
		return this;
	}

	public N5Factory cacheAttributes(final boolean cacheAttributes) {

		this.cacheAttributes = cacheAttributes;
		return this;
	}

	public N5Factory zarrDimensionSeparator(final String separator) {

		zarrDimensionSeparator = separator;
		return this;
	}

	public N5Factory zarrMapN5Attributes(final boolean mapAttributes) {

		zarrMapN5DatasetAttributes = mapAttributes;
		return this;
	}

	public N5Factory zarrMergeAttributes(final boolean mergeAttributes) {

		zarrMergeAttributes = mergeAttributes;
		return this;
	}

	public N5Factory googleCloudProjectId(final String projectId) {

		googleCloudProjectId = projectId;
		return this;
	}

	/**
	 * This factory will use the {@link DefaultAWSCredentialsProviderChain} to
	 * find s3 credentials.
	 *
	 * @return this N5Factory
	 */
	public N5Factory s3UseCredentials() {

		s3Anonymous = false;
		return this;
	}

	public N5Factory s3UseCredentials(final AWSCredentials credentials) {

		this.s3Credentials = credentials;
		return this;
	}

	public N5Factory s3ClientConfiguration(final ClientConfiguration clientConfiguration) {

		this.s3ClientConfiguration = clientConfiguration;
		return this;
	}

	@Deprecated
	public N5Factory s3RetryWithCredentials() {

		return this;
	}

	public N5Factory s3Endpoint(final String s3Endpoint) {

		this.s3Endpoint = s3Endpoint;
		return this;
	}

	public N5Factory s3Region(final String s3Region) {

		this.s3Region = s3Region;
		return this;
	}

	AmazonS3 createS3(final String uri) {

		try {
			return AmazonS3Utils.createS3(uri, s3Endpoint, AmazonS3Utils.getS3Credentials(s3Credentials, s3Anonymous), s3ClientConfiguration, s3Region);
		} catch (final Throwable e) {
			throw new N5Exception("Could not create s3 client from uri: " + uri, e);
		}
	}

	Storage createGoogleCloudStorage() {

		return GoogleCloudUtils.createGoogleCloudStorage(googleCloudProjectId);
	}

	/**
	 * Test the provided {@link URI} to and return the appropriate {@link KeyValueAccess}.
	 * If no appropriate {@link KeyValueAccess} is found, may be null
	 *
	 * @param uri to create a {@link KeyValueAccess} from.
	 * @return the {@link KeyValueAccess} and container path, or null if none are valid
	 */
	@Nullable
	KeyValueAccess getKeyValueAccess(final URI uri) {

		/*NOTE: The order of these tests is very important, as the predicates for each
		 * backend take into account reasonable defaults when possible.
		 * Here we test from most to least restrictive.
		 * See the Javadoc for more details.  */
		for (final KeyValueAccessBackend backend : KeyValueAccessBackend.values()) {
			final KeyValueAccess kva = backend.apply(uri, this);
			if (kva != null)
				return kva;
		}
		return null;
	}

	/**
	 * Open an {@link N5Reader} over an N5 Container.
	 * <p>
	 * NOTE: The name seems to imply that this will open any N5Reader, over a
	 * {@link FileSystemKeyValueAccess} however that is misleading. Instead
	 * this will open any N5Container that is a valid {@link  StorageFormat#N5}.
	 * This is partially why it is deprecated, as well as the redundant
	 * implementation with {@link N5Factory#openReader(StorageFormat, URI)}.
	 *
	 * @param path path to the n5 root folder
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Reader openFSReader(final String path) {

		return openN5ContainerWithStorageFormat(StorageFormat.N5, path, this::openReader);
	}

	/**
	 * Open an {@link N5Reader} for Zarr.
	 * <p>
	 * For more options of the Zarr backend study the {@link N5ZarrReader}
	 * constructors.}
	 *
	 * @param path path to the zarr directory
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Reader openZarrReader(final String path) {

		return openN5ContainerWithStorageFormat(StorageFormat.ZARR, path, this::openReader);
	}

	/**
	 * Open an {@link N5Reader} for HDF5. Close the reader when you do not need
	 * it any more.
	 * <p>
	 * For more options of the HDF5 backend study the {@link N5HDF5Reader}
	 * constructors.
	 *
	 * @param path path to the hdf5 file
	 * @return the N5Reader
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Reader openHDF5Reader(final String path) {

		return openN5ContainerWithStorageFormat(
				StorageFormat.HDF5,
				path,
				(format, uri) -> openReader(format, null, uri.getPath())
		);
	}

	/**
	 * Open an {@link N5Reader} for Google Cloud.
	 *
	 * @param uri uri to the google cloud object
	 * @return the N5Reader
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Reader openGoogleCloudReader(final String uri) throws URISyntaxException {

		return openN5ContainerWithBackend(KeyValueAccessBackend.GOOGLE_CLOUD, uri, this::openReader);
	}

	/**
	 * Open an {@link N5Reader} for AWS S3.
	 *
	 * @param uri uri to the amazon s3 object
	 * @return the N5Reader
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Reader openAWSS3Reader(final String uri) throws URISyntaxException {

		return openN5ContainerWithBackend(KeyValueAccessBackend.AWS, uri, this::openReader);
	}

	/**
	 * Open an {@link N5Reader} over a FileSytem.
	 *
	 * @param uri uri to the N5Reader
	 * @return the N5Reader
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Reader openFileSystemReader(final String uri) throws URISyntaxException {

		return openN5ContainerWithBackend(KeyValueAccessBackend.FILE, uri, this::openReader);
	}

	public N5Reader openReader(final StorageFormat format, final String uri) {

		return openN5Container(format, parseUriFromString(uri), this::openReader);
	}

	public N5Reader openReader(final StorageFormat format, final URI uri) {

		return openN5Container(format, uri, this::openReader);
	}

	/**
	 * Open an {@link N5Reader} based on some educated guessing from the url.
	 *
	 * @param uri the location of the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final String uri) {

		return openN5Container(uri, this::openReader, this::openReader);
	}

	private N5Reader openReader(@Nullable final StorageFormat storage, @Nullable final KeyValueAccess access, String containerPath) {

		if (storage == null) {
			for (final StorageFormat format : StorageFormat.values()) {
				try {
					return openReader(format, access, containerPath);
				} catch (final Throwable e) {
				}
			}
			throw new N5Exception("Unable to open " + containerPath + " as N5Reader");

		} else {

			switch (storage) {
			case N5:
				return new N5KeyValueReader(access, containerPath, gsonBuilder, cacheAttributes);
			case ZARR:
				return new ZarrKeyValueReader(access, containerPath, gsonBuilder, zarrMapN5DatasetAttributes, zarrMergeAttributes, cacheAttributes);
			case HDF5:
				return new N5HDF5Reader(containerPath, hdf5OverrideBlockSize, gsonBuilder, hdf5DefaultBlockSize);
			}
			return null;
		}
	}

	/**
	 * Open an {@link N5Writer} for N5 Container.
	 * <p>
	 * NOTE: The name seems to imply that this will open any N5Writer, over a
	 * {@link FileSystemKeyValueAccess} however that is misleading. Instead
	 * this will open any N5Container that is a valid {@link  StorageFormat#N5}.
	 * This is partially why it is deprecated, as well as the redundant
	 * implementation with {@link N5Factory#openWriter(StorageFormat, URI)}.
	 *
	 * @param path path to the n5 directory
	 * @return the N5Writer
	 * @deprecated use {@link N5Factory#openWriter(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Writer openFSWriter(final String path) {

		return openN5ContainerWithStorageFormat(StorageFormat.N5, path, this::openWriter);
	}

	/**
	 * Open an {@link N5Writer} for Zarr.
	 * <p>
	 * For more options of the Zarr backend study the {@link N5ZarrWriter}
	 * constructors.
	 *
	 * @param path path to the zarr directory
	 * @return the N5Writer
	 * @deprecated use {@link N5Factory#openWriter(StorageFormat, URI)}) instead
	 */
	@Deprecated
	public N5Writer openZarrWriter(final String path) {

		return openN5ContainerWithStorageFormat(StorageFormat.ZARR, path, this::openWriter);
	}

	/**
	 * Open an {@link N5Writer} for HDF5. Don't forget to close the writer after
	 * writing to close the file and make it available to other processes.
	 * <p>
	 * For more options of the HDF5 backend study the {@link N5HDF5Writer}
	 * constructors.
	 *
	 * @param path path to the hdf5 file
	 * @return the N5HDF5Writer
	 * @deprecated use {@link N5Factory#openReader(StorageFormat, URI)} instead
	 */
	@Deprecated
	public N5Writer openHDF5Writer(final String path) {

		return openN5ContainerWithStorageFormat(
				StorageFormat.HDF5,
				path,
				(format, uri) -> openWriter(format, null, uri.getPath())
		);
	}

	/**
	 * Open an {@link N5Writer} for Google Cloud.
	 *
	 * @param uri uri to the google cloud object
	 * @return the N5GoogleCloudStorageWriter
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Writer openGoogleCloudWriter(final String uri) throws URISyntaxException {

		return openN5ContainerWithBackend(KeyValueAccessBackend.GOOGLE_CLOUD, uri, this::openWriter);
	}

	/**
	 * Open an {@link N5Writer} for AWS S3.
	 *
	 * @param uri uri to the s3 object
	 * @return the N5Writer
	 * @throws URISyntaxException if the URI is malformed
	 */
	public N5Writer openAWSS3Writer(final String uri) throws URISyntaxException {

		return openN5ContainerWithBackend(KeyValueAccessBackend.AWS, uri, this::openWriter);
	}

	public N5Writer openWriter(final StorageFormat format, final String uri) {


		return openN5Container(format, parseUriFromString(uri), this::openWriter);

	}

	public N5Writer openWriter(final StorageFormat format, final URI uri) {

		return openN5Container(format, uri, this::openWriter);
	}

	/**
	 * Open an {@link N5Writer} based on some educated guessing from the uri.
	 *
	 * @param uri the location of the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final String uri) {

		return openN5Container(uri, this::openWriter, this::openWriter);
	}

	private N5Writer openWriter(@Nullable final StorageFormat storage, @Nullable final KeyValueAccess access, final String containerPath) {

		if (storage == null) {
			for (final StorageFormat format : StorageFormat.values()) {
				try {
					return openWriter(format, access, containerPath);
				} catch (final Throwable ignored) {
				}
			}
			throw new N5Exception("Unable to open " + containerPath + " as N5Writer");

		} else {

			switch (storage) {
			case ZARR:
				return new ZarrKeyValueWriter(access, containerPath, gsonBuilder, zarrMapN5DatasetAttributes, zarrMergeAttributes, zarrDimensionSeparator, cacheAttributes);
			case N5:
				return new N5KeyValueWriter(access, containerPath, gsonBuilder, cacheAttributes);
			case HDF5:
				return new N5HDF5Writer(containerPath, hdf5OverrideBlockSize, gsonBuilder, hdf5DefaultBlockSize);
			}
		}
		return null;
	}

	private <T extends N5Reader> T openN5ContainerWithStorageFormat(
			final StorageFormat format,
			final String uri,
			final BiFunction<StorageFormat, URI, T> openWithFormat
	) {

		try {
			final URI asUri = StorageFormat.parseUri(uri).getB();
			return openWithFormat.apply(format, asUri);
		} catch (final URISyntaxException e) {
			throw new N5Exception("Cannot create N5 Container (" + format + ") at " + uri, e);
		}
	}

	private <T extends N5Reader> T openN5ContainerWithBackend(
			final KeyValueAccessBackend backend,
			final String containerUri,
			final TriFunction<StorageFormat, KeyValueAccess, String, T> openWithBackend
	) throws URISyntaxException {

		final Pair<StorageFormat, URI> formatAndUri = StorageFormat.parseUri(containerUri);
		final URI uri = formatAndUri.getB();
		final KeyValueAccess kva = backend.apply(uri, this);
		return openWithBackend.apply(formatAndUri.getA(), kva, uri.toString());
	}

	private <T extends N5Reader> T openN5Container(
			final StorageFormat storageFormat,
			final URI uri,
			final TriFunction<StorageFormat, KeyValueAccess, String, T> openWithKva) {

		final KeyValueAccess kva = getKeyValueAccess(uri);
		if (kva == null)
			throw new N5Exception("Cannot get KeyValueAccess at " + uri);
		return openWithKva.apply(storageFormat, kva, uri.toString());
	}

	private <T extends N5Reader> T openN5Container(
			final String containerUri,
			final BiFunction<StorageFormat, URI, T> openWithFormat,
			final TriFunction<StorageFormat, KeyValueAccess, String, T> openWithKva) {

		final Pair<StorageFormat, URI> storageAndUri;
		try {
			storageAndUri = StorageFormat.parseUri(containerUri);
		} catch (final URISyntaxException e) {
			throw new N5Exception("Unable to open " + containerUri + " as N5 Container", e);
		}
		final StorageFormat format = storageAndUri.getA();
		final URI uri = storageAndUri.getB();
		if (format != null)
			return openWithFormat.apply(format, uri);
		else
			return openN5Container(null, uri, openWithKva);
	}

	/**
	 * Enum to discover and provide {@link KeyValueAccess} for {@link N5Reader}s and {@link N5Writer}s.
	 * IMPORTANT: If ever new {@link KeyValueAccess} backends are adding, they MUST be re-ordered
	 * such that the earliest predicates are the most restrictive, and the later predicates
	 * are the least restrictive. This ensures that when iterating through the values of
	 * {@link KeyValueAccessBackend} you can test them in order, and stop at the first
	 * {@link KeyValueAccess} that is generated.
	 */
	enum KeyValueAccessBackend implements Predicate<URI>, BiFunction<URI, N5Factory, KeyValueAccess> {
		GOOGLE_CLOUD(uri -> {
			final String scheme = uri.getScheme();
			final boolean hasScheme = scheme != null;
			return hasScheme && GoogleCloudUtils.GS_SCHEME.asPredicate().test(scheme)
					|| hasScheme && HTTPS_SCHEME.asPredicate().test(scheme)
					&& uri.getHost() != null && GoogleCloudUtils.GS_HOST.asPredicate().test(uri.getHost());
		}, N5Factory::newGoogleCloudKeyValueAccess),
		AWS(uri -> {
			final String scheme = uri.getScheme();
			final boolean hasScheme = scheme != null;
			return hasScheme && AmazonS3Utils.S3_SCHEME.asPredicate().test(scheme)
					|| uri.getHost() != null && hasScheme && HTTPS_SCHEME.asPredicate().test(scheme);
		}, N5Factory::newAmazonS3KeyValueAccess),
		FILE(uri -> {
			final String scheme = uri.getScheme();
			final boolean hasScheme = scheme != null;
			return !hasScheme || FILE_SCHEME.asPredicate().test(scheme);
		}, N5Factory::newFileSystemKeyValueAccess);

		private final Predicate<URI> backendTest;
		private final BiFunction<URI, N5Factory, KeyValueAccess> backendGenerator;

		KeyValueAccessBackend(Predicate<URI> test, BiFunction<URI, N5Factory, KeyValueAccess> generator) {

			backendTest = test;
			backendGenerator = generator;
		}

		@Override public KeyValueAccess apply(final URI uri, final N5Factory factory) {

			if (test(uri))
				return backendGenerator.apply(uri, factory);
			return null;
		}

		@Override public boolean test(URI uri) {

			return backendTest.test(uri);
		}
	}

	public enum StorageFormat {
		ZARR(Pattern.compile("zarr", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.zarr$", Pattern.CASE_INSENSITIVE).matcher(new File(uri.getPath()).toString()).find()),
		ZARR3(Pattern.compile("zarr3", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.zarr3$", Pattern.CASE_INSENSITIVE).matcher(new File(uri.getPath()).toString()).find()),
		N5(Pattern.compile("n5", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.n5$", Pattern.CASE_INSENSITIVE).matcher(new File(uri.getPath()).toString()).find()),
		HDF5(Pattern.compile("h(df)?5", Pattern.CASE_INSENSITIVE), uri -> {
			final boolean hasHdf5Extension = Pattern.compile("\\.h(df)?5$", Pattern.CASE_INSENSITIVE).matcher(uri.getPath()).find();
			return hasHdf5Extension || HDF5Utils.isHDF5(uri.getPath());
		});

		static final Pattern STORAGE_SCHEME_PATTERN = Pattern.compile("^(\\s*(?<storageScheme>(n5|h(df)?5|zarr)):(//)?)?(?<uri>.*)$", Pattern.CASE_INSENSITIVE);
		private final static String STORAGE_SCHEME_GROUP = "storageScheme";
		private final static String URI_GROUP = "uri";

		final Pattern schemePattern;
		private final Predicate<URI> uriTest;

		StorageFormat(final Pattern schemePattern, final Predicate<URI> test) {

			this.schemePattern = schemePattern;
			this.uriTest = test;
		}

		public static StorageFormat guessStorageFromUri(URI uri, KeyValueAccess kva) {

			// TODO: for Diyi!
			final N5URI n5uri = new N5URI(uri);
			final String absolutePath = uri.getPath(); // not sure if this is correct
			try {
				final String[] listResults = kva.list(absolutePath); // get a list of file at this path given by this 
			} catch (IOException e) { }

			return null;
		}

		public static StorageFormat guessStorageFromUri(URI uri) {

			for (final StorageFormat format : StorageFormat.values()) {
				if (format.uriTest.test(uri))
					return format;
			}
			return null;
		}

		public static Pair<StorageFormat, URI> parseUri(String uri) throws URISyntaxException {

			final Pair<StorageFormat, String> storageFromScheme = getStorageFromNestedScheme(uri);
			final URI asUri = parseUriFromString(storageFromScheme.getB());
			if (storageFromScheme.getA() != null)
				return new ValuePair<>(storageFromScheme.getA(), asUri);
			else
				return new ValuePair<>(guessStorageFromUri(asUri), asUri);

		}

		public static Pair<StorageFormat, String> getStorageFromNestedScheme(String uri) {

			final Matcher storageSchemeMatcher = StorageFormat.STORAGE_SCHEME_PATTERN.matcher(uri);
			storageSchemeMatcher.matches();
			final String storageFormatScheme = storageSchemeMatcher.group(STORAGE_SCHEME_GROUP);
			final String uriGroup = storageSchemeMatcher.group(URI_GROUP);
			if (storageFormatScheme != null) {
				for (final StorageFormat format : StorageFormat.values()) {
					if (format.schemePattern.asPredicate().test(storageFormatScheme))
						return new ValuePair<>(format, uriGroup);
				}
			}
			return new ValuePair<>(null, uriGroup);
		}
	}

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
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueAccess;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;

import javax.annotation.Nullable;

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

	private static final long serialVersionUID = -6823715427289454617L;

	private static byte[] HDF5_SIG = {(byte)137, 72, 68, 70, 13, 10, 26, 10};
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
	private boolean s3Anonymous = true;
	private boolean s3RetryWithCredentials = false;
	private String s3Endpoint;
	private boolean createS3Bucket = false;

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

	public N5Factory s3RetryWithCredentials() {

		s3RetryWithCredentials = true;
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

	private static boolean isHDF5Writer(final String path) {

		if (path.matches("(?i).*\\.(h(df)?5)"))
			return true;
		else
			return false;
	}

	private static boolean isHDF5Reader(final String path) throws N5IOException {

		if (Files.isRegularFile(Paths.get(path))) {
			/* optimistic */
			if (isHDF5Writer(path))
				return true;
			else
				return isHDF5(path);
		}
		return false;
	}

	private static boolean isHDF5(String path) {

		final File f = new File(path);
		if (!f.exists() || !f.isFile())
			return false;

		try (final FileInputStream in = new FileInputStream(f)) {
			final byte[] sig = new byte[8];
			in.read(sig);
			return Arrays.equals(sig, HDF5_SIG);
		} catch (final IOException e) {
			return false;
		}
	}

	AmazonS3 createS3(final String uri) {

		try {
			return AmazonS3Utils.createS3(uri, s3Endpoint, AmazonS3Utils.getS3Credentials(s3Credentials, s3Anonymous), s3Region);
		} catch (final Exception e) {
			throw new N5Exception("Could not create s3 client from uri: " + uri, e);
		}
	}

	/**
	 * Open an {@link N5Reader} for N5 filesystem.
	 *
	 * @param path path to the n5 root folder
	 * @return the N5FsReader
	 */
	public N5FSReader openFSReader(final String path) {

		return new N5FSReader(path, gsonBuilder);
	}

	/**
	 * Open an {@link N5Reader} for Zarr.
	 * <p>
	 * For more options of the Zarr backend study the {@link N5ZarrReader}
	 * constructors.}
	 *
	 * @param path path to the zarr directory
	 * @return the N5ZarrReader
	 */
	public N5ZarrReader openZarrReader(final String path) {

		return new N5ZarrReader(path, gsonBuilder, zarrMapN5DatasetAttributes, zarrMergeAttributes, cacheAttributes);
	}

	/**
	 * Open an {@link N5Reader} for HDF5. Close the reader when you do not need
	 * it any more.
	 * <p>
	 * For more options of the HDF5 backend study the {@link N5HDF5Reader}
	 * constructors.
	 *
	 * @param path path to the hdf5 file
	 * @return the N5HDF5Reader
	 */
	public N5HDF5Reader openHDF5Reader(final String path) {

		return new N5HDF5Reader(path, hdf5OverrideBlockSize, gsonBuilder, hdf5DefaultBlockSize);
	}

	/**
	 * Open an {@link N5Reader} for Google Cloud.
	 *
	 * @param uri uri to the google cloud object
	 * @return the N5GoogleCloudStorageReader
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Reader openGoogleCloudReader(final String uri) throws URISyntaxException {

		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(N5URI.encodeAsUri(uri));
		final Storage storage = createGoogleCloudStorage();

		final GoogleCloudStorageKeyValueAccess googleCloudBackend = new GoogleCloudStorageKeyValueAccess(storage,
				googleCloudUri.getBucket(), false);
		if (lastExtension(uri).startsWith(".zarr")) {
			return new ZarrKeyValueReader(googleCloudBackend, googleCloudUri.getKey(), gsonBuilder,
					zarrMapN5DatasetAttributes, zarrMergeAttributes, cacheAttributes);
		} else {
			return new N5KeyValueReader(googleCloudBackend, googleCloudUri.getKey(), gsonBuilder, cacheAttributes);
		}
	}

	private Storage createGoogleCloudStorage() {

		final GoogleCloudStorageClient storageClient = new GoogleCloudStorageClient();
		final Storage storage = storageClient.create();
		return storage;
	}

	/**
	 * Open an {@link N5Reader} for AWS S3.
	 *
	 * @param uri uri to the amazon s3 object
	 * @return the N5Reader
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Reader openAWSS3Reader(final String uri) throws URISyntaxException {

		final AmazonS3 s3 = createS3(N5URI.encodeAsUri(uri).toString());

		// when, if ever do we want to creat a bucket?
		final AmazonS3KeyValueAccess s3kv = new AmazonS3KeyValueAccess(s3, AmazonS3Utils.getS3Bucket(uri), false);
		if (lastExtension(uri).startsWith(".zarr")) {
			return new ZarrKeyValueReader(s3kv, AmazonS3Utils.getS3Key(uri), gsonBuilder, zarrMapN5DatasetAttributes,
					zarrMergeAttributes, cacheAttributes);
		} else {
			return new N5KeyValueReader(s3kv, AmazonS3Utils.getS3Key(uri), gsonBuilder, cacheAttributes);
		}
	}

	/**
	 * Open an {@link N5Writer} for N5 filesystem.
	 *
	 * @param path path to the n5 directory
	 * @return the N5FSWriter
	 */
	public N5FSWriter openFSWriter(final String path) {

		return new N5FSWriter(path, gsonBuilder);
	}

	/**
	 * Open an {@link N5Writer} for Zarr.
	 * <p>
	 * For more options of the Zarr backend study the {@link N5ZarrWriter}
	 * constructors.
	 *
	 * @param path path to the zarr directory
	 * @return the N5ZarrWriter
	 */
	public N5ZarrWriter openZarrWriter(final String path) {

		return new N5ZarrWriter(path, gsonBuilder, zarrDimensionSeparator, zarrMapN5DatasetAttributes, true);
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
	 */
	public N5HDF5Writer openHDF5Writer(final String path) {

		return new N5HDF5Writer(path, hdf5OverrideBlockSize, gsonBuilder, hdf5DefaultBlockSize);
	}

	/**
	 * Open an {@link N5Writer} for Google Cloud.
	 *
	 * @param uri uri to the google cloud object
	 * @return the N5GoogleCloudStorageWriter
	 * @throws URISyntaxException if uri is malformed
	 */
	public N5Writer openGoogleCloudWriter(final String uri) throws URISyntaxException {

		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(N5URI.encodeAsUri(uri));
		final GoogleCloudStorageClient storageClient;
		if (googleCloudProjectId == null) {
			final ResourceManager resourceManager = new GoogleCloudResourceManagerClient().create();
			final Iterator<Project> projectsIterator = resourceManager.list().iterateAll().iterator();
			if (!projectsIterator.hasNext())
				return null;
			storageClient = new GoogleCloudStorageClient(projectsIterator.next().getProjectId());
		} else
			storageClient = new GoogleCloudStorageClient(googleCloudProjectId);

		final Storage storage = storageClient.create();
		final GoogleCloudStorageKeyValueAccess googleCloudBackend = new GoogleCloudStorageKeyValueAccess(storage,
				googleCloudUri.getBucket(), false);
		if (lastExtension(uri).startsWith(".zarr")) {
			return new ZarrKeyValueWriter(googleCloudBackend, googleCloudUri.getKey(), gsonBuilder,
					zarrMapN5DatasetAttributes, zarrMergeAttributes, zarrDimensionSeparator, cacheAttributes);
		} else {
			return new N5KeyValueWriter(googleCloudBackend, googleCloudUri.getKey(), gsonBuilder, cacheAttributes);
		}
	}

	/**
	 * Open an {@link N5Writer} for AWS S3.
	 *
	 * @param uri uri to the s3 object
	 * @return the N5Writer
	 * @throws URISyntaxException if the URI is malformed
	 */
	public N5Writer openAWSS3Writer(final String uri) throws URISyntaxException {

		final AmazonS3 s3 = createS3(N5URI.encodeAsUri(uri).toString());
		// when, if ever do we want to creat a bucket?
		final AmazonS3KeyValueAccess s3kv = new AmazonS3KeyValueAccess(s3, AmazonS3Utils.getS3Bucket(uri), false);
		if (lastExtension(uri).startsWith(".zarr")) {
			return new ZarrKeyValueWriter(s3kv, AmazonS3Utils.getS3Key(uri), gsonBuilder, zarrMapN5DatasetAttributes,
					zarrMergeAttributes, zarrDimensionSeparator, cacheAttributes);
		} else {
			return new N5KeyValueWriter(s3kv, AmazonS3Utils.getS3Key(uri), gsonBuilder, cacheAttributes);
		}
	}

	/**
	 * Open an {@link N5Reader} based on some educated guessing from the url.
	 *
	 * @param uri the location of the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final String uri) {

		try {
			final URI encodedUri = N5URI.encodeAsUri(uri);
			final String scheme = encodedUri.getScheme();
			if (scheme == null)
				;
			else if (scheme.equals("file")) {
				try {
					return openFileBasedN5Reader(Paths.get(encodedUri).toFile().getCanonicalPath());
				} catch (final IOException e) {
					throw new N5Exception.N5IOException(e);
				}
			} else if (scheme.equals("s3"))
				return openAWSS3Reader(uri);
			else if (scheme.equals("gs"))
				return openGoogleCloudReader(uri);
			else if (encodedUri.getHost() != null && scheme.equals("https") || scheme.equals("http")) {
				if (encodedUri.getHost().matches(".*cloud\\.google\\.com")
						|| encodedUri.getHost().matches(".*storage\\.googleapis\\.com"))
					return openGoogleCloudReader(uri);
				else //if (encodedUri.getHost().matches(".*s3.*")) //< This is too fragile for what people in the wild are doing with their S3 instances, for now catch all
					return openAWSS3Reader(uri);
			}
		} catch (final URISyntaxException ignored) {
		}
		//		return null;
		return openFileBasedN5Reader(uri);
	}

	private N5Reader openFileBasedN5Reader(final String url) {

		if (isHDF5Reader(url))
			return openHDF5Reader(url);
		else if (lastExtension(url).startsWith(".zarr"))
			return openZarrReader(url);

		else
			return openFSReader(url);
	}

	/**
	 * Open an {@link N5Writer} based on some educated guessing from the uri.
	 *
	 * @param uri the location of the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final String uri) {

		try {
			final URI encodedUri = N5URI.encodeAsUri(uri);
			final String scheme = encodedUri.getScheme();
			if (scheme == null)
				;
			else if (scheme.equals("file"))
				return openFileBasedN5Writer(encodedUri.getPath());
			else if (scheme.equals("s3"))
				return openAWSS3Writer(uri);
			else if (scheme.equals("gs"))
				return openGoogleCloudWriter(uri);
			else if (encodedUri.getHost() != null && scheme.equals("https") || scheme.equals("http")) {
				if (encodedUri.getHost().matches(".*s3.*"))
					return openAWSS3Writer(uri);
				else if (encodedUri.getHost().matches(".*cloud\\.google\\.com")
						|| encodedUri.getHost().matches(".*storage\\.googleapis\\.com"))
					return openGoogleCloudWriter(uri);
			}
		} catch (final URISyntaxException e) {
		}
		return openFileBasedN5Writer(uri);
	}

	private N5Writer openFileBasedN5Writer(final String url) {

		if (isHDF5Writer(url))
			return openHDF5Writer(url);
		else if (lastExtension(url).startsWith(".zarr"))
			return openZarrWriter(url);
		else
			return openFSWriter(url);
	}

	private static String lastExtension(final String path) {

		final int i = path.lastIndexOf('.');
		if (i >= 0)
			return path.substring(path.lastIndexOf('.'));
		else
			return "";
	}

	public N5Reader getReader(final String uri) {

		try {
			final Pair<StorageFormat, URI> storageAndUri = StorageFormat.parseUri(uri);
			final StorageFormat format = storageAndUri.getA();
			final URI asUri = storageAndUri.getB();
			if (format != null)
				return format.openReader(asUri, this);

			final KeyValueAccess access = KeyValueAccessBackend.getKeyValueAccess(asUri, this);
			if (access == null) {
				throw new N5Exception("Cannot get KeyValueAccess at " + asUri);
			}
			final String containerPath;
			if (access instanceof AmazonS3KeyValueAccess) {
				containerPath = AmazonS3Utils.getS3Key(asUri.toString());
			} else {
				containerPath = asUri.getPath();
			}

			Exception exception = null;
			for (StorageFormat storageFormat : StorageFormat.values()) {
				// all possible attempts at making an hdf5 reader will be done by now
				// and HDF5 does not use a KeyValueAccess
				// revisit this if more backends are added
				if (storageFormat == StorageFormat.HDF5)
					continue;

				try {
					return StorageFormat.getReader(storageFormat, access, containerPath, this);
				} catch (Exception e) {
					exception = e;
				}
			}
			if (exception != null)
				throw new N5Exception("Unable to open " + uri + " as N5 Container", exception);
		} catch (final URISyntaxException ignored) {
		}
		return null;
	}

	public N5Writer getWriter(final String uri) {

		try {

			final Pair<StorageFormat, URI> storageAndUri = StorageFormat.parseUri(uri);
			final StorageFormat format = storageAndUri.getA();
			final URI asUri = storageAndUri.getB();
			if (format != null)
				return format.openWriter(asUri, this);
			else {
				try {
					return openHDF5Writer(uri);
				} catch (Exception ignored) {
				}
			}
			final KeyValueAccess access = KeyValueAccessBackend.getKeyValueAccess(asUri, this);
			if (access == null) {
				throw new N5Exception("Cannot create KeyValueAcccess for URI " + uri);
			}
			final String containerPath;
			if (access instanceof AmazonS3KeyValueAccess) {
				containerPath = AmazonS3Utils.getS3Key(asUri.toString());
			} else {
				containerPath = asUri.getPath();
			}
			try {
				final N5Writer zarrN5Writer = StorageFormat.getWriter(StorageFormat.ZARR, access, containerPath, this);
				if (zarrN5Writer != null)
					return zarrN5Writer;
			} catch (Exception ignored) {
			}
			try {
				final N5Writer n5Writer = StorageFormat.getWriter(StorageFormat.N5, access, containerPath, this);
				if (n5Writer != null)
					return n5Writer;
			} catch (Exception ignored) {
			}
		} catch (final URISyntaxException ignored) {
		}
		return null;
	}

	private static GoogleCloudStorageKeyValueAccess newGoogleCloudKeyValueAccess(final URI uri, final N5Factory factory) {

		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(uri);
		final GoogleCloudStorageClient storageClient = new GoogleCloudStorageClient();
		final Storage storage = storageClient.create();
		return new GoogleCloudStorageKeyValueAccess(storage, googleCloudUri.getBucket(), false);
	}

	private static AmazonS3KeyValueAccess newAmazonS3KeyValueAccess(final URI uri, final N5Factory factory) {

		final String uriString = uri.toString();
		final AmazonS3 s3 = factory.createS3(uriString);

		return new AmazonS3KeyValueAccess(s3, AmazonS3Utils.getS3Bucket(uriString), factory.createS3Bucket);
	}

	private static FileSystemKeyValueAccess newFileSystemKeyValueAccess(final URI uri, final N5Factory factory) {

		return new FileSystemKeyValueAccess(FileSystems.getDefault());
	}

	private final static Pattern GS_SCHEME = Pattern.compile("gs", Pattern.CASE_INSENSITIVE);
	private final static Pattern HTTPS_SCHEME = Pattern.compile("http(s)?", Pattern.CASE_INSENSITIVE);
	private final static Pattern GS_HOST = Pattern.compile("(cloud\\.google|storage\\.googleapis)\\.com", Pattern.CASE_INSENSITIVE);
	private final static Pattern FILE_SCHEME = Pattern.compile("file", Pattern.CASE_INSENSITIVE);

	/**
	 * Enum to discover and provide {@link KeyValueAccess} for {@link N5Reader}s and {@link N5Writer}s.
	 * IMPORTANT: If ever new {@link KeyValueAccess} backends are adding, they MUST be re-ordered
	 * such that the earliest predicates are the most restrictive, and the later predicates
	 * are the least restrictive. This ensures that when iterating through the values of
	 * {@link KeyValueAccessBackend} you can test them in order, and stop at the first
	 * {@link KeyValueAccess} that is generated.
	 */
	enum KeyValueAccessBackend implements Predicate<URI>, BiFunction<URI, N5Factory, KeyValueAccess> {
		//TODO Caleb: Move all of the pattern matching, tests, and magic strings to static fields/methods in the respective KVA
		GOOGLE_CLOUD(uri -> {
			final String scheme = uri.getScheme();
			final boolean hasScheme = scheme != null;
			return hasScheme && GS_SCHEME.asPredicate().test(scheme)
					|| hasScheme && HTTPS_SCHEME.asPredicate().test(scheme)
					&& uri.getHost() != null && GS_HOST.asPredicate().test(uri.getHost());
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
			return !hasScheme || hasScheme && FILE_SCHEME.asPredicate().test(scheme);
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

		/**
		 * Test the provided {@link URI} to and return the appropriate {@link KeyValueAccess}.
		 * If no appropriate {@link KeyValueAccess} is found, may be null
		 *
		 * @param uri to create a {@link KeyValueAccess} from.
		 * @return the {@link KeyValueAccess} or null if none are valid
		 */
		static KeyValueAccess getKeyValueAccess(final URI uri, final N5Factory factory) {

			/*NOTE: The order of these tests is very important, as the predicates for each
			 * backend take into account reasonable defaults when possible.
			 * Here we test from most to least restrictive.
			 * See the Javadoc for more details.  */
			for (KeyValueAccessBackend backend : KeyValueAccessBackend.values()) {
				final KeyValueAccess kva = backend.apply(uri, factory);
				if (kva != null)
					return kva;
			}
			return null;
		}

		@Override public boolean test(URI uri) {

			return backendTest.test(uri);
		}
	}

	enum StorageFormat {
		ZARR(Pattern.compile("zarr", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.zarr$", Pattern.CASE_INSENSITIVE).asPredicate().test(uri.getPath())),
		N5(Pattern.compile("n5", Pattern.CASE_INSENSITIVE), uri -> Pattern.compile("\\.n5$", Pattern.CASE_INSENSITIVE).asPredicate().test(uri.getPath())),
		HDF5(Pattern.compile("h(df)?5", Pattern.CASE_INSENSITIVE), uri -> {
			final boolean hasHdf5Extension = Pattern.compile("\\.h(df)5$", Pattern.CASE_INSENSITIVE).asPredicate().test(uri.getPath());
			return hasHdf5Extension || isHDF5(uri.getPath());
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

		private static StorageFormat guessStorageFromUri(URI uri) {

			for (StorageFormat format : StorageFormat.values()) {
				if (format.uriTest.test(uri))
					return format;
			}
			return null;
		}

		private static Pair<StorageFormat, URI> parseUri(String uri) throws URISyntaxException {

			final Pair<StorageFormat, String> storageFromScheme = getStorageFromNestedScheme(uri);
			final URI asUri = N5URI.encodeAsUri(storageFromScheme.getB());
			if (storageFromScheme.getA() != null)
				return new ValuePair<>(storageFromScheme.getA(), asUri);
			else
				return new ValuePair<>(guessStorageFromUri(asUri), asUri);

		}

		private static Pair<StorageFormat, String> getStorageFromNestedScheme(String uri) {

			final Matcher storageSchemeMatcher = StorageFormat.STORAGE_SCHEME_PATTERN.matcher(uri);
			storageSchemeMatcher.matches();
			final String storageFormatScheme = storageSchemeMatcher.group(STORAGE_SCHEME_GROUP);
			final String uriGroup = storageSchemeMatcher.group(URI_GROUP);
			if (storageFormatScheme != null) {
				for (StorageFormat format : StorageFormat.values()) {
					if (format.schemePattern.asPredicate().test(storageFormatScheme))
						return new ValuePair<>(format, uriGroup);
				}
			}
			return new ValuePair<>(null, uriGroup);
		}

		N5Reader openReader(final URI uri, final N5Factory factory) {

			return StorageFormat.getReader(this, uri, factory);
		}

		N5Writer openWriter(final URI uri, final N5Factory factory) {

			return StorageFormat.getWriter(this, uri, factory);
		}

		private static N5Reader getReader(StorageFormat storage, URI uri, N5Factory factory) {

			final KeyValueAccess access = KeyValueAccessBackend.getKeyValueAccess(uri, factory);
			final String containerPath;
			/* Any more special cases? google? */
			if (access instanceof AmazonS3KeyValueAccess) {
				containerPath = AmazonS3Utils.getS3Key(uri.toString());
			} else
				containerPath = uri.getPath();
			return StorageFormat.getReader(storage, access, containerPath, factory);
		}

		private static N5Writer getWriter(StorageFormat storage, URI uri, N5Factory factory) {

			factory.createS3Bucket = true;
			final KeyValueAccess access = KeyValueAccessBackend.getKeyValueAccess(uri, factory);
			final String containerPath;
			/* Any more special cases? google? */
			if (access instanceof AmazonS3KeyValueAccess) {
				containerPath = AmazonS3Utils.getS3Key(uri.toString());
			} else
				containerPath = uri.getPath();
			final N5Writer writer = StorageFormat.getWriter(storage, access, containerPath, factory);
			factory.createS3Bucket = false;
			return writer;
		}

		private static N5Reader getReader(StorageFormat storage, @Nullable KeyValueAccess access, String containerPath, N5Factory factory) {

			switch (storage) {
			case N5:
				return new N5KeyValueReader(access, containerPath, factory.gsonBuilder, factory.cacheAttributes);
			case ZARR:
				return new ZarrKeyValueReader(access, containerPath, factory.gsonBuilder, factory.zarrMapN5DatasetAttributes, factory.zarrMergeAttributes, factory.cacheAttributes);
			case HDF5:
				return new N5HDF5Reader(containerPath, factory.hdf5OverrideBlockSize, factory.gsonBuilder, factory.hdf5DefaultBlockSize);
			}
			return null;
		}

		private static N5Writer getWriter(StorageFormat storage, @Nullable KeyValueAccess access, String containerPath, N5Factory factory) {

			switch (storage) {
			case N5:
				return new N5KeyValueWriter(access, containerPath, factory.gsonBuilder, factory.cacheAttributes);
			case ZARR:
				return new ZarrKeyValueWriter(access, containerPath, factory.gsonBuilder, factory.zarrMapN5DatasetAttributes, factory.zarrMergeAttributes, factory.zarrDimensionSeparator, factory.cacheAttributes);
			case HDF5:
				return new N5HDF5Writer(containerPath, factory.hdf5OverrideBlockSize, factory.gsonBuilder, factory.hdf5DefaultBlockSize);
			}
			return null;
		}

	}
}
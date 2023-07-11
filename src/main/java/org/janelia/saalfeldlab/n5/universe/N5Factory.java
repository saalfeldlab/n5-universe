/**
 * Copyright (c) 2017-2021, Saalfeld lab, HHMI Janelia
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIG	 * @param mapN5DatasetAttributes 
	 * 	  If true, getAttributes and variants of getAttribute methods will
	 * 	  contain keys used by n5 datasets, and whose values are those for
	 *    their corresponding zarr fields. For example, if true, the key "dimensions"
	 *    (from n5) may be used to obtain the value of the key "shape" (from zarr). 
	 * @param mergeAttributes 
	 * 	  If true, fields from .zgroup, .zarray, and .zattrs will be merged
	 *    when calling getAttributes, and variants of getAttribute
	 * @param cacheMeta cache attributes and meta dataHT HOLDERS AND CONTRIBUTORS "AS IS"
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
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
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageReader;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageWriter;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;

/**
 * Factory for various N5 readers and writers.  Implementation specific
 * parameters can be provided to the factory instance and will be used when
 * such implementations are generated and ignored otherwise. Reasonable
 * defaults are provided.
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
	private boolean cacheAttributes = false;

	private String zarrDimensionSeparator = ".";
	private boolean zarrMapN5DatasetAttributes = true;
	private boolean zarrMergeAttributes = true;
	
	private String googleCloudProjectId = null;

	private String s3Region = null;
	private AWSCredentials s3Credentials = null;
	private boolean s3Anonymous = true;

	private KeyValueAccess keyValueAccess;

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
	 * This factory will use the {@link DefaultAWSCredentialsProviderChain} to find s3 credentials.
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

	private static boolean isHDF5Writer(final String path) {

		if (path.matches("(?i).*\\.(h5|hdf|hdf5)"))
			return true;
		else
			return false;
	}

	private static boolean isHDF5Reader(final String path) throws N5IOException {

		if (Files.isRegularFile(Paths.get(path))) {
			/* optimistic */
			if (isHDF5Writer(path))
				return true;
			else {
				try (final FileInputStream in = new FileInputStream(new File(path))) {
					final byte[] sig = new byte[8];
					in.read(sig);
					return Arrays.equals(sig, HDF5_SIG);
				} catch (IOException e) {
					throw new N5Exception.N5IOException(e);
				}
			}
		}
		return false;
	}

	private AmazonS3 createS3(final String url) {

		AWSCredentials credentials = null;
		final AWSStaticCredentialsProvider credentialsProvider;
		if( s3Credentials != null ) {
			credentials = s3Credentials;
			credentialsProvider = new AWSStaticCredentialsProvider(credentials);
		}
		else {
			// if not anonymous, try finding credentials
			if( !s3Anonymous ) {
				try {
					credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
				} catch(final Exception e) {
					System.out.println( "Could not load AWS credentials, falling back to anonymous." );
				}
				credentialsProvider = new AWSStaticCredentialsProvider(credentials == null ? new AnonymousAWSCredentials() : credentials);
			}
			else
				credentialsProvider = new AWSStaticCredentialsProvider(new AnonymousAWSCredentials());
		}

		final AmazonS3URI uri = new AmazonS3URI(url);
		Regions region = Optional.ofNullable(uri.getRegion()).map(Regions::fromName)	// first try getting the region from the URL
			.orElse( Optional.ofNullable(s3Region).map(Regions::fromName)				// next use whatever is passed in
			.orElse(Regions.US_EAST_1));												// fallback to us-east-1

		return AmazonS3ClientBuilder.standard()
				.withCredentials(credentialsProvider)
				.withRegion(region)
				.build();
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
	 *
	 * For more options of the Zarr backend study the {@link N5ZarrReader}
	 * constructors.
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
	 *
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
	 * @param url url to the google cloud object
	 * @return the N5GoogleCloudStorageReader
	 */
	public N5GoogleCloudStorageReader openGoogleCloudReader(final String url) {

		final GoogleCloudStorageClient storageClient = new GoogleCloudStorageClient();
		final Storage storage = storageClient.create();
		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(url);

		return new N5GoogleCloudStorageReader(
				storage,
				googleCloudUri.getBucket(),
				googleCloudUri.getKey(),
				gsonBuilder);
	}

	/**
	 * Open an {@link N5Reader} for AWS S3.
	 *
	 * @param url url to the amazon s3 object
	 * @return the N5Reader
	 */
	public N5Reader openAWSS3Reader(final String url) {

		final AmazonS3URI s3uri = new AmazonS3URI(url);
		final AmazonS3 s3 = createS3(url);

		// when, if ever do we want to creat a bucket?
		final AmazonS3KeyValueAccess s3kv = new AmazonS3KeyValueAccess(s3, s3uri.getBucket(), false);
		if( url.contains(".zarr" )) {
			return new ZarrKeyValueReader(s3kv, s3uri.getURI().getPath(), gsonBuilder,
					zarrMapN5DatasetAttributes, zarrMergeAttributes, cacheAttributes );	
		}
		else {
			return new N5KeyValueReader(s3kv, s3uri.getURI().getPath(), gsonBuilder, cacheAttributes );	
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
	 *
	 * For more options of the Zarr backend study the {@link N5ZarrWriter}
	 * constructors.
	 *
	 * @param path path to the zarr directory
	 * @return the N5ZarrWriter
	 */
	public N5ZarrWriter openZarrWriter(final String path) {

		return new N5ZarrWriter(path, gsonBuilder, zarrDimensionSeparator, zarrMapN5DatasetAttributes, true );
	}

	/**
	 * Open an {@link N5Writer} for HDF5.  Don't forget to close the writer
	 * after writing to close the file and make it available to other
	 * processes.
	 *
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
	 * @param url url to the google cloud object
	 * @return the N5GoogleCloudStorageWriter
	 */
	public N5GoogleCloudStorageWriter openGoogleCloudWriter(final String url) {

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
		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(url);
		return new N5GoogleCloudStorageWriter(
				storage,
				googleCloudUri.getBucket(),
				googleCloudUri.getKey(),
				gsonBuilder);
	}

	/**
	 * Open an {@link N5Writer} for AWS S3.
	 *
	 * @param url url to the s3 object
	 * @return the N5Writer
	 */
	public N5Writer openAWSS3Writer(final String url) {

		final AmazonS3URI s3uri = new AmazonS3URI(url);
		final AmazonS3 s3 = createS3(url);

		// when, if ever do we want to creat a bucket?
		final AmazonS3KeyValueAccess s3kv = new AmazonS3KeyValueAccess(s3, s3uri.getBucket(), false);
		if( url.contains(".zarr" )) {
			return new ZarrKeyValueWriter(s3kv, s3uri.getKey(), gsonBuilder, 
					zarrMapN5DatasetAttributes, zarrMergeAttributes, zarrDimensionSeparator, cacheAttributes );	
		}
		else {
			return new N5KeyValueWriter(s3kv, s3uri.getKey(), gsonBuilder, cacheAttributes );	
		}
	}

	/**
	 * Open an {@link N5Reader} based on some educated guessing from the url.
	 *
	 * @param url the location of the root location of the store
	 * @return the N5Reader
	 */
	public N5Reader openReader(final String url) {

		try {
			final URI uri = N5URI.from(url, null, null).getURI();
			final String scheme = uri.getScheme();
			if (scheme == null);
			else if (scheme.equals("file")) {
				try {
					return openFileBasedN5Reader(Paths.get(uri).toFile().getCanonicalPath());
				} catch (IOException e) {
					throw new N5Exception.N5IOException(e);
				}
			} else if (scheme.equals("s3"))
				return openAWSS3Reader(url);
			else if (scheme.equals("gs"))
				return openGoogleCloudReader(url);
			else if (uri.getHost()!= null && scheme.equals("https") || scheme.equals("http")) {
				if (uri.getHost().matches(".*s3\\.amazonaws\\.com"))
					return openAWSS3Reader(url);
				else if (uri.getHost().matches(".*cloud\\.google\\.com") || uri.getHost().matches(".*storage\\.googleapis\\.com"))
					return openGoogleCloudReader(url);
			}
		} catch (final URISyntaxException ignored ) {}
		return openFileBasedN5Reader( url );
	}

	private N5Reader openFileBasedN5Reader( final String url ) {
		if (isHDF5Reader( url ))
			return openHDF5Reader( url );
		else if (url.matches("(?i).*\\.zarr.*"))
			return openZarrReader( url );

		else
			return openFSReader( url );
	}

	/**
	 * Open an {@link N5Writer} based on some educated guessing from the url.
	 *
	 * @param url the location of the root location of the store
	 * @return the N5Writer
	 */
	public N5Writer openWriter(final String url) {

		try {
			final URI uri = N5URI.from(url, null, null).getURI();
			final String scheme = uri.getScheme();
			if (scheme == null);
			else if (scheme.equals("file"))
				return openFileBasedN5Writer( uri.getPath() );
			else if (scheme.equals("s3"))
				return openAWSS3Writer(url);
			else if (scheme.equals("gs"))
				return openGoogleCloudWriter(url);
			else if (uri.getHost() != null && scheme.equals("https") || scheme.equals("http")) {
				if (uri.getHost().matches(".*s3\\.amazonaws\\.com"))
					return openAWSS3Writer(url);
				else if (uri.getHost().matches(".*cloud\\.google\\.com") || uri.getHost().matches(".*storage\\.googleapis\\.com"))
					return openGoogleCloudWriter(url);
			}
		} catch (final URISyntaxException e) {}
		return openFileBasedN5Writer( url );
	}

	private N5Writer openFileBasedN5Writer( final String url )
	{
		if (isHDF5Writer(url))
			return openHDF5Writer(url);
		else if (url.matches("(?i).*\\.zarr"))
			return openZarrWriter(url);
		else
			return openFSWriter(url);
	}
}
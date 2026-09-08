package org.janelia.saalfeldlab.n5.universe.factory.spi;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

/**
 * The HDF5 format: knows how to open an HDF5 container given a path and
 * {@link HDF5Options}.
 * <p>
 * This is the service interface of the {@code n5-hdf5} module.
 * <p>
 * Unlike {@link AmazonS3Provider} / {@link GoogleCloudProvider}, this is a
 * <em>format</em> provider, not a backend one: an HDF5 container is a single
 * file opened by path, not a key-value hierarchy, so there is no
 * {@code KeyValueRoot} involved and {@code Containers} can only reach it for a
 * local location. Its reason to be a service is the same though — jhdf5 carries
 * platform-native binaries, which core should not force on anyone.
 * <p>
 * Note what deliberately does <em>not</em> live here: recognising an HDF5 file.
 * That is the 8-byte signature check inlined in
 * {@code InferredStorageFormat.isHdf5File}, plain {@code java.io}, so core can
 * say "this is HDF5" even when no provider can open it — and then throw a
 * message that names the missing artifact.
 */
public interface Hdf5Provider {

	/**
	 * Open an HDF5 container for reading.
	 *
	 * @param containerPath path of the HDF5 file
	 * @param options the format configuration
	 * @return the reader
	 */
	N5Reader buildReader(String containerPath, HDF5Options options);

	/**
	 * Open an HDF5 container for writing, creating it if it does not exist.
	 *
	 * @param containerPath path of the HDF5 file
	 * @param options the format configuration
	 * @return the writer
	 */
	N5Writer buildWriter(String containerPath, HDF5Options options);
}

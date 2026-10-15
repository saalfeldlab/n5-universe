package org.janelia.saalfeldlab.n5.universe.translation;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import org.janelia.saalfeldlab.n5.ContainerDialect;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonN5Reader;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;
import org.janelia.saalfeldlab.n5.N5Reader;

public class TranslatedN5Reader implements GsonN5Reader {

	private final GsonN5Reader delegate;

	/**
	 * The translated hierarchy. Used by {@link #containerDialect}.
	 */
	final DirectoryStore translated;

	/**
	 * Translation from {@link #translated} back to the hierarchy of the delegate
	 * container.
	 */
	final JqTranslation inverse;

	private final ContainerDialect containerDialect;

	public TranslatedN5Reader( final N5Reader n5Base,
			final String fwdTranslation,
			final String invTranslation ) {

		this(n5Base, fwdTranslation, invTranslation, null);
	}

	/**
	 * The delegate's hierarchy is harvested once, here, and with {@code
	 * executor} in parallel. That is worth it where every request to the
	 * delegate is a network round trip (cloud backends): the harvest makes a
	 * few requests per group or dataset.
	 * <p>
	 * {@code executor} is used only during construction. The caller keeps
	 * ownership, and may shut it down once this constructor returns.
	 *
	 * @param n5Base
	 * 		the container to translate
	 * @param fwdTranslation
	 * 		jq expression translating the hierarchy of {@code n5Base}
	 * @param invTranslation
	 * 		jq expression translating it back
	 * @param executor
	 * 		executor to harvest the hierarchy of {@code n5Base} with, or {@code
	 * 		null} to harvest it sequentially
	 */
	public TranslatedN5Reader( final N5Reader n5Base,
			final String fwdTranslation,
			final String invTranslation,
			final Executor executor ) {

		delegate = (GsonN5Reader) n5Base;
		final DialectInfo dialect = DialectInfo.of(delegate);

		// NB: The forward translation is applied once, here. After this the
		// translated hierarchy is the one that gets modified, and `inverse`
		// derives the delegate's hierarchy from it.
		final JqTranslation forward = new JqTranslation(dialect, fwdTranslation);
		translated = new DirectoryStore(forward.translate(Directory.collectFrom(delegate, executor)));
		inverse = new JqTranslation(dialect, invTranslation);

		containerDialect = new NotifyingDialect(
				delegate.getContainerDialect().withStore(translated),
				this::onModified);
	}

	@Override
	public ContainerDialect getContainerDialect() {
		return containerDialect;
	}

	/**
	 * Close the delegate container.
	 * <p>
	 * The translated view owns its delegate: chunks are read from the delegate
	 * for as long as the view is in use, so this is the only point at which it
	 * can be closed. This matters for delegates holding native resources, such
	 * as an {@code N5HDF5Reader} (its JHDF5 reader and open data sets).
	 */
	@Override
	public void close() {
		delegate.close();
	}

	/**
	 * Called after every modification of the translated hierarchy made through
	 * {@link #getContainerDialect()}.
	 * <p>
	 * Does nothing for a reader. {@link TranslatedN5Writer} overrides this to
	 * translate the change back and write it to the delegate container.
	 */
	void onModified() {}

	/**
	 * Returns the path in the original container given the path in the
	 * translated container.
	 *
	 * @param pathName the path in the translated container
	 * @return the path in the original container
	 */
	String originalPath(final String pathName) {

		return inverse.translatePath(N5DirectoryPath.of(pathName));
	}

	@Deprecated
	@Override
	public String getAttributesKey() {
		return delegate.getAttributesKey();
	}

	@Override
	public URI getURI() {
		return delegate.getURI();
	}

	/**
	 * The delegate decides how {@code DatasetAttributes} are converted (the zarr
	 * readers narrow them to their own subclass). Translating does not change
	 * the dialect, so the translated view must convert them the same way.
	 */
	@Override
	public DatasetAttributes getConvertedDatasetAttributes(final DatasetAttributes attributes) {
		return delegate.getConvertedDatasetAttributes(attributes);
	}


	// NB: Chunks are not translated: only the dataset path is.

	// TODO: Passing the datasetAttributes on to the delegate is not strictly correct.
	//   We obtain them from the translated store, so they might be modified by the translation.
	//   In practice it is probably always ok, so I'll just leave this unfixed for now.

	@Override
	public <T> DataBlock<T> readChunk(final String datasetPath, final DatasetAttributes datasetAttributes, final long... gridPosition) throws N5Exception {
		return delegate.readChunk(originalPath(datasetPath), datasetAttributes, gridPosition);
	}

	/**
	 * NB: Overridden so that the delegate does the bulk read. The inherited
	 * default would loop over {@link #readChunk}, which resolves {@code
	 * originalPath} per chunk and gives up the delegate's batched (and
	 * shard-aware) chunk access.
	 */
	@Override
	public <T> List<DataBlock<T>> readChunks(final String datasetPath, final DatasetAttributes datasetAttributes, final List<long[]> gridPositions) throws N5Exception {
		return delegate.readChunks(originalPath(datasetPath), datasetAttributes, gridPositions);
	}

	@Override
	public <T> DataBlock<T> readBlock(final String datasetPath, final DatasetAttributes datasetAttributes, final long... gridPosition) throws N5Exception {
		return delegate.readBlock(originalPath(datasetPath), datasetAttributes, gridPosition);
	}

	@Override
	public boolean blockExists(final String datasetPath, final DatasetAttributes datasetAttributes, final long... gridPosition) throws N5Exception {
		return delegate.blockExists(originalPath(datasetPath), datasetAttributes, gridPosition);
	}
}

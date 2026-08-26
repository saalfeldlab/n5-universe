package org.janelia.saalfeldlab.n5.universe.kvr;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonN5Writer;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;

/**
 * A writable {@link TranslatedN5Reader}.
 * <p>
 * Metadata is written to the translated hierarchy, which is then translated
 * back and flushed to the delegate container. This needs no per-method
 * overriding: the inherited {@link GsonN5Writer} methods are all routed through
 * {@link #getContainerDialect()}, a {@link NotifyingDialect} that calls {@link
 * #onModified()} after every modification.
 * <p>
 * Chunks are not translated, so they go straight to the delegate, at the
 * {@link #originalPath} of the dataset.
 */
public class TranslatedN5Writer extends TranslatedN5Reader implements GsonN5Writer {

	private final GsonN5Writer delegateWriter;

	public TranslatedN5Writer(final N5Writer n5Base,
			final String fwdTranslation,
			final String invTranslation) {
		super(n5Base, fwdTranslation, invTranslation);
		delegateWriter = (GsonN5Writer) n5Base;
	}

	/**
	 * Translate the (just modified) hierarchy back, and write it to the delegate
	 * container.
	 */
	@Override
	void onModified() {
		Directory.writeTo(inverse.translate(translated.getRoot()), delegateWriter);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * NB: Removing needs more than {@link #onModified()}, which only creates and
	 * overwrites: the group or dataset has to be deleted from the delegate
	 * explicitly.
	 */
	@Override
	public boolean remove(final String path) throws N5Exception {
		final String originalPath = originalPath(path);
		return GsonN5Writer.super.remove(path) && delegateWriter.remove(originalPath);
	}


	// NB: Chunks are not translated: only the dataset path is.

	@Override
	public <T> void writeChunk(final String datasetPath, final DatasetAttributes datasetAttributes, final DataBlock<T> chunk) throws N5Exception {
		delegateWriter.writeChunk(originalPath(datasetPath), datasetAttributes, chunk);
	}

	@Override
	public <T> void writeBlock(final String datasetPath, final DatasetAttributes datasetAttributes, final DataBlock<T> dataBlock) throws N5Exception {
		delegateWriter.writeBlock(originalPath(datasetPath), datasetAttributes, dataBlock);
	}

	@Override
	public <T> void writeRegion(final String datasetPath, final DatasetAttributes datasetAttributes, final long[] min, final long[] size,
			final DataBlockSupplier<T> chunkSupplier, final boolean writeFully) throws N5Exception {
		delegateWriter.writeRegion(originalPath(datasetPath), datasetAttributes, min, size, chunkSupplier, writeFully);
	}

	@Override
	public <T> void writeRegion(final String datasetPath, final DatasetAttributes datasetAttributes, final long[] min, final long[] size,
			final DataBlockSupplier<T> chunkSupplier, final boolean writeFully, final ExecutorService exec) throws N5Exception, InterruptedException, ExecutionException {
		delegateWriter.writeRegion(originalPath(datasetPath), datasetAttributes, min, size, chunkSupplier, writeFully, exec);
	}

	@Override
	public boolean deleteChunk(final String datasetPath, final DatasetAttributes datasetAttributes, final long... gridPosition) throws N5Exception {
		return delegateWriter.deleteChunk(originalPath(datasetPath), datasetAttributes, gridPosition);
	}

	@Override
	public boolean deleteBlock(final String datasetPath, final DatasetAttributes datasetAttributes, final long... gridPosition) throws N5Exception {
		return delegateWriter.deleteBlock(originalPath(datasetPath), datasetAttributes, gridPosition);
	}
}

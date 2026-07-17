package org.janelia.saalfeldlab.n5.universe;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class N5FactoryWithCacheTests extends N5FactoryTests {

	private @NonNull N5FactoryWithCache newN5FactoryWithCache() {
		return new N5FactoryWithCache();
	}

	@Override
	protected N5Factory newN5Factory() {
		return newN5FactoryWithCache();
	}

	/**
	 * Overrides {@link N5FactoryTests#testAmbiguousZarrFormat} to assert the caching behavior, which differs from the
	 * base factory in one spot. The cache is keyed by URI and validated for format compatibility: once an ambiguous
	 * {@code ZARR} open has cached a zarr2 reader, a later ambiguous open at the same URI returns that cached zarr2
	 * reader even after zarr3 has been added to the container. It does not re-resolve to the preferred zarr3 the way
	 * the uncached base factory does. Only the marked assertion differs.
	 */
	@Override
	@Test
	public void testAmbiguousZarrFormat() throws IOException {

		final N5Factory factory = newN5Factory();
		File tmpDir = Files.createTempDirectory("factory-test-").toFile();

		/* by default, should be zarr 3 with no other information */
		URI doesntExistsIsZarr3 = tmpDir.toPath().resolve("doesnt_exists_is_zarr3").toUri();
		N5Writer n5 = factory.openWriter(StorageFormat.ZARR, doesntExistsIsZarr3);
		assertEquals(ZarrV3KeyValueWriter.class, n5.getClass());

		/* If a zarr2 container exists, it should correctly return zarr2 */
		URI explicitZarr2 = tmpDir.toPath().resolve("explicit_zarr2").toUri();
		N5Writer explicitZarr2n5 = factory.openWriter(StorageFormat.ZARR2, explicitZarr2);
		assertEquals(ZarrKeyValueWriter.class, explicitZarr2n5.getClass());

		N5Reader guessZarr2Reader = factory.openReader(StorageFormat.ZARR, explicitZarr2);
		assertEquals(ZarrKeyValueReader.class, guessZarr2Reader.getClass());

		N5Reader guessZarr2Writer = factory.openWriter(StorageFormat.ZARR, explicitZarr2);
		assertEquals(ZarrKeyValueWriter.class, guessZarr2Writer.getClass());

		/* Valid as both zarr2 and zarr3 */
		URI bothZarrs = tmpDir.toPath().resolve("both_zarr2_zarr3").toUri();
		N5Writer bothZarr2 = factory.openWriter(StorageFormat.ZARR2, bothZarrs);
		/* only zarr2 exists so far, so an ambiguous read caches a zarr2 reader here */
		assertEquals(ZarrKeyValueReader.class, factory.openReader(StorageFormat.ZARR, bothZarrs).getClass());
		assertEquals(ZarrKeyValueWriter.class, factory.openWriter(StorageFormat.ZARR, bothZarrs).getClass());

		N5Writer bothZarr3 = factory.openWriter(StorageFormat.ZARR3, bothZarrs);
		/* the base factory re-resolves and returns zarr3 here; the cache instead returns the zarr2 reader cached
		 * above, since it is still format-compatible with the ambiguous ZARR request */
		assertEquals(ZarrKeyValueReader.class, factory.openReader(StorageFormat.ZARR, bothZarrs).getClass());
		/* the writer cache, on the other hand, was replaced by the zarr3 writer just opened, so ambiguous returns it */
		assertEquals(ZarrV3KeyValueWriter.class, factory.openWriter(StorageFormat.ZARR, bothZarrs).getClass());

		assertEquals(ZarrKeyValueReader.class, factory.openReader(StorageFormat.ZARR2, bothZarrs).getClass());
		assertEquals(ZarrKeyValueWriter.class, factory.openWriter(StorageFormat.ZARR2, bothZarrs).getClass());
	}

	@Test
	public void testCachedFactoryKeys() throws IOException {

		final N5FactoryWithCache cachedFactory = newN5FactoryWithCache();

		File tmp = null;
		try {
			tmp = Files.createTempDirectory("n5-cachedFactory-test-").toFile();
			final String tmpPath = tmp.getAbsolutePath();

			/* Writers */
			final N5Writer writer1 = cachedFactory.openWriter(tmpPath); // a ZarrKeyValueWriter
			final N5Writer writer2 = cachedFactory.openWriter(tmpPath);
			assertSame(writer2, writer1);

			final N5Writer writerFromStoragePrefix = cachedFactory.openWriter("zarr3:" + tmpPath);
			assertSame(writerFromStoragePrefix, writer1);

			final N5Writer writerFromStoragePrefix2 = cachedFactory.openWriter("zarr3://" + tmpPath);
			assertSame(writerFromStoragePrefix2, writer1);

			final N5Writer writerDifferentStorageFormat = cachedFactory.openWriter(StorageFormat.N5, tmpPath);
			assertNotSame(writerDifferentStorageFormat, writer1);

			final N5Writer writerFromStorageType = cachedFactory.openWriter(StorageFormat.ZARR3, tmpPath);
			assertNotSame(writerFromStorageType, writerDifferentStorageFormat);
			assertNotSame(writerFromStorageType, writer1);


			/* Readers */
			final N5Reader reader1 = cachedFactory.openReader(tmpPath);
			assertNotSame(reader1, writer1);

			final N5Reader reader2 = cachedFactory.openReader(tmpPath);
			assertSame(reader2, reader1);

			final N5Reader readerFromStoragePrefix = cachedFactory.openReader("zarr3:" + tmpPath);
			assertSame(readerFromStoragePrefix, reader1);

			final N5Reader readerFromStoragePrefix2 = cachedFactory.openReader("zarr3://" + tmpPath);
			assertSame(readerFromStoragePrefix2, reader1);

			final N5Reader readerDifferentStorageFormat = cachedFactory.openReader(StorageFormat.N5, tmpPath);
			assertNotSame(readerDifferentStorageFormat, reader1);

			final N5Reader readerFromStorageType = cachedFactory.openReader(StorageFormat.ZARR3, tmpPath);
			assertNotSame(readerFromStorageType, readerDifferentStorageFormat);
			assertNotSame(readerFromStorageType, reader1);


			/* path normalization */
			N5Reader expected = readerFromStorageType;

			final N5Reader readerFromUri = cachedFactory.openReader(tmp.toURI().toString());
			assertSame(readerFromUri, expected);

			final N5Reader readerSlash = cachedFactory.openReader(tmpPath + "/");
			assertSame(readerSlash, expected);

			final N5Reader readerNotNormal = cachedFactory.openReader(tmpPath + "/foo/..");
			assertSame(readerNotNormal, expected);

			/* different methods of URI creation */
			final N5Reader readerFromFileUri = cachedFactory.openReader(tmp.toURI().toString());
			assertSame(readerFromFileUri, expected);

			final N5Reader readerFromPathUri = cachedFactory.openReader(tmp.toPath().toUri().toString());
			assertSame(readerFromPathUri, expected);


			/* relative paths */
			final String rootName = "monkeySee.n5";
			final String absPath = Paths.get(rootName).toFile().getAbsolutePath();

			final N5Writer writerAbs = cachedFactory.openWriter(absPath);
			final N5Writer writerRel = cachedFactory.openWriter("./" + rootName);
			assertSame(String.format("writers not same instance: %s \n%s\n", writerRel.getURI(), writerAbs.getURI()),
					writerRel, writerAbs);

			final N5Writer writerRel2 = cachedFactory.openWriter(rootName);
			assertSame(String.format("writers not same instance: %s \n%s\n", writerRel2.getURI(), writerAbs.getURI()),
					writerRel2, writerAbs);

			final N5Writer writerRel3 = cachedFactory.openWriter(rootName + "/foo/..");
			assertSame(String.format("writers not same instance: %s \n%s\n", writerRel3.getURI(), writerAbs.getURI()),
					writerRel3, writerAbs);

			/*Clean up*/
			writerAbs.remove();


			/* clear and remove */
			cachedFactory.clear();

			final N5Reader readerAfterClear = cachedFactory.openReader(tmpPath);
			assertNotSame(readerAfterClear, expected);
			expected = readerAfterClear;

			final N5Writer writerAfterClear = cachedFactory.openWriter(tmpPath);
			assertNotSame(writerAfterClear, expected);

			// remove
			cachedFactory.remove(tmpPath);
			final N5Reader readerAfterRemove = cachedFactory.openReader(tmpPath);
			assertNotSame(readerAfterRemove, expected);
			expected = readerAfterRemove;

			final N5Writer writerAfterRemove = cachedFactory.openWriter(tmpPath);
			assertNotSame(writerAfterRemove, writerAfterClear);

			// remove normalization
			cachedFactory.remove("zarr:" + tmpPath);
			final N5Reader readerAfterRemovePrefix = cachedFactory.openReader(tmpPath);
			assertNotSame(readerAfterRemovePrefix, expected);

			final N5Writer writerAfterRemovePrefix = cachedFactory.openWriter(tmpPath);
			assertNotSame(writerAfterRemovePrefix, writerAfterRemove);


		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}

	/* Many threads opening the same not-yet-cached container must open it exactly once (a single cached instance). */
	@Test
	public void testConcurrentOpenIsThreadSafeAndOpensOnce() throws Exception {

		final int threads = 12;
		final int iterations = 12;
		final File tmp = Files.createTempDirectory("factory-cache-concurrent-").toFile();
		try {
			for (int iteration = 0; iteration < iterations; iteration++) {
				final N5FactoryWithCache factory = newN5FactoryWithCache();
				final String uri = tmp.toPath().resolve("iter" + iteration + ".n5").normalize().toUri().toString();

				final Set<N5Writer> writers = runConcurrently(threads, () -> factory.openWriter(uri));
				assertEquals("iteration " + iteration + ": writer opened more than once", 1, writers.size());

				/* the container now exists; concurrent readers likewise resolve to a single cached reader */
				final Set<N5Reader> readers = runConcurrently(threads, () -> factory.openReader(uri));
				assertEquals("iteration " + iteration + ": reader opened more than once", 1, readers.size());

				factory.clear();
			}
		} finally {
			FileUtils.deleteDirectory(tmp);
		}
	}

	@Test
	public void testOpenReaderAllowCachedWriter() throws IOException {

		final File tmp = Files.createTempDirectory( "factory-allow-cached-writer-" ).toFile();
		try {
			final String uri = "n5:" + new File( tmp, "container.n5" ).getAbsolutePath();
			/* create the container on disk so the fresh factories below can open it */
			new N5FactoryWithCache().openWriter( uri );

			/* 1. a cached reader is returned */
			final N5FactoryWithCache cachedReaderFactory = newN5FactoryWithCache();
			final N5Reader cachedReader = cachedReaderFactory.openReader( uri );
			assertSame( cachedReader, cachedReaderFactory.openReaderAllowCachedWriter( uri ) );

			/* 2. with no cached reader, a cached writer is returned as the reader */
			final N5FactoryWithCache cachedWriterFactory = newN5FactoryWithCache();
			final N5Writer cachedWriter = cachedWriterFactory.openWriter( uri );
			assertSame( cachedWriter, cachedWriterFactory.openReaderAllowCachedWriter( uri ) );

			/* 3. with neither cached, a new reader is opened and cached */
			final N5FactoryWithCache freshFactory = newN5FactoryWithCache();
			final N5Reader openedReader = freshFactory.openReaderAllowCachedWriter( uri );
			assertNotNull( openedReader );
			assertFalse( "a fresh reader, not a writer-as-reader", openedReader instanceof N5Writer );
			assertSame( "the opened reader was cached", openedReader, freshFactory.openReader( uri ) );
		} finally {
			FileUtils.deleteDirectory( tmp );
		}
	}

	/* Run task on every thread simultaneously. */
	private <T> Set<T> runConcurrently(final int threads, final Callable<T> task) throws Exception {

		final ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			final CyclicBarrier barrier = new CyclicBarrier(threads);
			final List<Future<T>> futures = new ArrayList<>();
			for (int t = 0; t < threads; t++) {
				futures.add(pool.submit(() -> {
					barrier.await();
					return task.call();
				}));
			}
			final Set<T> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
			for (final Future<T> future : futures) {
				final T result = future.get(30, TimeUnit.SECONDS);
				assertNotNull(result);
				distinct.add(result);
			}
			return distinct;
		} finally {
			pool.shutdownNow();
		}
	}
}

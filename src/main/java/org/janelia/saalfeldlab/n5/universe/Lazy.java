package org.janelia.saalfeldlab.n5.universe;

import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

/**
 * Convenience methods to create lazy evaluated cached cell images with consumers.
 *
 * @author Stephan Saalfeld
 */
public class Lazy {

	private Lazy() {}

	/**
	 * Create a memory {@link CachedCellImg} with a cell {@link Cache}.
	 *
	 * @param grid
	 * @param cache
	 * @param type
	 * @param accessFlags
	 * @return
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T extends NativeType<T>> CachedCellImg<T, ?> createImg(
			final CellGrid grid,
			final Cache<Long, Cell<?>> cache,
			final T type,
			final Set<AccessFlags> accessFlags) {

		final CachedCellImg<T, ?> img;

		if (GenericByteType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(BYTE, accessFlags));
		} else if (GenericShortType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(SHORT, accessFlags));
		} else if (GenericIntType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(INT, accessFlags));
		} else if (GenericLongType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(LONG, accessFlags));
		} else if (FloatType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(FLOAT, accessFlags));
		} else if (DoubleType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(DOUBLE, accessFlags));
		} else {
			img = null;
		}
		return img;
	}

	/**
	 * Create a memory {@link CachedCellImg} with a {@link CellLoader}.
	 * Unless you are doing something special, you will likely not use this
	 * method.
	 *
	 * @param targetInterval
	 * @param blockSize
	 * @param type
	 * @param accessFlags
	 * @param loader
	 * @return
	 */
	public static <T extends NativeType<T>> CachedCellImg<T, ?> createImg(
			final Interval targetInterval,
			final int[] blockSize,
			final T type,
			final Set<AccessFlags> accessFlags,
			final CellLoader<T> loader) {

		final long[] dimensions = Intervals.dimensionsAsLongArray(targetInterval);
		final CellGrid grid = new CellGrid(dimensions, blockSize);

		@SuppressWarnings({"unchecked", "rawtypes"})
		final Cache<Long, Cell<?>> cache =
				new SoftRefLoaderCache().withLoader(LoadedCellCacheLoader.get(grid, loader, type, accessFlags));

		return createImg(grid, cache, type, accessFlags);
	}

	/**
	 * Create a memory {@link CachedCellImg} with a cell generator implemented
	 * as a {@link Consumer}.  This is the most general purpose method for
	 * anything new.  Note that any inputs are managed by the cell generator,
	 * not by this method.
	 *
	 * @param targetInterval
	 * @param blockSize
	 * @param type
	 * @param accessFlags
	 * @param op
	 * @return
	 */
	public static <T extends NativeType<T>> CachedCellImg<T, ?> generate(
			final Interval targetInterval,
			final int[] blockSize,
			final T type,
			final Set<AccessFlags> accessFlags,
			final Consumer<RandomAccessibleInterval<T>> op) {

		return createImg(
				targetInterval,
				blockSize,
				type,
				accessFlags,
				op::accept);
	}

	/**
	 * Trigger pre-fetching of an {@link Interval} in a
	 * {@link RandomAccessible} by concurrent sampling of values at a sparse
	 * grid.
	 *
	 * Pre-fetching is only triggered and the set of value sampling
	 * {@link Future}s is returned such that it can be used to wait for
	 * completion or ignored (typically, ignoring will be best).
	 *
	 * This method is most useful to reduce wasted time waiting for high latency
	 * data loaders (such as AWS S3 or GoogleCloud).  Higher and more random
	 * latency benefit from higher parallelism, e.g. total parallelism with
	 * {@link Executors#newCachedThreadPool()}.  Medium latency loaders may be
	 * served better with a limited number of threads, e.g.
	 * {@link Executors#newFixedThreadPool(int)}.  The optimal solution depends
	 * also on how the rest of the application is parallelized and how much
	 * caching memory is available.
	 *
	 * We do not suggest to use this to fill a {@link CachedCellImg} with a
	 * generator because now the {@link ExecutorService} will do the complete
	 * processing work without guarantees that the generated cells will persist.
	 *
	 * @param <T>
	 * @param source
	 * @param interval
	 * @param spacing
	 * @param exec
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static <T> ArrayList<Future<T>> preFetch(
			final RandomAccessible<T> source,
			final Interval interval,
			final long[] spacing,
			final ExecutorService exec) throws InterruptedException, ExecutionException {

		final int n = interval.numDimensions();

		final long[] max = new long[n];
		Arrays.setAll(max, d -> interval.max(d) + spacing[d]);

		final ArrayList<Future<T>> futures = new ArrayList<>();

		final long[] offset = Intervals.minAsLongArray(interval);
		for (int d = 0; d < n;) {

			final long[] offsetLocal = offset.clone();
			futures.add(
					exec.submit(() -> {
						final RandomAccess<T> access = source.randomAccess(interval);
						access.setPosition(offsetLocal);
						return access.get();
					}));

			for (d = 0; d < n; ++d) {
				offset[d] += spacing[d];
				if (offset[d] < max[d]) {
					offset[d] = Math.min(offset[d], interval.max(d));
					break;
				} else
					offset[d] = interval.min(d);
			}
		}

		return futures;
	}
}

package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.HierarchyStore;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.cache.HierarchyCache;
import org.janelia.saalfeldlab.n5.universe.translation.TranslatedN5Reader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Constructing a {@link TranslatedN5Reader}, which harvests the delegate's
 * hierarchy, sequentially ({@code threads = 0}) and in parallel.
 * <p>
 * The delegate is a local N5 container whose {@code HierarchyStore} sleeps
 * {@code latencyMs} per read request, standing in for a cloud backend where
 * every request is a network round trip. Attributes are cached (the store sits
 * below a {@link HierarchyCache}, as for a reader opened with {@code
 * cacheAttributes}), and every invocation opens a new reader, so the cache
 * starts cold.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 1)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class ParallelHarvestBenchmarks {

	/**
	 * {@code wide}: 10 groups of 10 datasets. {@code deep}: 10 chains of 10
	 * nested groups, each ending in a dataset. About 110 directories each.
	 */
	@Param(value = {"wide", "deep"})
	private String shape;

	/** Number of threads harvesting; 0 means sequential (no executor). */
	@Param(value = {"0", "4", "16", "64"})
	private int threads;

	@Param(value = {"5"})
	private int latencyMs;

	/**
	 * NB: static, because the reader's constructor calls {@code
	 * createHierarchyStore}, before the fields of an anonymous subclass are
	 * assigned.
	 */
	private static volatile int sleepMs;

	private Path containerPath;
	private ExecutorService executor;

	public static void main(String... args) throws RunnerException {

		final Options options = new OptionsBuilder().include(ParallelHarvestBenchmarks.class.getSimpleName() + "\\.")
				.build();

		new Runner(options).run();
	}

	@Setup(Level.Trial)
	public void setup() throws IOException {

		sleepMs = latencyMs;
		containerPath = Files.createTempDirectory("ParallelHarvestBenchmark-").resolve("container.n5");
		try (N5FSWriter n5 = new N5FSWriter(containerPath.toString())) {
			for (int i = 0; i < 10; i++) {
				if (shape.equals("wide")) {
					for (int j = 0; j < 10; j++)
						n5.createDataset(String.format("g%d/ds%d", i, j), new long[]{8, 8}, new int[]{4, 4}, DataType.UINT8, new RawCompression());
				} else {
					final StringBuilder path = new StringBuilder("chain" + i);
					for (int depth = 0; depth < 9; depth++)
						path.append("/level").append(depth);
					n5.createDataset(path.append("/ds").toString(), new long[]{8, 8}, new int[]{4, 4}, DataType.UINT8, new RawCompression());
				}
			}
		}
		executor = threads > 0 ? Executors.newFixedThreadPool(threads) : null;
	}

	@TearDown(Level.Trial)
	public void teardown() throws IOException {

		if (executor != null)
			executor.shutdownNow();
		try (Stream<Path> paths = Files.walk(containerPath.getParent())) {
			paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	@Benchmark
	public void harvest(final Blackhole hole) {

		final N5FSReader delegate = new N5FSReader(containerPath.toString(), true) {

			@Override
			public HierarchyStore createHierarchyStore(final KeyValueRoot keyValueRoot, final boolean cacheMeta) {

				final HierarchyStore store = new SleepingHierarchyStore(super.createHierarchyStore(keyValueRoot, false));
				return cacheMeta ? new HierarchyCache(store) : store;
			}
		};
		try (TranslatedN5Reader translated = new TranslatedN5Reader(delegate, ".", ".", executor)) {
			hole.consume(translated);
		}
	}

	/**
	 * Sleeps {@link #sleepMs} before every read request, then delegates.
	 */
	private static class SleepingHierarchyStore implements HierarchyStore {

		private final HierarchyStore delegate;

		SleepingHierarchyStore(final HierarchyStore delegate) {

			this.delegate = delegate;
		}

		private static void sleep() {

			try {
				Thread.sleep(sleepMs);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new N5IOException("interrupted", e);
			}
		}

		@Override
		public JsonElement readAttributesJson(final N5DirectoryPath parent, final String filename, final Gson gson) throws N5IOException {

			sleep();
			return delegate.readAttributesJson(parent, filename, gson);
		}

		@Override
		public boolean isDirectory(final N5DirectoryPath path) {

			sleep();
			return delegate.isDirectory(path);
		}

		@Override
		public String[] listDirectories(final N5DirectoryPath path) throws N5IOException {

			sleep();
			return delegate.listDirectories(path);
		}

		@Override
		public void writeAttributesJson(final N5DirectoryPath parent, final String filename, final JsonElement attributes, final Gson gson) throws N5IOException {

			delegate.writeAttributesJson(parent, filename, attributes, gson);
		}

		@Override
		public void createDirectories(final N5DirectoryPath path) throws N5IOException {

			delegate.createDirectories(path);
		}

		@Override
		public void removeDirectory(final N5DirectoryPath path) throws N5IOException {

			delegate.removeDirectory(path);
		}
	}
}

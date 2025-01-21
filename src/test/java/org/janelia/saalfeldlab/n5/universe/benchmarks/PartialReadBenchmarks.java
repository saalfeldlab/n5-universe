package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.LockedChannel;
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

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 50, time = 1000, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class PartialReadBenchmarks {

	// @Param(value = {"1000000", "10000000", "100000000"})
	@Param(value = {"10000000"})
	protected int objectSizeBytes;

	@Param(value = {"1000"})
	protected int readSizeBytes;

	@Param(value = {"10", "50", "100"})
	protected int numSubReads;

	@Param(value = {"partial", "complete"})
	protected String mode;

	protected String baseDir;
	protected KeyValueAccess kva;
	protected Random random;

	protected int[] startPositions;

	public PartialReadBenchmarks() {}

	@TearDown(Level.Trial)
	public void teardown() {

		for (final int sz : sizes()) {
			new File(baseDir, "" + sz).delete();
			new File(baseDir).delete();
		}
	}

	@Setup(Level.Trial)
	public void setup() {

		random = new Random(1);
		kva = new FileSystemKeyValueAccess(FileSystems.getDefault());

		File tmpFile;
		try {
			tmpFile = Files.createTempDirectory("partialReadBenchmark-").toFile();
			baseDir = tmpFile.getCanonicalPath();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		startPositions = IntStream.generate(() -> {

			return random.nextInt(objectSizeBytes - readSizeBytes);
		}).limit(numSubReads).toArray();

		for (final int sz : sizes()) {
			String path;
			try {
				path = new File(baseDir, "" + sz).getCanonicalPath();
				write(path, sz);
			} catch (final IOException e) {}
		}
	}

	protected void write(String path, int numBytes) {

		final byte[] data = new byte[numBytes];
		random.nextBytes(data);

		System.out.println("write to path: " + path);

		try (final LockedChannel ch = kva.lockForWriting(path)) {
			final OutputStream os = ch.newOutputStream();
			os.write(data);
			os.flush();
			os.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] read(String path, int startByte, int numBytes) throws IOException {

		try (final LockedChannel ch = kva.lockForReading(path, startByte, numBytes)) {
			final InputStream is = ch.newInputStream();
			final byte[] data = new byte[numBytes];
			is.read(data);
			is.close(); // not strictly needed
			return data;
		}
	}

	private byte[] readComplete(String path) throws IOException {

		try (final ByteArrayOutputStream result = new ByteArrayOutputStream()) {

			try (final LockedChannel ch = kva.lockForReading(path)) {
				final InputStream is = ch.newInputStream();
				byte[] buffer = new byte[1024];
				for (int length; (length = is.read(buffer)) != -1;) {
					result.write(buffer, 0, length);
				}
				return result.toByteArray();
			}
		}
	}

	@Benchmark
	public void run(Blackhole blackhole) throws IOException {

		final String path = new File(baseDir, "" + objectSizeBytes).getCanonicalPath();
		if (mode.equals("partial")) {
			for (int i = 0; i < numSubReads; i++) {
				blackhole.consume(read(path, startPositions[i], readSizeBytes));
			}
		} else {
			blackhole.consume(readComplete(path));
		}
	}

	public int[] sizes() {

		try {
			final Param ann = PartialReadBenchmarks.class.getDeclaredField("objectSizeBytes")
					.getAnnotation(Param.class);
			System.out.println(Arrays.toString(ann.value()));
			return Arrays.stream(ann.value()).mapToInt(Integer::parseInt).toArray();

		} catch (final NoSuchFieldException e) {
			e.printStackTrace();
		} catch (final SecurityException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String... args) throws RunnerException {

		final Options options = new OptionsBuilder().include(PartialReadBenchmarks.class.getSimpleName() + "\\.")
				.build();

		new Runner(options).run();
	}

}

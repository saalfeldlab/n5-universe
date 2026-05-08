package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.LockedChannel;
import org.janelia.saalfeldlab.n5.readdata.ReadData;
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
@Warmup(iterations = 50, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 500, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class KeyValueAccessBenchmarks {

	@Param(value = { "100000", "1000000", "10000000" })
	protected int objectSizeBytes;
	
	protected byte[] data;
	protected ReadData readData;

	protected Path path;
	protected KeyValueAccess kva;
	protected Random random;

	public KeyValueAccessBenchmarks() {}

	public static void main(String... args) throws RunnerException {

		final Options options = new OptionsBuilder().include(KeyValueAccessBenchmarks.class.getSimpleName() + "\\.")
				.build();

		new Runner(options).run();
	}
	
	@Setup(Level.Trial)
	public void setup() throws IOException {

		random = new Random();
		kva = new FileSystemKeyValueAccess();

		data = new byte[objectSizeBytes];
		random.nextBytes(data);
		readData = ReadData.from(data);

		final int randomId = random.nextInt();
		path = Files.createTempFile("KeyValueAccessBenchmark-" + objectSizeBytes, "-" + randomId + ".dat");
		Files.write(path, data, StandardOpenOption.CREATE);
	}

	@TearDown(Level.Trial)
	public void teardown() {
		path.toFile().delete();
	}

	@Benchmark
	public void read(Blackhole hole) throws IOException {

		final ReadData read = kva.createReadData(path.toString());
		hole.consume(read.materialize().allBytes());
	}
	
	/**
	 * Legacy reading
	 */
//	@Benchmark
//	public void read(Blackhole hole) throws IOException {
//
//		try (	final LockedChannel ch = kva.lockForReading(path.toAbsolutePath().toString());
//				final InputStream is = ch.newInputStream()) {
//
//			final byte[] readData = new byte[objectSizeBytes];
//			is.read(readData);
//			hole.consume(readData);
//		} catch (final IOException e) {
//			e.printStackTrace();
//		};
//	}

	@Benchmark
	public void write(Blackhole hole) throws IOException {

		kva.write(path.toAbsolutePath().toString(), readData);
	}

}

package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.LockedChannel;
import org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueAccess;
import org.janelia.saalfeldlab.n5.s3.AmazonS3Utils;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.amazonaws.services.s3.AmazonS3;

@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 2, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class S3PartialReadBenchmarks {

	// TODO do a better job sharing functionality with file system benchmark?

	@Param(value = {"1000000"})
	protected int objectSizeBytes;

	@Param(value = {"1", "10", "50", "100"})
	protected int numSubReads;

	protected String baseDir;
	protected KeyValueAccess kva;
	protected Random random;

	public S3PartialReadBenchmarks() {}

	@TearDown(Level.Trial)
	public void teardown() {

		for (final int sz : sizes()) {
			try {
				kva.delete(baseDir + "/" + sz);
				kva.delete(baseDir);
			} catch (final IOException e) {}
		}
	}

	@Setup(Level.Trial)
	public void setup() throws InterruptedException {

		random = new Random();

		URI uri;
		try {
			uri = new URI("s3://n5-zarr-benchmarks");
		} catch (final URISyntaxException e) {
			e.printStackTrace();
			return;
		}

		final AmazonS3 s3 = AmazonS3Utils.createS3(uri.toASCIIString(), null,
				AmazonS3Utils.getS3Credentials(null, false), null, null);
		kva = new AmazonS3KeyValueAccess(s3, uri, false);

		baseDir = uri + "/partialReadBenchmarkData/" + random.nextInt(99999);
		for (final int sz : sizes()) {
			final String path = baseDir + "/" + sz;
			write(path, sz);
			Thread.sleep(500);
		}
	}

	private void read(String path, int startByte, int numBytes) {

		try (final LockedChannel ch = kva.lockForReading(path, startByte, numBytes)) {
			final InputStream is = ch.newInputStream();
			final byte[] data = new byte[numBytes];
			is.read(data);
			is.close(); // not strictly needed
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Benchmark
	public void run() throws IOException {

		final String path = baseDir + "/" + objectSizeBytes;

		// read
		final int numBytesToRead = objectSizeBytes / numSubReads;
		int start = 0;
		for (int i = 0; i < numSubReads; i++) {
			read(path, start, numBytesToRead);
			start += numBytesToRead;
		}
	}

	protected void write(String path, int numBytes) {

		final byte[] data = new byte[numBytes];
		random.nextBytes(data);

		try (final LockedChannel ch = kva.lockForWriting(path)) {
			final OutputStream os = ch.newOutputStream();
			os.write(data);
			os.flush();
			os.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public int[] sizes() {

		try {
			final Param ann = S3PartialReadBenchmarks.class.getDeclaredField("objectSizeBytes")
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

		final Options options = new OptionsBuilder().include(S3PartialReadBenchmarks.class.getSimpleName() + "\\.")
				.build();

		new Runner(options).run();
	}

}

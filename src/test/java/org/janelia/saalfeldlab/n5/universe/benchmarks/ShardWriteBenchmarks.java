package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.ByteArrayDataBlock;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ShardedDatasetAttributes;
import org.janelia.saalfeldlab.n5.codec.Codec;
import org.janelia.saalfeldlab.n5.codec.DeterministicSizeCodec;
import org.janelia.saalfeldlab.n5.codec.N5BlockCodec;
import org.janelia.saalfeldlab.n5.codec.RawBytes;
import org.janelia.saalfeldlab.n5.codec.checksum.Crc32cChecksumCodec;
import org.janelia.saalfeldlab.n5.shard.InMemoryShard;
import org.janelia.saalfeldlab.n5.shard.Shard;
import org.janelia.saalfeldlab.n5.shard.ShardingCodec.IndexLocation;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
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

/**
 * Compare the speed of writing in append-only mode vs "packed" (read-write) mode.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 50, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class ShardWriteBenchmarks {

	@Param(value = {"1000"})
	protected int blockSize;

	@Param(value = {"50"})
	protected int numBlocks;

	@Param(value = {"50", "75"})
	protected int numBlockWrites;

	@Param(value = {"append", "readWrite"})
	protected String writeMode;

	protected String baseDir;
	protected KeyValueAccess kva;
	protected Random random;

	protected N5Writer n5;
	private ShardedDatasetAttributes attributes;
	protected List<DataBlock<byte[]>> blocks;

	public ShardWriteBenchmarks() {}

	@TearDown(Level.Trial)
	public void teardown() {

		System.out.println("teardown");
		n5.remove();
		n5.close();
	}

	@Setup(Level.Trial)
	public void setup() {

		random = new Random();
		kva = new FileSystemKeyValueAccess(FileSystems.getDefault());

		File tmpFile;
		try {
			tmpFile = Files.createTempDirectory("shardWriteBenchmark-").toFile();
			baseDir = tmpFile.getCanonicalPath();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		genDataBlocks();
		genAttributes();

		n5 = new N5Factory().openWriter(baseDir);
		n5.createDataset("", attributes);
	}
	
	protected void genAttributes() {

		final long[] dimensions = new long[]{blockSize * numBlocks};
		final int[] blkSize = new int[]{blockSize};
		final int[] shardSize = new int[]{blockSize * numBlocks};

		attributes =  new ShardedDatasetAttributes(
				dimensions,
				shardSize,
				blkSize,
				DataType.UINT8,
				new Codec[]{new N5BlockCodec(), new GzipCompression(4)},
				new DeterministicSizeCodec[]{new RawBytes(), new Crc32cChecksumCodec()},
				IndexLocation.END
		);
	}

	protected void genDataBlocks() {

		blocks = IntStream.range(0, numBlockWrites).mapToObj( i -> {

			final byte[] data = new byte[blockSize];
			random.nextBytes(data);

			final int position = i % numBlocks;
			return new ByteArrayDataBlock(new int[]{blockSize}, new long[] {position}, data);

		}).collect(Collectors.toList());
	}

	public void append() {

		for (DataBlock<byte[]> blk : blocks) {
			n5.writeBlock(baseDir, attributes, blk);
		}
	}

	public void readWrite() {

		for (DataBlock<byte[]> blk : blocks) {

			@SuppressWarnings("unchecked")
			final Shard<byte[]> vshard = n5.readShard("", attributes, 0);
			final InMemoryShard<byte[]> shard = InMemoryShard.fromShard(vshard);
			shard.addBlock(blk);

			n5.writeShard("", attributes, shard);
		}
	}

	@Benchmark
	public void run() throws IOException {

		if( writeMode.equals("readWrite")) {
			readWrite();
		}
		else {
			append();
		}
	}

	public static void main(String... args) throws RunnerException {

		final Options options = new OptionsBuilder().include(ShardWriteBenchmarks.class.getSimpleName() + "\\.")
				.build();

		new Runner(options).run();
	}

}

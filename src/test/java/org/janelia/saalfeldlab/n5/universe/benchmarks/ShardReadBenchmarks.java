package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.ByteArrayDataBlock;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.ShardedDatasetAttributes;
import org.janelia.saalfeldlab.n5.codec.Codec;
import org.janelia.saalfeldlab.n5.codec.DeterministicSizeCodec;
import org.janelia.saalfeldlab.n5.codec.RawBytes;
import org.janelia.saalfeldlab.n5.codec.checksum.Crc32cChecksumCodec;
import org.janelia.saalfeldlab.n5.shard.InMemoryShard;
import org.janelia.saalfeldlab.n5.shard.Shard;
import org.janelia.saalfeldlab.n5.shard.ShardingCodec.IndexLocation;
import org.janelia.saalfeldlab.n5.shard.VirtualShard;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.util.GridIterator;
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


import net.imglib2.type.numeric.integer.UnsignedByteType;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time=100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 25, time=100, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class ShardReadBenchmarks {

	@Param(value = {"SHARD_WHOLE", "SHARD_BATCH", "BLOCK" })
	protected String readMethod;

//	@Param(value = {"10", "100", "1000"})
	@Param(value = {"1000"})
	protected int blocksPerShard;

//	@Param(value = {"10", "100", "1000"})
	@Param(value = {"1", "100"})
	protected int numBlockReads;

//	@Param(value = {"1000", "10000", "100000"})
	@Param(value = {"10000"})
	protected int blockSize;

	@Param(value = {"END"})
	protected String indexLocation;

	protected String baseDir;
	protected String dset = "shards";

	protected GsonKeyValueN5Writer n5Writer;
	protected int[] blockIndexes;
	protected List<long[]> blockPositions;
	private ShardedDatasetAttributes attrs;

	protected Random random;


	public ShardReadBenchmarks() {}

	@TearDown(Level.Trial)
	public void teardown() {

		n5Writer.remove();
	}

	@Setup(Level.Trial)
	public void setup() {

		random = new Random();

//		File tmpFile;
//		try {
//			tmpFile = Files.createTempDirectory("shardReadBenchmark-").toFile();
//			baseDir = tmpFile.getCanonicalPath();
//		} catch (final IOException e) {
//			e.printStackTrace();
//		}

		baseDir = "s3://n5-zarr-benchmarks/shardBenchmarks.zarr";

		write(baseDir);

		// the block indexes to read, a random
		List<Integer> indexes = IntStream.range(0, blocksPerShard).boxed().collect(Collectors.toList());
		Collections.shuffle(indexes);
		blockIndexes = indexes.stream().mapToInt(Integer::intValue).limit(numBlockReads).toArray();
		blockPositions = Arrays.stream(blockIndexes).mapToObj(i -> {
			final long[] p = new long[1];
			GridIterator.indexToPosition(i, new int[]{blocksPerShard}, p);
			return p;
		}).collect(Collectors.toList());

	}

	protected void write(String path) {

		n5Writer = (GsonKeyValueN5Writer) new N5Factory().openWriter("zarr3:"+path);
		attrs = new ShardedDatasetAttributes(dimensions(), shardSize(), blockSize(), DataType.INT8, 
				new Codec[]{new RawBytes(), new GzipCompression()},
				new DeterministicSizeCodec[]{new RawBytes(), new Crc32cChecksumCodec()},
				indexLocation());

		n5Writer.remove(dset);
		n5Writer.createDataset(dset, attrs);

		System.out.println("writing to: " + path);

		final InMemoryShard<byte[]> shard = new InMemoryShard<byte[]>(attrs, new long[1]);
		n5Writer.writeBlocks(dset, attrs, dataBlocks(shard));
	}

	protected DataBlock<byte[]>[] dataBlocks(final Shard<?> shard) {

		final DataBlock<byte[]>[] blocks = new DataBlock[blocksPerShard];

		int i = 0;
		Iterator<long[]> it = shard.blockPositionIterator();
		while( it.hasNext()) {
			final long[] position = it.next();

			final byte[] arr = new byte[blockSize];
			random.nextBytes(arr);

			blocks[i++] = new ByteArrayDataBlock(blockSize(), position, arr);
		}
		return blocks;
	}

	protected long[] dimensions() {
		return new long[] { blockSize * blocksPerShard };
	}

	protected int[] shardSize() {
		return GridIterator.long2int(dimensions());
	}

	protected int[] blockSize() {
		return new int[] { blockSize };
	}

	protected IndexLocation indexLocation() {
		return IndexLocation.valueOf(indexLocation);
	}

	private void readShardBatch(Blackhole blackhole, String path) {

		Shard<UnsignedByteType> shard = n5Writer.readShard(path, attrs, 0);
		List<DataBlock<UnsignedByteType>> blks = shard.getBlocks();
		for( DataBlock<UnsignedByteType> blk : blks)
			blackhole.consume(blk);

	}

	private void readShardWhole(Blackhole blackhole, String path) {

		InMemoryShard<UnsignedByteType> shard;
		try {
			final KeyValueAccess kva = n5Writer.getKeyValueAccess();
			final String shardPath = n5Writer.absoluteDataBlockPath(path, 0);
			shard = InMemoryShard.readShard(kva, shardPath, new long[]{ 0 }, attrs);
			for (DataBlock<UnsignedByteType> blk : shard.getBlocks(blockIndexes))
				blackhole.consume(blk);

		} catch (IOException e) { }

	}

	private void readBlock(Blackhole blackhole, String path) {

		Shard<UnsignedByteType> shard = n5Writer.readShard(path, attrs, 0);
		DataBlock<UnsignedByteType> blk;
		for( long[] blockPosition : blockPositions ) {
			blk = shard.getBlock(blockPosition);
			blackhole.consume(blk);
		}
	}

	@Benchmark
	public void run(Blackhole blackhole) throws IOException {

		if (readMethod.equals("SHARD_BATCH"))
			readShardBatch(blackhole, dset);
		else if (readMethod.equals("SHARD_WHOLE"))
			readShardWhole(blackhole, dset);
		else if (readMethod.equals("BLOCK"))
			readBlock(blackhole, dset);
	}

	public int[] sizes() {

		try {
			final Param ann = ShardReadBenchmarks.class.getDeclaredField("objectSizeBytes")
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

		final Options options = new OptionsBuilder().include(ShardReadBenchmarks.class.getSimpleName() + "\\.")
				.build();

		new Runner(options).run();
	}

}

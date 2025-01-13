package org.janelia.saalfeldlab.n5.universe.demo;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GsonKeyValueN5Writer;
import org.janelia.saalfeldlab.n5.IntArrayDataBlock;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ShardedDatasetAttributes;
import org.janelia.saalfeldlab.n5.codec.BytesCodec;
import org.janelia.saalfeldlab.n5.codec.Codec;
import org.janelia.saalfeldlab.n5.codec.DeterministicSizeCodec;
import org.janelia.saalfeldlab.n5.codec.checksum.Crc32cChecksumCodec;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.shard.InMemoryShard;
import org.janelia.saalfeldlab.n5.shard.ShardingCodec.IndexLocation;
import org.janelia.saalfeldlab.n5.shard.VirtualShard;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;

public class ShardWritingDemos {

	public static void main(String[] args) throws IOException {

		highLevel();

		midLevel();
		midLevelBatch();

		lowLevelVirtual();
		lowLevelBatch();
	}

	public static void highLevel() {

		final ArrayImg<IntType, IntArray> img = generateData(32, 27);

		try (final N5Writer zarr = new N5Factory().openWriter("zarr3:/home/john/tests/codeReview/sharded.zarr")) {

			N5Utils.save(img, zarr, 
					"highLevel", // dataset path
					new int[] { 16, 9 }, 	// shard size
					new int[] { 4, 3 }, 	// block size
					new ZstandardCompression());
		}
	}

	public static void midLevel() {

		final long[] imageSize = new long[] { 32, 27 };
		final int[] shardSize = new int[] { 16, 9 };
		final int[] blockSize = new int[] { 4, 3 };
		final int numBlockElements = Arrays.stream(blockSize).reduce(1, (x, y) -> x * y);

		try( final N5Writer zarr = new N5Factory().openWriter("zarr3:/home/john/tests/codeReview/sharded.zarr") ) {

			/*
			 * If you need control over everything, this is here.
			 */
			final ShardedDatasetAttributes attributes = new ShardedDatasetAttributes(
					imageSize,
					shardSize,
					blockSize,
					DataType.INT32,
					new Codec[]{
							// codecs applied to image data
							new BytesCodec(ByteOrder.BIG_ENDIAN)
					},
					new DeterministicSizeCodec[]{
							// codecs applied to the shard index, must not be compressors
							new BytesCodec(ByteOrder.LITTLE_ENDIAN), 
							new Crc32cChecksumCodec()
					},
					IndexLocation.START
			);

			// manually create a dataset
			zarr.createDataset("midLevel", attributes);

			// manually write a few blocks
			zarr.writeBlock("midLevel", attributes, 
					new IntArrayDataBlock( blockSize, new long[] {1,1}, generateArray(numBlockElements, x -> 1)));

			zarr.writeBlock("midLevel", attributes, 
					new IntArrayDataBlock( blockSize, new long[] {0,1}, generateArray(numBlockElements, x -> 2)));

			zarr.writeBlock("midLevel", attributes, 
					new IntArrayDataBlock( blockSize, new long[] {4,0}, generateArray(numBlockElements, x -> 3)));
		}

	}

	public static void midLevelBatch() {

		final String dset = "midLevelBatch";
		final long[] imageSize = new long[] { 32, 27 };
		final int[] shardSize = new int[] { 16, 9 };
		final int[] blockSize = new int[] { 4, 3 };
		final int numBlockElements = Arrays.stream(blockSize).reduce(1, (x, y) -> x * y);

		try( final N5Writer zarr = new N5Factory().openWriter("zarr3:/home/john/tests/codeReview/sharded.zarr") ) {

			/*
			 * If you need control over everything, this is here.
			 */
			final ShardedDatasetAttributes attributes = new ShardedDatasetAttributes(
					imageSize,
					shardSize,
					blockSize,
					DataType.INT32,
					new Codec[]{
							// codecs applied to image data
							new BytesCodec(ByteOrder.BIG_ENDIAN)
					},
					new DeterministicSizeCodec[]{
							// codecs applied to the shard index, must not be compressors
							new BytesCodec(ByteOrder.LITTLE_ENDIAN), 
							new Crc32cChecksumCodec()
					},
					IndexLocation.START
			);

			// manually create a dataset
			zarr.createDataset(dset, attributes);

			// Manually write several blocks
			// In this case, the blocks that belong to the same shard are combined before writing

			// should this be a collection?
			// alternatively, this could be a generator over blocks
			// eventually need to also pass an ExecutorService here

			// could also be a generator of a collection
			// we'd like to have an iterator over "writable units"
			// where the writable units are blocks are shards.
			// downstream code would be responsible for filling those units
			zarr.writeBlocks(dset, attributes, 
							new IntArrayDataBlock( blockSize, new long[] {1,1}, generateArray(numBlockElements, x -> 1)),
							new IntArrayDataBlock( blockSize, new long[] {0,1}, generateArray(numBlockElements, x -> 2)),
							new IntArrayDataBlock( blockSize, new long[] {4,0}, generateArray(numBlockElements, x -> 3))
					);

		}

	}

	public static void lowLevelVirtual() {

		final String dset = "lowLevelVirtual";
		final long[] imageSize = new long[] { 32, 27 };
		final int[] shardSize = new int[] { 16, 9 };
		final int[] blockSize = new int[] { 4, 3 };
		final int numBlockElements = Arrays.stream(blockSize).reduce(1, (x, y) -> x * y);

		try( final GsonKeyValueN5Writer zarr = (GsonKeyValueN5Writer)new N5Factory().openWriter("zarr3:/home/john/tests/codeReview/sharded.zarr") ) {

			final ShardedDatasetAttributes attributes = new ShardedDatasetAttributes(
					imageSize,
					shardSize,
					blockSize,
					DataType.INT32,
					new Codec[]{
							// codecs applied to image data
							new BytesCodec(ByteOrder.BIG_ENDIAN)
					},
					new DeterministicSizeCodec[]{
							// codecs applied to the shard index, must not be compressors
							new BytesCodec(ByteOrder.LITTLE_ENDIAN), 
							new Crc32cChecksumCodec()
					},
					IndexLocation.END
			);

			// manually create a dataset
			zarr.createDataset(dset, attributes);

			/*
			 * Programmer's reponsibility to create shards, and to determine
			 * which blocks go in which shard.
			 */
			final VirtualShard<int[]> shard00 = new VirtualShard<>(
				attributes, 
				new long[]{0,0},
				zarr.getKeyValueAccess(),
				zarr.absoluteDataBlockPath(dset, 0, 0) // path for this shard
			);

			// 

			// write to disk

			// the block location here could be relative to the shard
			shard00.writeBlock(new IntArrayDataBlock(blockSize, new long[] {1,1}, generateArray(numBlockElements, x -> 1)));
			// write to disk
			shard00.writeBlock(new IntArrayDataBlock(blockSize, new long[] {0,1}, generateArray(numBlockElements, x -> 2)));

			final VirtualShard<int[]> shard10 = new VirtualShard<>(
					attributes, 
					new long[]{1,0},
					zarr.getKeyValueAccess(),
					zarr.absoluteDataBlockPath(dset, 1, 0) // path for this shard
			);
			shard10.writeBlock(new IntArrayDataBlock( blockSize, new long[] {4,0}, generateArray(numBlockElements, x -> 3)));
		}
	}

	public static void lowLevelBatch() throws IOException {

		final String dset = "lowLevelBatch";
		final long[] imageSize = new long[] { 32, 27 };
		final int[] shardSize = new int[] { 16, 9 };
		final int[] blockSize = new int[] { 4, 3 };
		final int numBlockElements = Arrays.stream(blockSize).reduce(1, (x, y) -> x * y);

		try( final GsonKeyValueN5Writer zarr = (GsonKeyValueN5Writer)new N5Factory().openWriter("zarr3:/home/john/tests/codeReview/sharded.zarr") ) {

			final ShardedDatasetAttributes attributes = new ShardedDatasetAttributes(
					imageSize,
					shardSize,
					blockSize,
					DataType.INT32,
					new Codec[]{
							// codecs applied to image data
							new BytesCodec(ByteOrder.BIG_ENDIAN)
					},
					new DeterministicSizeCodec[]{
							// codecs applied to the shard index, must not be compressors
							new BytesCodec(ByteOrder.LITTLE_ENDIAN), 
							new Crc32cChecksumCodec()
					},
					IndexLocation.END
			);

			// manually create a dataset
			zarr.createDataset("lowLevelBatch", attributes);

			/*
			 * Programmer's reponsibility to create shards, and to determine
			 * which blocks go in which shard.
			 */
			final InMemoryShard<int[]> shard00 = new InMemoryShard<>(attributes, new long[] { 0, 0 });
			shard00.addBlock(new IntArrayDataBlock(blockSize, new long[] { 1, 1 }, generateArray(numBlockElements, x -> 1)));
			shard00.addBlock(new IntArrayDataBlock(blockSize, new long[] { 0, 1 }, generateArray(numBlockElements, x -> 2)));

			// write to disk
			shard00.write(zarr.getKeyValueAccess(), zarr.absoluteDataBlockPath(dset, 0, 0));

			final InMemoryShard<int[]> shard10 = new InMemoryShard<>( attributes, new long[]{1,0}); 
			shard10.addBlock(new IntArrayDataBlock( blockSize, new long[] {4,0}, generateArray(numBlockElements, x -> 3)));

			// write to disk
			shard10.write(zarr.getKeyValueAccess(), zarr.absoluteDataBlockPath(dset, 1, 0));
		}
	}

	public static int[] generateArray( int size ) {
		return IntStream.range(0, size).toArray();
	}
	
	public static int[] generateArray( int size, IntUnaryOperator f ) {
		return IntStream.range(0, size).map(f) .toArray();
	}
	
	public static ArrayImg<IntType, IntArray> generateData( long... size) {

		ArrayImg<IntType, IntArray> img = ArrayImgs.ints(size);
		int i = 0;
		final ArrayCursor<IntType> c = img.cursor();
		while (c.hasNext()) {
			c.next().set(i++);
		}
		return img;
	}


}

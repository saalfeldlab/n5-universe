/*-
 * #%L
 * Not HDF5
 * %%
 * Copyright (C) 2017 - 2025 Stephan Saalfeld
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.janelia.saalfeldlab.n5.universe.benchmarks;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.codec.BlockCodec;
import org.janelia.saalfeldlab.n5.codec.N5BlockCodecs;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@Warmup(iterations = 250, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 1000, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class BlockCodecBenchmarks {

	static final String FILL_RANDOM = "random";
	static final String FILL_SEQUENCE = "sequence";

	static final Random random = new Random(7777);

	@SuppressWarnings("rawtypes")
	BlockCodec codec;
	ReadData encodedBlock;
	long[] gridPosition;

	@Param(value = {BlockReadWriteBenchmarks.RAW_COMPRESSION, BlockReadWriteBenchmarks.GZIP_COMPRESSION,
			BlockReadWriteBenchmarks.LZ4_COMPRESSION, BlockReadWriteBenchmarks.XZ_COMPRESSION,
			BlockReadWriteBenchmarks.BLOSC_COMPRESSION, BlockReadWriteBenchmarks.ZSTD_COMPRESSION})
	protected String compressionType;

	@Param(value = {"int8", "int16", "int32", "int64", "float32", "float64"})
	protected String dataType;

	@Param(value = {"3"})
	protected int numDimensions;

	@Param(value = {"64"})
	protected int blockDim;

	@Param(value = {FILL_RANDOM, FILL_SEQUENCE})
	protected String fillType;

	public static void main(String[] args) throws RunnerException {

		final Options options = new OptionsBuilder()
				.include(BlockCodecBenchmarks.class.getSimpleName() + "\\.")
				.build();
		new Runner(options).run();
	}

	@Setup(Level.Trial)
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setup() throws Exception {

		final int[] blockSize = new int[numDimensions];
		Arrays.fill(blockSize, blockDim);
		gridPosition = new long[numDimensions];

		final DataType dtype = DataType.fromString(dataType);
		codec = N5BlockCodecs.create(dtype, BlockReadWriteBenchmarks.getCompression(compressionType));

		final DataBlock block = dtype.createDataBlock(blockSize, gridPosition);
		fillBlock(dtype, block);

		encodedBlock = codec.encode(block).materialize();
	}

	@Benchmark
	public void decodeBenchmark(Blackhole hole) throws Exception {

		hole.consume(codec.decode(encodedBlock, gridPosition));
	}

	@SuppressWarnings("rawtypes")
	private void fillBlock(DataType dtype, DataBlock blk) {

		if (fillType.equals(FILL_SEQUENCE)) {
			fillBlockSequence(dtype, blk);
		} else {
			fillBlockRandom(dtype, blk);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void fillBlockRandom(DataType dtype, DataBlock blk) {

		switch (dtype) {
		case INT32:
		case UINT32:
			fillRandom((int[])blk.getData());
			break;
		case FLOAT32:
			fillRandom((float[])blk.getData());
			break;
		case FLOAT64:
			fillRandom((double[])blk.getData());
			break;
		case INT16:
		case UINT16:
			fillRandom((short[])blk.getData());
			break;
		case INT64:
		case UINT64:
			fillRandom((long[])blk.getData());
			break;
		case INT8:
		case UINT8:
			random.nextBytes((byte[])blk.getData());
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("rawtypes")
	public static void fillBlockSequence(DataType dtype, DataBlock blk) {

		switch (dtype) {
		case INT32:
		case UINT32:
			fillSequence((int[])blk.getData());
			break;
		case FLOAT32:
			fillSequence((float[])blk.getData());
			break;
		case FLOAT64:
			fillSequence((double[])blk.getData());
			break;
		case INT16:
		case UINT16:
			fillSequence((short[])blk.getData());
			break;
		case INT64:
		case UINT64:
			fillSequence((long[])blk.getData());
			break;
		case INT8:
		case UINT8:
			fillSequence((byte[])blk.getData());
			break;
		default:
			break;
		}
	}

	public static void fillRandom(short[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = (short)random.nextInt();
	}

	public static void fillRandom(int[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = random.nextInt();
	}

	public static void fillRandom(long[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = random.nextLong();
	}

	public static void fillRandom(float[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = random.nextFloat();
	}

	public static void fillRandom(double[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = random.nextDouble();
	}

	public static void fillSequence(byte[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = (byte)i;
	}

	public static void fillSequence(short[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = (short)i;
	}

	public static void fillSequence(int[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = i;
	}

	public static void fillSequence(long[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = i;
	}

	public static void fillSequence(float[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = i;
	}

	public static void fillSequence(double[] arr) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = i;
	}
}

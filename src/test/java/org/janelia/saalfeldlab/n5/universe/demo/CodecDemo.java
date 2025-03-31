package org.janelia.saalfeldlab.n5.universe.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.IntUnaryOperator;

import org.janelia.saalfeldlab.n5.GsonUtils;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.codec.Codec;
import org.janelia.saalfeldlab.n5.serialization.NameConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CodecDemo {

	public static void main(String[] args) throws IOException {

		final String message = "my son is also named bort";

		encodeDecode(message);
		customCodecs(message);
		composeCodecs(message);
		serializeCodec();
	}

	public static void encodeDecode(final String message) throws IOException {
		final Codec.BytesCodec codec = new GzipCompression();

		// encode
		byte[] encodedData;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		OutputStream encodedOs = codec.encode(out);

		encodedOs.write(message.getBytes());
		encodedOs.close();
		encodedData = out.toByteArray();

		// decode
		byte[] decodedData = new byte[message.getBytes().length];
		ByteArrayInputStream is = new ByteArrayInputStream(encodedData);
		InputStream decodedIs = codec.decode(is);
		decodedIs.read(decodedData);

		System.out.println("\nGzip Codec:");
		System.out.println("original message: " + message);
		System.out.println("encoded messsage: " + new String(encodedData));
		System.out.println("decoded messsage: " + new String(decodedData));
	}

	public static void customCodecs(final String message) throws IOException {

		final FunctionCodec addCodec = new FunctionCodec( x -> x + 3, x -> x - 3);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStream encodedOut = addCodec.encode(out);
		encodedOut.write(message.getBytes());

		System.out.println("\nAdd Codec:");
		System.out.println(message);
		System.out.println(new String(out.toByteArray()));
	}

	public static void composeCodecs(final String message) throws IOException {

		final FunctionCodec subtractCodec = new FunctionCodec(x -> x - 32, x -> x + 32);
		final FunctionCodec noNegativesCodec = new FunctionCodec(x -> x > 0 ? x : 32, x -> x);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		OutputStream encodedOut = Codec.encode(out, noNegativesCodec, subtractCodec);
		encodedOut.write(message.getBytes());

		System.out.println("\nComposed Codec:");
		System.out.println(message);
		System.out.println(new String(out.toByteArray()));
	}

	public static void serializeCodec() throws IOException {

		final FancyCodec codec = new FancyCodec();

		final GsonBuilder gsonBuilder = new GsonBuilder();
		GsonUtils.registerGson(gsonBuilder);
		final Gson gson = gsonBuilder.create();

		System.out.println( gson.toJson(codec) );
	}

	/*
	 * Not actually useful. For demonstration purposes only.
	 */
	public static class FunctionCodec implements Codec.BytesCodec {

		private static final long serialVersionUID = 999L;

		public static final String TYPE = "add";

		private IntUnaryOperator encode;
		private IntUnaryOperator decode;

		public FunctionCodec(IntUnaryOperator encode, IntUnaryOperator decode) {
			this.encode = encode;
			this.decode = decode;
		}

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public InputStream decode(InputStream in) throws IOException {
			return new FilterInputStream(in) {

				public int read() throws IOException {
					return decode.applyAsInt(in.read());
				}
			};
		}

		@Override
		public OutputStream encode(OutputStream out) throws IOException {

			return new FilterOutputStream(out) {
				public void write(int b) throws IOException {
					out.write(encode.applyAsInt(b));
				}
			};
		}
	}

	@NameConfig.Name("fancy")
	public static class FancyCodec implements Codec.BytesCodec {

		private static final long serialVersionUID = -1785908861729837317L;

		@NameConfig.Parameter
		private final int rizz;

		@NameConfig.Parameter(optional = true)
		private final String swag;

		public FancyCodec() {
			this(99, "hella");
		}

		public FancyCodec(int rizz, String swag) {

			this.rizz = rizz;
			this.swag = swag;
		}

		@Override
		public String getType() {
			return "fancy";
		}

		@Override
		public InputStream decode(InputStream in) throws IOException {
			return in;
		}

		@Override
		public OutputStream encode(OutputStream out) throws IOException {
			return out;
		}

	}

}

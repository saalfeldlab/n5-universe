package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.universe.factory.spi.AmazonS3Options;
import org.janelia.saalfeldlab.n5.universe.factory.spi.HDF5Options;
import org.junit.Test;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The {@code kvr} counterpart of {@code universe.options.N5FactoryOptionsTest}.
 * <p>
 * Two things differ from the old test. The options are split along the two axes the design
 * rests on, so the fallback chain is asserted on {@link FormatOptions} rather than on the
 * top-level options; and {@link HDF5Options} has no {@code cacheAttributes}, since HDF5 has no
 * attribute cache to configure. The {@link BackendOptions} half is new — nothing covered it.
 */
public class N5FactoryOptionsTest {

	/** The format options that share the fallback chain through {@code gsonBuilder}. */
	private static Object[] gsonFallbackSharers(final FormatOptions format) {

		return new Object[]{format.n5(), format.hdf5(), format.zarr2(), format.zarr3()};
	}

	private static GsonBuilder gsonBuilderOf(final Object options) {

		if (options instanceof N5Options)
			return ((N5Options)options).gsonBuilder();
		return ((HDF5Options)options).gsonBuilder();
	}

	@Test
	public void defaults() {

		final FormatOptions format = new N5FactoryOptions().format();

		for (final Object options : gsonFallbackSharers(format))
			assertNotNull(gsonBuilderOf(options));

		/* HDF5Options has no cacheAttributes */
		assertTrue(format.n5().cacheAttributes());
		assertTrue(format.zarr2().cacheAttributes());
		assertTrue(format.zarr3().cacheAttributes());

		assertEquals(".", format.zarr2().dimensionSeparator());
		assertEquals("/", format.zarr3().dimensionSeparator());

		assertTrue(format.zarr2().mapN5DatasetAttributes());
		assertTrue(format.zarr2().mergeAttributes());

		assertArrayEquals(new int[]{64, 64, 64, 1, 1}, format.hdf5().defaultBlockSize());
		assertFalse(format.hdf5().overrideBlockSize());
	}

	@Test
	public void sharedOptionsPropagate() {

		final FormatOptions format = new N5FactoryOptions().format();

		final GsonBuilder initial = format.n5().gsonBuilder();
		for (final Object options : gsonFallbackSharers(format))
			assertSame(initial, gsonBuilderOf(options));

		format.cacheAttributes(false);
		final GsonBuilder shared = new GsonBuilder();
		format.gsonBuilder(shared);

		for (final Object options : gsonFallbackSharers(format))
			assertSame(shared, gsonBuilderOf(options));

		assertFalse(format.n5().cacheAttributes());
		assertFalse(format.zarr2().cacheAttributes());
		assertFalse(format.zarr3().cacheAttributes());
	}

	@Test
	public void perFormatOverrides() {

		final GsonBuilder shared = new GsonBuilder();
		final FormatOptions format = new N5FactoryOptions().format()
				.cacheAttributes(true)
				.gsonBuilder(shared);

		format.zarr2().cacheAttributes(false);

		assertFalse(format.zarr2().cacheAttributes());
		assertTrue(format.n5().cacheAttributes());
		assertTrue(format.zarr3().cacheAttributes());

		final GsonBuilder perFormat = new GsonBuilder();
		format.zarr3(options -> options.gsonBuilder(perFormat));

		assertSame(perFormat, format.zarr3().gsonBuilder());
		assertSame(shared, format.n5().gsonBuilder());
		assertSame(shared, format.hdf5().gsonBuilder());
		assertSame(shared, format.zarr2().gsonBuilder());
	}

	@Test
	public void consumerStyleConfigUsesSameInstance() {

		final N5FactoryOptions options = new N5FactoryOptions();
		final HDF5Options[] seen = new HDF5Options[1];

		final N5FactoryOptions returned = options.hdf5(hdf5 -> seen[0] = hdf5);

		assertSame(options, returned);
		assertSame(options.format().hdf5(), seen[0]);
	}

	/** The flat methods downstream calls must reach the same objects as the two-axis ones. */
	@Test
	public void flatMethodsDelegate() {

		final N5FactoryOptions options = new N5FactoryOptions();

		final GsonBuilder gson = new GsonBuilder();
		options.gsonBuilder(gson).cacheAttributes(false);
		assertSame(gson, options.format().n5().gsonBuilder());
		assertFalse(options.format().n5().cacheAttributes());

		options.zarr2(zarr2 -> zarr2.dimensionSeparator("x"));
		assertEquals("x", options.format().zarr2().dimensionSeparator());

		final Supplier<com.google.cloud.storage.Storage> gcsClient = () -> null;
		options.googleCloud(gc -> gc.clientProvider(gcsClient));
		assertSame(gcsClient, options.backend().googleCloud().clientProvider());

		options.googleCloudProjectId("a-project");
		assertEquals("a-project", options.backend().googleCloud().projectId());

		final Function<URI, software.amazon.awssdk.services.s3.S3Client> s3Client = uri -> null;
		options.s3(s3 -> s3.clientProvider(s3Client));
		assertSame(s3Client, options.backend().s3().clientProvider());

		options.http(http -> http.readTimeoutMillis(123));
		assertEquals(123, options.backend().http().readTimeoutMillis());
	}

	/**
	 * The backend half is pure data with no fallback chain: each backend is independent, and
	 * everything defaults to "not configured". This is what lets core name these classes
	 * without depending on any cloud SDK.
	 */
	@Test
	public void backendOptionsAreIndependentData() {

		final BackendOptions backend = new N5FactoryOptions().backend();

		assertNull(backend.s3().clientProvider());
		assertNull(backend.s3().builderConfig());
		assertNull(backend.googleCloud().clientProvider());
		assertNull(backend.googleCloud().builderConfig());
		assertNull(backend.googleCloud().projectId());
		assertFalse("the expensive S3 client probe is opt-in", backend.probeHttpWithS3Client());

		assertEquals(5000, backend.http().connectionTimeoutMillis());
		assertEquals(5000, backend.http().readTimeoutMillis());

		/* configuring one backend leaves the others alone */
		backend.googleCloud(gc -> gc.projectId("p"));
		assertNull(backend.s3().clientProvider());
		assertEquals(5000, backend.http().readTimeoutMillis());

		/* the consumer form hands out the same instance the accessor returns */
		final AmazonS3Options[] seen = new AmazonS3Options[1];
		assertSame(backend, backend.s3(s3 -> seen[0] = s3));
		assertSame(backend.s3(), seen[0]);
	}
}

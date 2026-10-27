package org.janelia.saalfeldlab.n5.universe;

import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.factory.options.N5FactoryOptions;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * {@link N5Factory} and {@link N5FactoryWithCache} in this package only forward to
 * {@link org.janelia.saalfeldlab.n5.universe.factory.N5Factory} and
 * {@link org.janelia.saalfeldlab.n5.universe.factory.N5FactoryWithCache}, which have their own tests.
 * This checks the forwarding: that configuration reaches the delegate, that the deprecated
 * methods kept here still work, and that the cache is the delegate's.
 */
public class N5FactoryForwardingTest {

	private static String tempLocation(final String suffix) throws IOException {

		final Path dir = Files.createTempDirectory("n5-factory-forwarding-");
		dir.toFile().deleteOnExit();
		return dir.resolve("container" + suffix).toString();
	}

	@Test
	public void optionsAreTheDelegates() {

		final N5Factory factory = new N5Factory();
		assertSame(factory.getFactory().getOptions(), factory.getOptions());

		/* deprecated setters write into the delegate's options */
		factory.zarrDimensionSeparator("x").cacheAttributes(false).hdf5OverrideBlockSize(true);
		final N5FactoryOptions options = factory.getFactory().getOptions();
		assertEquals("x", options.format().zarr2().dimensionSeparator());
		assertFalse(options.format().n5().cacheAttributes());
		assertTrue(options.format().hdf5().overrideBlockSize());

		final N5FactoryOptions replaced = new N5FactoryOptions();
		assertSame(factory, factory.setOptions(replaced));
		assertSame(replaced, factory.getFactory().getOptions());
	}

	@Test
	public void opensContainers() throws IOException {

		final N5Factory factory = new N5Factory();

		final String n5 = tempLocation(".n5");
		try (final N5Writer writer = factory.openWriter(n5)) {
			assertTrue(writer instanceof N5KeyValueWriter);
			writer.setAttribute("/", "key", "value");
		}
		try (final N5Reader reader = factory.openReader(n5)) {
			assertTrue(reader instanceof N5KeyValueReader);
			assertEquals("value", reader.getAttribute("/", "key", String.class));
		}

		final String zarr3 = tempLocation("");
		try (final N5Writer writer = factory.openWriter(StorageFormat.ZARR3, zarr3)) {
			assertTrue(writer instanceof ZarrV3KeyValueWriter);
		}
		try (final N5Reader reader = factory.openReader("zarr3:" + zarr3)) {
			assertTrue(reader instanceof ZarrV3KeyValueReader);
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedOpenMethods() throws IOException {

		final N5Factory factory = new N5Factory();

		final String n5 = tempLocation(".n5");
		try (final N5Writer writer = factory.openFSWriter(n5)) {
			assertTrue(writer instanceof N5KeyValueWriter);
		}
		try (final N5Reader reader = factory.openReader(KeyValueAccessBackend.FILE, "n5:" + n5)) {
			assertTrue(reader instanceof N5KeyValueReader);
		}
		try (final N5Reader reader = factory.openFileSystemReader(n5)) {
			assertTrue(reader instanceof N5KeyValueReader);
		}
	}

	@Test
	public void cacheIsTheDelegates() throws IOException {

		final N5FactoryWithCache factory = new N5FactoryWithCache();
		final String n5 = tempLocation(".n5");

		final N5Writer writer = factory.openWriter(n5);
		assertSame(writer, factory.getFactory().openWriter(n5));
		assertSame(writer, factory.openReaderAllowCachedWriter(n5));

		assertTrue(factory.remove(n5));
		assertNotSame(writer, factory.openWriter(n5));

		factory.clear();
		assertFalse(factory.remove(n5));
	}
}

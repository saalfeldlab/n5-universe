package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class N5FactoryOptionsTest {

    private static AbstractN5Builder @NonNull [] getBuilders(N5FactoryOptions options) {
        return new AbstractN5Builder[]{options.getN5Builder(), options.getHdf5Builder(), options.getZarr2Builder(), options.getZarr3Builder()};
    }

    @Test
    public void defaults() {
        final N5FactoryOptions options = new N5FactoryOptions();

        AbstractN5Builder[] builders = getBuilders(options);
        for (AbstractN5Builder builder : builders) {
            assertTrue(builder.getCacheAttributes());
            assertNotNull(builder.getGsonBuilder());
        }

        assertEquals(".", options.getZarr2Builder().getDimensionSeparator());
        assertEquals("/", options.getZarr3Builder().getDimensionSeparator());

        assertTrue(options.getZarr2Builder().getMapN5DatasetAttributes());
        assertTrue(options.getZarr2Builder().getMergeAttributes());

        assertArrayEquals(new int[]{64, 64, 64, 1, 1}, options.getHdf5Builder().getDefaultBlockSize());
        assertFalse(options.getHdf5Builder().getOverrideBlockSize());
    }


    @Test
    public void sharedOptionsPropagate() {
        final N5FactoryOptions options = new N5FactoryOptions();
        AbstractN5Builder[] builders = getBuilders(options);

        GsonBuilder initFromBuilder = options.getN5Builder().getGsonBuilder();

        for (AbstractN5Builder builder : builders) {
            assertTrue(builder.getCacheAttributes());
            assertSame(initFromBuilder, builder.getGsonBuilder());
        }

        options.cacheAttributes(false);
        GsonBuilder shared = new GsonBuilder();
        options.gsonBuilder(shared);

        for (AbstractN5Builder builder : builders) {
            assertFalse(builder.getCacheAttributes());
            assertSame(shared, builder.getGsonBuilder());
        }
    }

    @Test
    public void perFormatOverrides() {
        GsonBuilder shared = new GsonBuilder();
        final N5FactoryOptions options = new N5FactoryOptions()
                .cacheAttributes(true)
                .gsonBuilder(shared);

        options.getZarr2Builder().cacheAttributes(false);

        assertFalse(options.getZarr2Builder().getCacheAttributes());
        assertTrue(options.getN5Builder().getCacheAttributes());
        assertTrue(options.getHdf5Builder().getCacheAttributes());
        assertTrue(options.getZarr3Builder().getCacheAttributes());

        final GsonBuilder perFormat = new GsonBuilder();
        options.zarr3(opts -> opts.gsonBuilder(perFormat));

        assertSame(perFormat, options.getZarr3Builder().getGsonBuilder());
        assertSame(shared, options.getN5Builder().getGsonBuilder());
        assertSame(shared, options.getHdf5Builder().getGsonBuilder());
        assertSame(shared, options.getZarr2Builder().getGsonBuilder());
    }

    @Test
    public void consumerStyleConfigUsesSameBuilderInstance() {
        final N5FactoryOptions options = new N5FactoryOptions();
        final HDF5Builder[] seen = new HDF5Builder[1];

        final N5FactoryOptions returned = options.hdf5(builder -> seen[0] = builder);

        assertSame(options, returned);
        assertSame(options.getHdf5Builder(), seen[0]);
    }
}

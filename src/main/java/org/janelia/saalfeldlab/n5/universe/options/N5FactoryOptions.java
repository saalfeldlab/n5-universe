package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;

import java.util.function.Consumer;

public class N5FactoryOptions {

    private final AbstractN5Builder sharedOptions = new DefaultSharedBuilder();

    private final N5Builder n5Builder = new N5Builder(sharedOptions);
    private final HDF5Builder hdf5Builder = new HDF5Builder(sharedOptions);
    private final Zarr2Builder zarr2Builder = new Zarr2Builder(sharedOptions);
    private final Zarr3Builder zarr3Builder = new Zarr3Builder(sharedOptions);

    /**
     * Configure {@link N5Builder} to be used when the factory accesses an N5 container.
     *
     * @param configure a {@code Consumer<N5Builder>} and performs custom configuration on it
     * @return the current instance of {@code N5FactoryOptions}
     */
    public N5FactoryOptions n5(Consumer<N5Builder> configure) {

        configure.accept(getN5Builder());
        return this;
    }

    /**
     * Configure {@link HDF5Builder} to be used when the factory accesses an HDF5 container.
     *
     * @param configure a {@code Consumer<HDF5Builder>} and performs custom configuration on it
     * @return the current instance of {@code N5FactoryOptions}
     */
    public N5FactoryOptions hdf5(Consumer<HDF5Builder> configure) {

        configure.accept(getHdf5Builder());
        return this;
    }

    /**
     * Configure {@link Zarr2Builder} to be used when the factory accesses a Zarr2 container.
     *
     * @param configure a {@code Consumer<Zarr2Builder>} and performs custom configuration on it
     * @return the current instance of {@code N5FactoryOptions}
     */
    public N5FactoryOptions zarr2(Consumer<Zarr2Builder> configure) {

        configure.accept(getZarr2Builder());
        return this;
    }

    /**
     * Configure {@link Zarr3Builder} to be used when the factory accesses a Zarr3 container.
     *
     * @param configure a {@code Consumer<Zarr3Builder>} and performs custom configuration on it
     * @return the current instance of {@code N5FactoryOptions}
     */
    public N5FactoryOptions zarr3(Consumer<Zarr3Builder> configure) {

        configure.accept(getZarr3Builder());
        return this;
    }

    /**
     * Set the attributes caching behavior. Will be overridden by any format-specific configurations
     *
     * @param cacheAttributes flag to use for otherwise unconfigured N5Readers
     * @return this
     */
    public N5FactoryOptions cacheAttributes(boolean cacheAttributes) {

        sharedOptions.cacheAttributes(cacheAttributes);
        return this;
    }

    /**
     * Set the default GsonBuilder. Will be overridden by any format-specific configurations
     *
     * @param gsonBuilder to use for otherwise unconfigured N5Readers
     * @return this
     */
    public N5FactoryOptions gsonBuilder(GsonBuilder gsonBuilder) {

        sharedOptions.gsonBuilder(gsonBuilder);
        return this;
    }


    public N5Builder getN5Builder() {
        return n5Builder;
    }

    public HDF5Builder getHdf5Builder() {
        return hdf5Builder;
    }

    public Zarr2Builder getZarr2Builder() {
        return zarr2Builder;
    }

    public Zarr3Builder getZarr3Builder() {
        return zarr3Builder;
    }

    private static class DefaultSharedBuilder extends AbstractN5Builder {
        public DefaultSharedBuilder() {
            super(null);
            cacheAttributes = true;
            gsonBuilder = new GsonBuilder();
        }

        @Override
        public DefaultSharedBuilder cacheAttributes(boolean cacheAttributes) {
            this.cacheAttributes = cacheAttributes;
            return this;
        }

        @Override
        public DefaultSharedBuilder gsonBuilder(GsonBuilder gsonBuilder) {
            this.gsonBuilder = gsonBuilder;
            return this;
        }
    }
}

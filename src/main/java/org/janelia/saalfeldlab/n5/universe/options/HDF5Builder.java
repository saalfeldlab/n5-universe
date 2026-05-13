package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;

@SuppressWarnings("UnusedReturnValue")
public class HDF5Builder extends AbstractN5Builder {

    protected int[] defaultBlockSize = new int[]{64, 64, 64, 1, 1};
    protected boolean overrideBlockSize = false;

    HDF5Builder(AbstractN5Builder sharedOptions) {
        super(sharedOptions);
    }

    @Override
    public HDF5Builder cacheAttributes(boolean cacheAttributes) {
        this.cacheAttributes = cacheAttributes;
        return this;
    }

    @Override
    public HDF5Builder gsonBuilder(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
        return this;
    }

    public HDF5Builder defaultBlockSize(int[] defaultBlockSize) {
        this.defaultBlockSize = defaultBlockSize.clone();
        return this;
    }

    public HDF5Builder overrideBlockSize(boolean overrideBlockSize) {
        this.overrideBlockSize = overrideBlockSize;
        return this;
    }

    public int[] getDefaultBlockSize() {
        return defaultBlockSize.clone();
    }

    public boolean getOverrideBlockSize() {
        return overrideBlockSize;
    }

    public N5HDF5Writer buildWriter(String containerLocation) {
        return new N5HDF5Writer(containerLocation, getOverrideBlockSize(), getGsonBuilder(), getDefaultBlockSize());
    }

    public N5HDF5Reader buildReader(String containerPath) {
        return new N5HDF5Reader(containerPath, getOverrideBlockSize(), getGsonBuilder(), getDefaultBlockSize());
    }
}

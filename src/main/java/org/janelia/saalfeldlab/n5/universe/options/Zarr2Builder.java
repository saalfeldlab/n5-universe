package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;

@SuppressWarnings("UnusedReturnValue")
public class Zarr2Builder extends ZarrBuilder {

    static final String DEFAULT_DIMENSION_SEPARATOR = ".";

    protected boolean mapN5DatasetAttributes = true;
    protected boolean mergeAttributes = true;

    Zarr2Builder(AbstractN5Builder sharedOptions) {
        super(sharedOptions, DEFAULT_DIMENSION_SEPARATOR);
    }

    public boolean getMapN5DatasetAttributes() {
        return mapN5DatasetAttributes;
    }

    public boolean getMergeAttributes() {
        return mergeAttributes;
    }

    @Override
    public Zarr2Builder cacheAttributes(boolean cacheAttributes) {
        this.cacheAttributes = cacheAttributes;
        return this;
    }

    @Override
    public Zarr2Builder gsonBuilder(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
        return this;
    }

    @Override
    public Zarr2Builder dimensionSeparator(String dimensionSeparator) {
        this.dimensionSeparator = dimensionSeparator;
        return this;
    }

    public Zarr2Builder mapN5DatasetAttributes(boolean mapN5DatasetAttributes) {
        this.mapN5DatasetAttributes = mapN5DatasetAttributes;
        return this;
    }

    public Zarr2Builder mergeAttributes(boolean mergeAttributes) {
        this.mergeAttributes = mergeAttributes;
        return this;
    }

    public ZarrKeyValueWriter buildWriter(KeyValueAccess access, String containerLocation) {
        return new ZarrKeyValueWriter(access, containerLocation, getGsonBuilder(), getMapN5DatasetAttributes(), getMergeAttributes(), getDimensionSeparator(), getCacheAttributes());
    }

    public ZarrKeyValueReader buildReader(KeyValueAccess access, String containerLocation) {
        return new ZarrKeyValueReader(access, containerLocation, getGsonBuilder(), getMapN5DatasetAttributes(), getMergeAttributes(), getCacheAttributes());
    }


}

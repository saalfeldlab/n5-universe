package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;

@SuppressWarnings("UnusedReturnValue")
public class Zarr3Builder extends ZarrBuilder {

    static final String DEFAULT_DIMENSION_SEPARATOR = "/";

    Zarr3Builder(AbstractN5Builder sharedOptions) {
        super(sharedOptions, DEFAULT_DIMENSION_SEPARATOR);
    }

    @Override
    public Zarr3Builder cacheAttributes(boolean cacheAttributes) {
        this.cacheAttributes = cacheAttributes;
        return this;
    }

    @Override
    public Zarr3Builder gsonBuilder(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
        return this;
    }

    @Override
    public Zarr3Builder dimensionSeparator(String dimensionSeparator) {
        this.dimensionSeparator = dimensionSeparator;
        return this;
    }

    public ZarrV3KeyValueWriter buildWriter(KeyValueRoot keyValueRoot) {
        ZarrV3KeyValueWriter writer = new ZarrV3KeyValueWriter(keyValueRoot, getGsonBuilder(), getCacheAttributes());
        writer.setDimensionSeparator(getDimensionSeparator());
        return writer;
    }

    public ZarrV3KeyValueReader buildReader(KeyValueRoot keyValueRoot) {
        ZarrV3KeyValueReader reader = new ZarrV3KeyValueReader(keyValueRoot, getGsonBuilder(), getCacheAttributes());
        reader.setDimensionSeparator(getDimensionSeparator());
        return reader;
    }

}

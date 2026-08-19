package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5KeyValueWriter;

@SuppressWarnings("UnusedReturnValue")
public class N5Builder extends AbstractN5Builder {

    N5Builder(AbstractN5Builder sharedOptions) {
        super(sharedOptions);
    }

    @Override
    public N5Builder cacheAttributes(boolean cacheAttributes) {
        this.cacheAttributes = cacheAttributes;
        return this;
    }

    @Override
    public N5Builder gsonBuilder(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
        return this;
    }

    public N5KeyValueWriter buildWriter(KeyValueRoot keyValueRoot) {
        return new N5KeyValueWriter(keyValueRoot, getGsonBuilder(), getCacheAttributes());
    }

    public N5KeyValueReader buildReader(KeyValueRoot keyValueRoot) {
        return new N5KeyValueReader(keyValueRoot, getGsonBuilder(), getCacheAttributes());
    }
}

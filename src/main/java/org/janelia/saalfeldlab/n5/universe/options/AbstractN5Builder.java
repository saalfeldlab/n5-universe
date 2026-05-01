package org.janelia.saalfeldlab.n5.universe.options;

import com.google.gson.GsonBuilder;

@SuppressWarnings("UnusedReturnValue")
public abstract class AbstractN5Builder {

    protected Boolean cacheAttributes = null;
    protected GsonBuilder gsonBuilder = null;

    final protected AbstractN5Builder sharedOptions;

    AbstractN5Builder(AbstractN5Builder sharedOptions) {
        this.sharedOptions = sharedOptions;
    }

    public abstract AbstractN5Builder cacheAttributes(boolean cacheAttributes);

    public abstract AbstractN5Builder gsonBuilder(GsonBuilder gsonBuilder);

    public boolean getCacheAttributes() {
        return cacheAttributes != null ? cacheAttributes : sharedOptions.getCacheAttributes();
    }

    public GsonBuilder getGsonBuilder() {
        return gsonBuilder != null ? gsonBuilder : sharedOptions.getGsonBuilder();
    }
}



package org.janelia.saalfeldlab.n5.universe.factory.spi;

import com.google.gson.GsonBuilder;
import java.util.function.Supplier;

public class HDF5Options {

	private final Supplier<GsonBuilder> fallbackGsonBuilder;

	private GsonBuilder gsonBuilder;
	private int[] defaultBlockSize = new int[]{64, 64, 64, 1, 1};
    private boolean overrideBlockSize = false;

	public HDF5Options(final Supplier<GsonBuilder> fallbackGsonBuilder) {
		this.fallbackGsonBuilder = fallbackGsonBuilder;
	}

	public HDF5Options gsonBuilder(final GsonBuilder gsonBuilder) {
		this.gsonBuilder = gsonBuilder;
		return this;
	}

	public GsonBuilder gsonBuilder() {
		return gsonBuilder != null ? gsonBuilder : fallbackGsonBuilder.get();
	}

	public HDF5Options defaultBlockSize(int[] defaultBlockSize) {
        this.defaultBlockSize = defaultBlockSize.clone();
        return this;
    }

	public int[] defaultBlockSize() {
		return defaultBlockSize;
	}

	public HDF5Options overrideBlockSize(boolean overrideBlockSize) {
        this.overrideBlockSize = overrideBlockSize;
        return this;
    }

    public boolean overrideBlockSize() {
        return overrideBlockSize;
    }
}

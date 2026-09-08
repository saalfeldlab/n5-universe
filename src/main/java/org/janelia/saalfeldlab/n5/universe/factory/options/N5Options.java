package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.gson.GsonBuilder;

public class N5Options {

	private final FallbackN5Options fallback;

	private Boolean cacheAttributes;
	private GsonBuilder gsonBuilder;

	N5Options(final FallbackN5Options fallback) {
		this.fallback = fallback;
	}

	public N5Options cacheAttributes(final boolean cacheAttributes) {
		this.cacheAttributes = cacheAttributes;
		return this;
	}

	public boolean cacheAttributes() {
		return cacheAttributes != null ? cacheAttributes : fallback.cacheAttributes();
	}

	public N5Options gsonBuilder(final GsonBuilder gsonBuilder) {
		this.gsonBuilder = gsonBuilder;
		return this;
	}

	public GsonBuilder gsonBuilder() {
		return gsonBuilder != null ? gsonBuilder : fallback.gsonBuilder();
	}
}

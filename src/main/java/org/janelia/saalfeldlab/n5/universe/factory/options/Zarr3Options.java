package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.gson.GsonBuilder;

public class Zarr3Options extends N5Options {

	private static final String DEFAULT_DIMENSION_SEPARATOR = "/";

	private String dimensionSeparator = DEFAULT_DIMENSION_SEPARATOR;

	Zarr3Options(final FallbackN5Options fallback) {
		super(fallback);
	}

	@Override
	public Zarr3Options cacheAttributes(final boolean cacheAttributes) {
		super.cacheAttributes(cacheAttributes);
		return this;
	}

	@Override
	public Zarr3Options gsonBuilder(final GsonBuilder gsonBuilder) {
		super.gsonBuilder(gsonBuilder);
		return this;
	}

	public Zarr3Options dimensionSeparator(final String dimensionSeparator) {
		this.dimensionSeparator = dimensionSeparator;
		return this;
	}

	public String dimensionSeparator() {
		return dimensionSeparator;
	}
}

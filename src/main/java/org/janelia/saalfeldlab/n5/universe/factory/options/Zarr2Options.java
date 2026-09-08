package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.gson.GsonBuilder;

public class Zarr2Options extends N5Options {

	private static final String DEFAULT_DIMENSION_SEPARATOR = ".";

	private String dimensionSeparator = DEFAULT_DIMENSION_SEPARATOR;
	private boolean mapN5DatasetAttributes = true;
	private boolean mergeAttributes = true;

	Zarr2Options(final FallbackN5Options fallback) {
		super(fallback);
	}

	@Override
	public Zarr2Options cacheAttributes(final boolean cacheAttributes) {
		super.cacheAttributes(cacheAttributes);
		return this;
	}

	@Override
	public Zarr2Options gsonBuilder(final GsonBuilder gsonBuilder) {
		super.gsonBuilder(gsonBuilder);
		return this;
	}

	public Zarr2Options dimensionSeparator(final String dimensionSeparator) {
		this.dimensionSeparator = dimensionSeparator;
		return this;
	}

	public String dimensionSeparator() {
		return dimensionSeparator;
	}

	public Zarr2Options mapN5DatasetAttributes(final boolean mapN5DatasetAttributes) {
		this.mapN5DatasetAttributes = mapN5DatasetAttributes;
		return this;
	}

	public boolean mapN5DatasetAttributes() {
		return mapN5DatasetAttributes;
	}

	public Zarr2Options mergeAttributes(final boolean mergeAttributes) {
		this.mergeAttributes = mergeAttributes;
		return this;
	}

	public boolean mergeAttributes() {
		return mergeAttributes;
	}
}

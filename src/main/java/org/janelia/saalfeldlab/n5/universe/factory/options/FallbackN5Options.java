package org.janelia.saalfeldlab.n5.universe.factory.options;

import com.google.gson.GsonBuilder;

class FallbackN5Options {

	private boolean cacheAttributes = true;
	private GsonBuilder gsonBuilder = new GsonBuilder();

	public void cacheAttributes(boolean cacheAttributes) {
		this.cacheAttributes = cacheAttributes;
	}

	public void gsonBuilder(GsonBuilder gsonBuilder) {
		this.gsonBuilder = gsonBuilder;
	}

	public boolean cacheAttributes() {
		return cacheAttributes;
	}

	public GsonBuilder gsonBuilder() {
		return gsonBuilder;
	}
}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;

public class NgffSceneMetadata implements N5Metadata {

	private final String path;
	private final NgffScene scene;

	public NgffSceneMetadata(final String path, final NgffScene scene) {
		this.path = path;
		this.scene = scene;
	}

	@Override
	public String getPath() {
		return path;
	}

	public NgffScene getScene() {
		return scene;
	}

}

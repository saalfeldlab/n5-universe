package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.scene;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;

/**
 * Represents a "scene" that represents multiple images or other objects in a
 * one or more common coordinate system via a set of coordinate transformations.
 */
public class NgffScene {
	
	public static final String SCENE_KEY = "scene";

	private final CoordinateTransformation[] coordinateTransformations;

	private final CoordinateSystem[] coordinateSystems;

	public NgffScene() {
		this(null, null);
	}

	public NgffScene(final CoordinateTransformation[] coordinateTransformations) {
		this(coordinateTransformations, null);
	}

	public NgffScene(
			final CoordinateTransformation[] coordinateTransformations,
			final CoordinateSystem[] coordinateSystems) {

		this.coordinateTransformations = coordinateTransformations;
		this.coordinateSystems = coordinateSystems;
	}

	public CoordinateTransformation[] getCoordinateTransformations() {
		return coordinateTransformations;
	}

	public CoordinateSystem[] getCoordinateSystems() {
		return coordinateSystems;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import net.imglib2.realtransform.AffineGet;

public abstract class AbstractCoordinateTransformation<T extends AffineGet> implements CoordinateTransformation<T>
{
	protected final String type;

	public AbstractCoordinateTransformation(final String type) {
		this.type = type;
	}

	public abstract T getTransform();

	public String getType() {
		return type;
	}

}

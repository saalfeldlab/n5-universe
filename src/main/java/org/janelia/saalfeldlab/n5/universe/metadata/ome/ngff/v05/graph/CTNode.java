package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph;

import java.util.ArrayList;
import java.util.List;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

/**
 * A node in a {@link TransformGraph}.
 *
 * Edges are directed with this node as their base.
 *
 * @author John Bogovic
 */
public class CTNode {

	private final CoordinateSystem coordinateSystem;

	private final List<CoordinateTransform<?>> edges;

	public CTNode(final CoordinateSystem space) {

		this(space, new ArrayList<CoordinateTransform<?>>());
	}

	public CTNode(final CoordinateSystem coordinateSystem, final List<CoordinateTransform<?>> edges) {

		this.coordinateSystem = coordinateSystem;
		this.edges = edges;
	}

	public CoordinateSystem coordinateSystem() {

		return coordinateSystem;
	}

	public List<CoordinateTransform<?>> edges() {

		return edges;
	}

	@Override
	public boolean equals(final Object other) {

		return coordinateSystem.equals(other);
	}

}

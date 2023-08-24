package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph;

import java.util.ArrayList;
import java.util.List;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;

/**
 * A node in a {@link TransformGraph}.
 *
 * Edges are directed with this node as their base.
 *
 * @author John Bogovic
 */
public class CTNode
{
	private final CoordinateSystem space;

	private final List< CoordinateTransform<?> > edges;

	public CTNode( final CoordinateSystem space )
	{
		this( space, new ArrayList< CoordinateTransform<?> >() );
	}

	public CTNode( final CoordinateSystem space, final List< CoordinateTransform<?> > edges )
	{
		this.space = space;
		this.edges = edges;
	}

	public CoordinateSystem space()
	{
		return space;
	}

	public List<CoordinateTransform<?>> edges()
	{
		return edges;
	}

	@Override
	public boolean equals( final Object other )
	{
		return space.equals(other);
	}

}

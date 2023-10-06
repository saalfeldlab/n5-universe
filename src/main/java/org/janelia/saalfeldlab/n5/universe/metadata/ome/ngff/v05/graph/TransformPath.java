package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph;

import java.util.LinkedList;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

public class TransformPath {

	private final String start;

	private final TransformPath parentPath;

	private final CoordinateTransform<?> transform;

	private final String end;

	public TransformPath( final CoordinateTransform<?> transform ) {

		this.start = transform.getInput();
		this.transform = transform;
		this.end = transform.getOutput();
		this.parentPath = null;
	}

	public TransformPath( final TransformPath parentPath, final CoordinateTransform<?> transform ) {

		this.start = parentPath.getStart();
		this.parentPath = parentPath;
		this.transform = transform;
		this.end = transform.getOutput();
	}

	public String getStart()
	{
		return start;
	}

	public String getEnd()
	{
		return end;
	}

	public double getCost()
	{
		// consider this in the future
		// return flatTransforms().stream().mapToDouble( Transform::getCost ).sum();
		return 1.0;
	}

	/**
	 * Does this path run through the given space.
	 *
	 * @param cs the coordinate system
	 * @return true if this path contains space
	 */
	public boolean hasSpace( final CoordinateSystem cs )
	{
		if ( start.equals( cs.getName() ) || end.equals( cs.getName() ))
			return true;

		if( parentPath != null )
			return parentPath.hasSpace( cs );

		return false;
	}

	public List<CoordinateTransform<?>> flatTransforms()
	{
		final LinkedList<CoordinateTransform<?>> flatTransforms = new LinkedList<>();
		flatTransforms( flatTransforms );
		return flatTransforms;
	}

	public RealTransform totalTransform( final N5Reader n5, final TransformGraph g )
	{
		final RealTransformSequence total = new RealTransformSequence();
		flatTransforms().forEach( t ->  {
				if( t instanceof SequenceCoordinateTransform)
				{
					final SequenceCoordinateTransform s = (SequenceCoordinateTransform)t;
					if( s.isAffine())
					{
						final int nd = g.getInput( t ).getAxes().length;
						total.add( TransformUtils.toAffine( s, nd ));
					}
					else
						total.add( t.getTransform() );
				}
				else
				{
					total.add( ((RealTransform)t.getTransform( n5 )));
				}
			}
			);
		return total;
	}

	public RealTransform totalTransform( final N5Reader n5 )
	{
		final RealTransformSequence total = new RealTransformSequence();
		flatTransforms().forEach( t -> total.add( ((RealTransform)t.getTransform( n5 ))));
		return total;
	}

	public RealTransform totalTransform()
	{
		final RealTransformSequence total = new RealTransformSequence();
		flatTransforms().forEach( t -> total.add( ((RealTransform)t.getTransform())));
		return total;
	}

	public AffineTransform3D totalAffine3D( final N5Reader n5 )
	{
		return Common.toAffine3D(n5, flatTransforms());
	}

	public AffineTransform3D totalAffine3D()
	{
		return Common.toAffine3D(flatTransforms());
	}

	private void flatTransforms( final LinkedList<CoordinateTransform<?>> queue )
	{
		if( transform != null )
			queue.addFirst( transform );

		if( parentPath != null )
			parentPath.flatTransforms( queue );
	}

	public List<String> flatSpace()
	{
		final LinkedList<String> flatSpace = new LinkedList<>();
		flatSpace( flatSpace );
		flatSpace.addFirst( start );
		return flatSpace;
	}

	private void flatSpace( final LinkedList<String> queue )
	{
		if( end != null )
			queue.addFirst( end );

		if( parentPath != null )
			parentPath.flatSpace( queue );
	}

	@Override
	public String toString()
	{
		final List<String> spaceList = flatSpace();
		final List<CoordinateTransform<?>> transformList = flatTransforms();

		if( transformList.size() < 1 )
			return "(" + spaceList.get(0) + ")";

		final StringBuffer out = new StringBuffer();
		for( int i = 0; i < transformList.size(); i++ )
		{
			out.append( "("+spaceList.get(i));
			out.append( ") --" );
			out.append( transformList.get(i));
			out.append( "-> " );
		}
		out.append( "(" + end  + ")");

		return out.toString();
	}

}

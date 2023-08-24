package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph;

import java.util.LinkedList;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransformation;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ScaleAndTranslation;

public class TransformPath {

	private final String start;

	private final TransformPath parentPath;

	private final CoordinateTransformation transform;

	private final String end;

	public TransformPath( final CoordinateTransformation transform ) {

		this.start = transform.getInput();
		this.transform = transform;
		this.end = transform.getOutput();
		this.parentPath = null;
	}

	public TransformPath( final TransformPath parentPath, final CoordinateTransformation transform ) {

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
	 * @param space the space
	 * @return true if this path contains space
	 */
	public boolean hasSpace( final Space space )
	{
		if ( start.equals( space.getName() ) || end.equals( space.getName() ))
			return true;

		if( parentPath != null )
			return parentPath.hasSpace( space );

		return false;
	}

	public List<CoordinateTransformation> flatTransforms()
	{
		final LinkedList<CoordinateTransformation> flatTransforms = new LinkedList<>();
		flatTransforms( flatTransforms );
		return flatTransforms;
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

	private void flatTransforms( final LinkedList<CoordinateTransformation> queue )
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
//		if( parentPath == null )
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
		final List<CoordinateTransformation> transformList = flatTransforms();

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

//	public void add(Space s, Transform t) {
//		spaces.add( s );
//		transforms.add( t );
//	}

}

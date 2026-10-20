package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

public class SequenceCoordinateTransform extends AbstractCoordinateTransform<RealTransformSequence> {

	public static final String TYPE = "sequence";

	private final CoordinateTransform<?>[] transformations;

	protected SequenceCoordinateTransform() {
		super(TYPE);
		transformations = null;
	}

	public SequenceCoordinateTransform(
			final CoordinateTransform<?>[] transformations) {
		this("", OmeNgffReference.DUMMY, OmeNgffReference.DUMMY, transformations);
	}

	public SequenceCoordinateTransform(
			final String input, final String output,
			final CoordinateTransform<?>[] transformations) {
		this("", input, output, transformations );
	}

	public SequenceCoordinateTransform( final String name,
			final String input, final String output,
			final CoordinateTransform<?>... transformations) {
		super(TYPE, name, input, output);
		this.transformations = transformations;
	}

	public SequenceCoordinateTransform( final String name,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final CoordinateTransform<?>... transformations) {
		super(TYPE, name, inputRef, outputRef);
		this.transformations = transformations;
	}

	public SequenceCoordinateTransform( final String name,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final List<CoordinateTransform<?>> transformationList ) {
		super(TYPE, name, inputRef, outputRef);
		this.transformations = new CoordinateTransform[ transformationList.size() ];
		for( int i = 0; i < transformationList.size(); i++ )
			this.transformations[i] = transformationList.get( i );
	}

	public SequenceCoordinateTransform( final String name,
			final String input, final String output,
			final List<CoordinateTransform<?>> transformationList ) {
		super(TYPE, name, input, output);
		this.transformations = new CoordinateTransform[ transformationList.size() ];
		for( int i = 0; i < transformationList.size(); i++ )
			this.transformations[i] = transformationList.get( i );
	}

	@Override
	public RealTransformSequence getTransform()
	{
		// with permutations
//		final ArrayList< String[] > axOrders = axisOrdersForTransform( getTransformations(), getOutputSpaceObj().getAxisNames() );
//		for( final String[] axes : axOrders )
//			System.out.println(String.join(" ", axes ));

		final RealTransformSequence totalTransform = new RealTransformSequence();
		final CoordinateTransform<?>[] tforms = getTransformations();
		for( int i = 0; i < tforms.length; i++ )
			totalTransform.add( tforms[i].getTransform() );

		return totalTransform;
	}

	public boolean isAffine() {

		return Arrays.stream(transformations)
				.map(CoordinateTransform::create)
				.allMatch(x -> x.getTransform() instanceof AffineGet);
	}

	public AffineGet asAffine( int nd )
	{
		final AffineTransform affine = new AffineTransform( nd );

//		for( final CoordinateTransform<?> t : transformations )
//		{
//			final CoordinateTransform< ? > nt = CoordinateTransform.create( t );
////			AffineGet tform  (AffineGet)nt.getTransform();=
//			affine.preConcatenate(  (AffineGet)nt.getTransform() );
//		}

		for( final CoordinateTransform<?> t : transformations )
		{
			final RealTransform tform = t.getTransform();
			if( tform instanceof AffineGet )
				affine.preConcatenate(  (AffineGet)tform );
			else
				return null;
		}

//		Arrays.stream( transformations )
//			.map( NgffCoordinateTransformation::create )
//			.forEach( x -> affine.preConcatenate( ( AffineGet ) x.getTransform() ));

		return affine;
	}

	public RealTransformSequence getTransformNoSubsets()
	{
		final RealTransformSequence transform = new RealTransformSequence();
		for( final CoordinateTransform<?> t : getTransformations() )
			transform.add( t.getTransform() );

		return transform;
	}

	public RealTransformSequence getTransform( final String[] axes )
	{
		return getTransform();
	}

	@Override
	public RealTransformSequence getTransform(final N5Reader n5 )
	{
		final RealTransformSequence transform = new RealTransformSequence();
		for( final CoordinateTransform<?> t : getTransformations() )
			transform.add( t.getTransform(n5) );

		return transform;
	}

	public CoordinateTransform<?>[] getTransformations() {
		return transformations;
	}

	/**
	 * @return true if every transformation is invertible (may read parameters)
	 */
	public boolean isInvertible() {

		return inverse() != null;
	}

	/**
	 * @return the inverse, or null if any transformation is not invertible
	 */
	public SequenceCoordinateTransform inverse() {

		final ArrayList<CoordinateTransform<?>> invTransforms = new ArrayList<CoordinateTransform<?>>();
		for( CoordinateTransform<?> ct : getTransformations()) {
			final CoordinateTransform<?> inv = CoordinateTransform.inverseOf(ct);
			if( inv == null )
				return null;
			invTransforms.add(inv);
		}

		// the inverse of a sequence needs to apply the inverse transformations in reverse order
		Collections.reverse(invTransforms);

		return new SequenceCoordinateTransform(getName() +"inv", getOutput(), getInput(), invTransforms);
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Reader;

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
		this("", null, null, transformations );
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

	public boolean isInvertible() {

		return Arrays.stream(getTransformations()).allMatch(t -> {
			return t instanceof InvertibleCoordinateTransform;
		});
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public SequenceCoordinateTransform inverse() { 
		
		if( !isInvertible()) {
			return null;
		}

		final ArrayList<CoordinateTransform<?>> invTransforms = new ArrayList<CoordinateTransform<?>>();
		for( CoordinateTransform<?> ct : getTransformations()) {
			InvertibleCoordinateTransform<?> ict = (InvertibleCoordinateTransform<?>)ct;
			invTransforms.add(new InverseCoordinateTransform(ict));
		}

		// the inverse of a sequence needs to apply the inverse transformations in reverse order
		Collections.reverse(invTransforms);

		return new SequenceCoordinateTransform(getName() +"inv", getOutput(), getInput(), invTransforms);
	}

	/**
	 * Checks if the list of transformations is valid.
	 * A list is valid if an axis is an output of only one transformation,
	 * and there are no loops in the graph.
	 *
	 * @param tforms transformation list
	 * @return if the transform is valie
	 */
	public static boolean isValid( final CoordinateTransform<?>[] tforms )
	{
		final HashSet<Integer> outputAxes = new HashSet<>();
		for( final CoordinateTransform<?> t : tforms )
		{
			for( final int a : t.getOutputAxes() )
				if( outputAxes.contains( a ))
					return false;
				else
					outputAxes.add( a );
		}
		return true;
	}

	/**
	 * Builds a list of axes for intermediate results given a set of target axes and a list of transformations.
	 *
	 * Currently does not allow re-used axes.
	 *
	 * @param tforms transformations
	 * @param tgtAxes target axis names
	 * @return list of intermediate axis lists
	 */
	public static ArrayList<int[]> axisOrdersForTransform2( final CoordinateTransform<?>[] tforms, final int[] tgtAxes )
	{
		final ArrayList<int[]> axisOrders = new ArrayList<>();
		axisOrders.add( tgtAxes );

		final ArrayList<CoordinateTransform<?>> tlist = new ArrayList<>();
		Collections.addAll( tlist, tforms );
		Collections.reverse( tlist );

		final ArrayList<Integer> axes = new ArrayList<>();
		for (int a : tgtAxes) axes.add(a);
		for( final CoordinateTransform<?> t : tlist )
		{
			final List<Integer> inAx = boxedList( t.getInputAxes() );
			final List<Integer> outAx = boxedList( t.getOutputAxes() );
			axes.removeAll( outAx );
			axes.addAll( inAx );

			axisOrders.add( axes.stream().mapToInt(Integer::intValue).toArray() );
		}
		Collections.reverse( axisOrders );
		return axisOrders;
	}

	/**
	 * Builds a list of axes for intermediate results given a set of target axes and a list of transformations.
	 *
	 * Currently does not allow re-used axes.
	 *
	 * @param tforms transformations	public static class InverseCT extends AbstractCoordinateTransform<InvertibleRealTransform>
	implements InvertibleCoordinateTransform<InvertibleRealTransform> {

	InvertibleCoordinateTransform<?> ict;

	public InverseCT( final InvertibleCoordinateTransform<?> ict ) {
		super("invWrap", "inv-" + ict.getName(), ict.getOutput(), ict.getInput());
		this.ict = ict;
	}

	public InverseCT(final String type, final String name, final String inputSpace, final String outputSpace,
			final InvertibleCoordinateTransform<?> ict ) {
		super(type, name, inputSpace, outputSpace);
		this.ict = ict;
	}
	
	public InvertibleCoordinateTransform<?> getWrappedCoordinateTransform() {
		return ict;
	}

	@Override
	public InvertibleRealTransform getTransform() {
		return ict.getInvertibleTransform();
	}

	@Override
	public InvertibleRealTransform getTransform( final N5Reader n5 ) {
		return ict.getInvertibleTransform( n5 );
	}

	@Override
	public InvertibleRealTransform getInvertibleTransform() {
		return ict.getTransform();
	}

	@Override
	public InvertibleRealTransform getInvertibleTransform( final N5Reader n5 ) {
		return ict.getTransform( n5 );
	}
}

	 * @param tgtAxes target axis names
	 * @return list of intermediate axis lists
	 */
	public static ArrayList<int[]> axisOrdersForTransform( final CoordinateTransform<?>[] tforms, final int[] tgtAxes )
	{
		final ArrayList<int[]> axisOrders = new ArrayList<>();
		axisOrders.add( tgtAxes );

		final ArrayList<CoordinateTransform<?>> tlist = new ArrayList<>();
		Collections.addAll( tlist, tforms );
		Collections.reverse( tlist );

		final ArrayList<Integer> axes = new ArrayList<>();
		for (int a : tgtAxes) axes.add(a);
		for( final CoordinateTransform<?> t : tlist )
		{
			final List<Integer> inAx = boxedList( t.getInputAxes() );
			final List<Integer> outAx = boxedList( t.getOutputAxes() );
			final int i = firstIndex( axes, outAx );
			axes.removeAll( outAx );
			axes.addAll( i < 0 ? 0 : i, inAx );

			axisOrders.add( axes.stream().mapToInt(Integer::intValue).toArray() );
		}
		Collections.reverse( axisOrders );
		return axisOrders;
	}

	public static ArrayList<int[]> axisOrdersForTransformFirst( final CoordinateTransform<?>[] tforms, final int[] tgtAxes )
	{
		final ArrayList<int[]> axisOrders = new ArrayList<>();
		axisOrders.add( tforms[0].getInputAxes() );

		final ArrayList<Integer> axes = new ArrayList<>();
		for (int a : tgtAxes) axes.add(a);
		for( final CoordinateTransform<?> t : tforms )
		{
			final List<Integer> inAx = boxedList( t.getInputAxes() );
			final List<Integer> outAx = boxedList( t.getOutputAxes() );
			final int i = firstIndex( axes, outAx );
			axes.removeAll( outAx );
			axes.addAll( i < 0 ? 0 : i, inAx );

			axisOrders.add( axes.stream().mapToInt(Integer::intValue).toArray() );
		}
		Collections.reverse( axisOrders );
		return axisOrders;
	}

	public static ArrayList<HashSet<Integer>> cumNeededAxes( final CoordinateTransform<?>[] tforms )
	{
		final ArrayList<CoordinateTransform<?>> tlist = new ArrayList<>();
		Collections.addAll( tlist, tforms );
		Collections.reverse( tlist );

		final ArrayList<HashSet<Integer>> cAxisList = new ArrayList<>();
		HashSet<Integer> prev = null;
		for( final CoordinateTransform<?> t : tlist )
		{
			final HashSet<Integer> set = new HashSet<>();
			for (int a : t.getInputAxes()) set.add(a);
			if( prev != null )
				set.addAll( prev );

			prev = set;
			cAxisList.add( set );
		}

		Collections.reverse( cAxisList );
		return cAxisList;
	}

	private static int firstIndex( final List<Integer> list, final List<Integer> search )
	{
		int idx = -1;
		for( final Integer t : search )
		{
			final int i = list.indexOf( t );
			if( i >= 0 && (idx < 0 || i < idx ))
				idx = i;
		}
		return idx;
	}

	public static ArrayList<int[]> inputIndexesFromAxisOrders( final CoordinateTransform<?>[] tforms, final List<int[]> axisOrders )
	{
		final ArrayList<int[]> idxList = new ArrayList<>();
		for( int i = 0; i < tforms.length; i++ )
			idxList.add( indexes( tforms[i].getInputAxes(), axisOrders.get( i )));

		return idxList;
	}

	public static ArrayList<int[]> outputIndexesFromAxisOrders( final CoordinateTransform<?>[] tforms, final List<int[]> axisOrders )
	{
		final ArrayList<int[]> idxList = new ArrayList<>();
		for( int i = 0; i < tforms.length; i++ )
			idxList.add( indexes( tforms[i].getOutputAxes(), axisOrders.get( i + 1 )));

		return idxList;
	}

	private static int[] indexes( final int[] src, final int[] tgt )
	{
		final int[] idxs = new int[ src.length ];
		for( int i = 0; i < src.length; i++ )
			idxs[ i ] = indexOf( src[i], tgt );

		return idxs;
	}

	private static int indexOf( final int t, final int[] tgt )
	{
		for( int i = 0; i < tgt.length; i++ )
			if( tgt[i] == t )
				return i;

		return -1;
	}

	private static List<Integer> boxedList( final int[] arr )
	{
		return Arrays.stream(arr).boxed().collect(Collectors.toList());
	}

}

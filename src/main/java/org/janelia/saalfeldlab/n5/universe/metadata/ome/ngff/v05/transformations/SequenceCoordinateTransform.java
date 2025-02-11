package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

@NameConfig.Name("sequence")
public class SequenceCoordinateTransform extends AbstractCoordinateTransform<RealTransformSequence> implements RealCoordinateTransform<RealTransformSequence> {

	public static final String TYPE = "sequence";

	@NameConfig.Parameter()
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
			final CoordinateTransform<?>[] transformations) {
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
		final CoordinateTransform[] tforms = getTransformations();
		for( int i = 0; i < tforms.length; i++ )
		{
			final CoordinateTransform t = tforms[i];
			totalTransform.add( tforms[i].getTransform() );
		}

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
	 * Checks if the list of transformations is valid.
	 * A list is valid if an axis is an output of only one transformation,
	 * and there are no loops in the graph.
	 *
	 * @param tforms transformation list
	 * @return if the transform is valie
	 */
	public static boolean isValid( final CoordinateTransform<?>[] tforms )
	{
		final HashSet<String> outputAxes = new HashSet<>();
		for( final CoordinateTransform<?> t : tforms )
		{
			for( final String a : t.getOutputAxes() )
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
	public static ArrayList< String[] > axisOrdersForTransform2( final CoordinateTransform<?>[] tforms, final String[] tgtAxes )
	{
		System.out.println( "\naxisOrdersForTransform" );

		// work backwards:
		// we need to end up with the target axes
		final ArrayList<String[]> axisOrders = new ArrayList<>();
		axisOrders.add( tgtAxes );

		// reverse the list of transforms
		final ArrayList<CoordinateTransform<?>> tlist = new ArrayList<>();
		Collections.addAll( tlist, tforms );
		Collections.reverse( tlist );

		final ArrayList<String> axes = new ArrayList<>();
		Collections.addAll( axes, tgtAxes );
		for( final CoordinateTransform<?> t : tlist )
		{
			final List< String > inAx = Arrays.asList( t.getInputAxes() );
			final List< String > outAx = Arrays.asList( t.getOutputAxes() );
			axes.removeAll( outAx );
			axes.addAll( inAx );

			axisOrders.add( axes.stream().toArray( String[]::new ));
		}
		System.out.println( "" );
		Collections.reverse( axisOrders );
		return axisOrders;
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
	public static ArrayList< String[] > axisOrdersForTransform( final CoordinateTransform<?>[] tforms, final String[] tgtAxes )
	{
		System.out.println( "axisOrdersForTransform" );

		// work backwards:
		// we need to end up with the target axes
		final ArrayList<String[]> axisOrders = new ArrayList<>();
		axisOrders.add( tgtAxes );

		// reverse the list of transforms
		final ArrayList<CoordinateTransform<?>> tlist = new ArrayList<>();
		Collections.addAll( tlist, tforms );
		Collections.reverse( tlist );

		final ArrayList<String> axes = new ArrayList<>();
		Collections.addAll( axes, tgtAxes );
		for( final CoordinateTransform<?> t : tlist )
		{
			final List< String > inAx = Arrays.asList( t.getInputAxes() );
			final List< String > outAx = Arrays.asList( t.getOutputAxes() );
			final int i = firstIndex( axes, outAx );
			axes.removeAll( outAx );
			axes.addAll( i < 0 ? 0 : i, inAx );

			axisOrders.add( axes.stream().toArray( String[]::new ));
		}
		System.out.println( "" );
		Collections.reverse( axisOrders );
		return axisOrders;
	}

	public static ArrayList< String[] > axisOrdersForTransformFirst( final CoordinateTransform<?>[] tforms, final String[] tgtAxes )
	{
		System.out.println( "axisOrdersForTransformFirst" );

		final ArrayList<String[]> axisOrders = new ArrayList<>();
		axisOrders.add( tforms[0].getInputAxes() );

		final ArrayList<String> axes = new ArrayList<>();
		Collections.addAll( axes, tgtAxes );
		for( final CoordinateTransform<?> t : tforms )
		{
			final List< String > inAx = Arrays.asList( t.getInputAxes() );
			final List< String > outAx = Arrays.asList( t.getOutputAxes() );
			final int i = firstIndex( axes, outAx );
			axes.removeAll( outAx );
			axes.addAll( i < 0 ? 0 : i, inAx );

			axisOrders.add( axes.stream().toArray( String[]::new ));
		}
		System.out.println( "" );
		Collections.reverse( axisOrders );
		return axisOrders;
	}

	public static ArrayList< HashSet< String > > cumNeededAxes( final CoordinateTransform<?>[] tforms )
	{
		System.out.println("cumNeededAxes");
		final ArrayList<CoordinateTransform> tlist = new ArrayList<>();
		Collections.addAll( tlist, tforms );
		Collections.reverse( tlist );

		final ArrayList<HashSet<String>> cAxisList = new ArrayList<>();
		HashSet<String> prev = null;
		for( final CoordinateTransform t : tlist )
		{
			System.out.println( "t: " + t );
			System.out.println( "  axes : " + Arrays.toString( t.getInputAxes() ) );
			final HashSet< String > set = new HashSet<>();
			Collections.addAll( set, t.getInputAxes());
			if( prev != null )
				set.addAll( prev );

			prev = set;
			cAxisList.add( set );
		}

		Collections.reverse( cAxisList );
		return cAxisList;
	}

	private static <T> int firstIndex( final List<T> list, final List<T> search )
	{
		int idx = -1;
		for( final T t : search )
		{
			final int i = list.indexOf( t );
			if( i >= 0 && (idx < 0 || i < idx ))
				idx = i;
		}
		return idx;
	}

	public static ArrayList< int[] > inputIndexesFromAxisOrders( final CoordinateTransform<?>[] tforms, final List<String[]> axisOrders )
	{
		final ArrayList< int[] > idxList = new ArrayList<>();
		for( int i = 0; i < tforms.length; i++ )
			idxList.add( indexes( tforms[i].getInputAxes(), axisOrders.get( i )));

		return idxList;
	}

	public static ArrayList< int[] > outputIndexesFromAxisOrders( final CoordinateTransform<?>[] tforms, final List<String[]> axisOrders )
	{
		final ArrayList< int[] > idxList = new ArrayList<>();
		for( int i = 0; i < tforms.length; i++ )
			idxList.add( indexes( tforms[i].getOutputAxes(), axisOrders.get( i + 1 )));

		return idxList;
	}

	/**
	 * Returns the indexes of src objects into tgt object.
	 *
	 *
	 * @param <T> the type
	 * @param src
	 * @param tgt
	 * @return
	 */
	private static <T> int[] indexes( final T[] src, final T[] tgt )
	{
		final int[] idxs = new int[ src.length ];
		for( int i = 0; i < src.length; i++ )
			idxs[ i ] = indexOf( src[i], tgt );

		return idxs;
	}

	private static <T> int indexOf( final T t, final T[] tgt )
	{
		for( int i = 0; i < tgt.length; i++ )
			if( tgt[i].equals( t ))
				return i;

		return -1;
	}

	private static boolean isReindexed(final int[] indexes) {
		for( int i = 0; i < indexes.length; i++ )
			if( indexes[i] != i )
				return true;

		return false;
	}

}

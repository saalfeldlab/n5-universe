package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class AxisUtils {

	public static final String xLabel = "x";
	public static final String yLabel = "y";
	public static final String cLabel = "z";
	public static final String zLabel = "c";
	public static final String tLabel = "t";

	public static final String spaceType = "space";
	public static final String timeType = "time";
	public static final String channelType = "channel";

	public static final DefaultAxisTypes defaultAxisTypes = DefaultAxisTypes.getInstance();


	/**
	 * Finds and returns a permutation p such that source[p[i]] equals target[i]
	 *
	 * @param <T> the object type
	 * @param source the source objects
	 * @param target the target objects
	 * @return the permutation array
	 */
	public static <T> int[] findPermutation( final T[] source, final T[] target ) {

		final int[] p = new int[ target.length ];
		for( int i = 0; i < target.length; i++ ) {
			final T t = target[ i ];
			boolean found = false;
			for( int j = 0; j < source.length; j++ ) {
				if( source[j].equals(t) ) {
					p[i] = j;
					found = true;
					break;
				}
			}
			if( !found )
				return null;
		}
		return p;
	}

	public static <T> List<T> permute( final List<T> in, final int[] p )
	{
		final ArrayList<T> out = new ArrayList<T>( p.length );
		for( int i = 0; i < p.length; i++ )
			out.add( in.get( p[i] ));

		return out;
	}

	public static Axis[] buildAxes( final String... labels )
	{
		return Arrays.stream(labels).map( x -> {
			final String type = getDefaultType( x );
			return new Axis( x, type, "", type.equals("channel") );
		}).toArray( Axis[]::new );
	}

	/**
	 * Finds and returns a permutation p such that source[p[i]] equals target[i]
	 *
	 * @param <A> the axis type
	 * @param axisMetadata the axis metadata
	 * @return the permutation
	 */
	public static <A extends AxisMetadata> int[] findImagePlusPermutation( final AxisMetadata axisMetadata ) {
		return findImagePlusPermutation( axisMetadata.getAxisLabels());
	}

	/**
	 * Finds and returns a permutation p such that source[p[i]] equals xyczt
	 *
	 * @param axisLabels the axis labels
	 * @return the permutation array
	 */
	public static int[] findImagePlusPermutation( final String[] axisLabels ) {

		final int[] p = new int[ 5 ];
		p[0] = indexOf( axisLabels, "x" );
		p[1] = indexOf( axisLabels, "y" );
		p[2] = indexOf( axisLabels, "c" );
		p[3] = indexOf( axisLabels, "z" );
		p[4] = indexOf( axisLabels, "t" );
		return p;
	}

	/**
	 * Replaces "-1"s in the input permutation array
	 * with the largest value.
	 *
	 * @param p the permutation
	 */
	public static void fillPermutation( final int[] p ) {
		int j = Arrays.stream(p).max().getAsInt() + 1;
		for (int i = 0; i < p.length; i++)
			if (p[i] < 0)
				p[i] = j++;
	}

	/**
	 * Turns a length p.length permutation into a length N permutation,
	 * by returning a new permutation whose first p.length indexes come from p,
	 * and that contains all integers from [0,N-1].
	 *
	 * If a p of length N is passed, it is passed to fillPermutation( p ),
	 * then returned.
	 *
	 * @param p the permutation
	 * @param N the total size
	 */
	public static int[] fillPermutation( final int[] p, final int N ) {
		if( p.length == N )
		{
			fillPermutation( p );
			return p;
		}
		else {
			final int[] pfilled = new int[ N ];
			final SortedSet<Integer> remainingIndexes = new TreeSet<>();
			remainingIndexes.addAll( IntStream.range(0, N).boxed().collect(Collectors.toList()));

			for( int i = 0; i < N; i++)
			{
				if( i < p.length )
				{
					pfilled[i] = p[i];
					remainingIndexes.remove(p[i]);
				}
				else
					pfilled[i] = -1;
			}

			for( int i = 0; i < N; i++)
			{
				if( pfilled[i] == -1 )
				{
					final int j = remainingIndexes.first();
					remainingIndexes.remove( j );
					pfilled[i] = j;
				}
			}

			return pfilled;
		}
	}

	public static double[] matrixFromPermutation( final int[] p, final int ncols )
	{
		final int nrows = p.length;
		final double[] mtx = new double[ nrows * ncols ];

		/*
		 * 	p[i] = j
		 * 	means that output i comes from input j
		 * that means the i-j the element of the matrix is 1
		 */
		for( int i = 0; i < p.length; i++ )
			mtx[ p[i] + ncols * i ] = 1;

		return mtx;
	}

	public static boolean isIdentityPermutation( final int[] p )
	{
		for( int i = 0; i < p.length; i++ )
			if( p[i] != i )
				return false;

		return true;
	}

	public static <T> RandomAccessibleInterval<T> permuteForImagePlus(
			final RandomAccessibleInterval<T> img,
			final AxisMetadata meta ) {

		final int[] p = findImagePlusPermutation( meta );
		fillPermutation( p );

		RandomAccessibleInterval<T> imgTmp = img;
		while( imgTmp.numDimensions() < 5 )
			imgTmp = Views.addDimension(imgTmp, 0, 0 );

		if( isIdentityPermutation( p ))
			return imgTmp;

		return permute(imgTmp, invertPermutation(p));
	}

	private static final <T> int indexOf( final T[] arr, final T tgt ) {
		for( int i = 0; i < arr.length; i++ ) {
			if( arr[i].equals(tgt))
				return i;
		}
		return -1;
	}

    /**
     * Permutes the dimensions of a {@link RandomAccessibleInterval}
     * using the given permutation vector, where the ith value in p
     * gives destination of the ith input dimension in the output.
     *
     * @param <T> the image type
     * @param source the source data
     * @param p the permutation
     * @return the permuted source
     */
	public static final < T > IntervalView< T > permute( final RandomAccessibleInterval< T > source, final int[] p )
	{
		final int n = source.numDimensions();

		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		for ( int i = 0; i < n; ++i )
		{
			min[ p[ i ] ] = source.min( i );
			max[ p[ i ] ] = source.max( i );
		}

		final MixedTransform t = new MixedTransform( n, n );
		t.setComponentMapping( p );

		final IntervalView<T> out = Views.interval( new MixedTransformView< T >( source, t ), min, max );
		return out;
	}

	public static int[] invertPermutation( final int[] p )
	{
		final int[] inv = new int[ p.length ];
		for( int i = 0; i < p.length; i++ )
			inv[p[i]] = i;

		return inv;
	}

	public static String[] getDefaultTypes( final String[] labels ) {
		return Arrays.stream(labels).map( l -> defaultAxisTypes.get(l)).toArray( String[]::new );
	}

	public static String getDefaultType( final String label ) {
		return defaultAxisTypes.get(label);
	}

	// implemented as a singleton
	public static class DefaultAxisTypes {

		private static DefaultAxisTypes INSTANCE;

		private final HashMap<String,String> labelToType;

		private DefaultAxisTypes() {
			 labelToType = new HashMap<>();
			 labelToType.put("x", "space");
			 labelToType.put("y", "space");
			 labelToType.put("z", "space");
			 labelToType.put("c", "channel");
			 labelToType.put("t", "time");
			 labelToType.put("X", "space");
			 labelToType.put("Y", "space");
			 labelToType.put("Z", "space");
			 labelToType.put("C", "channel");
			 labelToType.put("T", "time");
		}

		public static final DefaultAxisTypes getInstance()
		{
			if( INSTANCE == null )
				INSTANCE = new DefaultAxisTypes();

			return INSTANCE;
		}

		public String get( final String label ) {
			if( labelToType.containsKey(label))
				return labelToType.get(label);
			else if( label.toLowerCase().startsWith("data"))
				return "data";
			else
				return "unknown";
		}
	}

	/**
	 * Returns true if any elements of array are contained in the set
	 * @param set the set
	 * @param array the array
	 * @return
	 */
	public static <T> boolean containsAny( final Set<T> set, final T[] array )
	{
		for( final T t : array )
			if( set.contains( t ))
				return true;

		return false;
	}

	/**
	 * Returns true if any elements of array  equal t
	 * @param t some element
	 * @param array the array
	 * @return true if array contains t
	 */
	public static <T> boolean contains( final T t, final T[] array )
	{
		return Arrays.stream(array).anyMatch( x -> x.equals(t) );
	}

}

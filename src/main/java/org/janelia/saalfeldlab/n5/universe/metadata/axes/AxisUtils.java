package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialModifiable;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class AxisUtils {

	public static final String xLabel = "x";
	public static final String yLabel = "y";
	public static final String cLabel = "c";
	public static final String zLabel = "z";
	public static final String tLabel = "t";

	public static final String spaceType = "space";
	public static final String timeType = "time";
	public static final String channelType = "channel";
	public static final String unknownType = "unknown";

	public static final DefaultAxisTypes defaultAxisTypes = DefaultAxisTypes.getInstance();

	public static String SPACE_UNIT = "um";
	public static String TIME_UNIT = "s";


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

	public static <T> List<T> permute(final List<T> in, final int[] p) {

		final ArrayList<T> out = new ArrayList<T>(p.length);
		for (int i = 0; i < p.length; i++)
			out.add(in.get(p[i]));

		return out;
	}

	/**
	 * Permutes an input array into a destination array. The input and destination may be the same
	 * instance.
	 *
	 * @param <T>
	 *            the type
	 * @param in
	 *            the input array
	 * @param dest
	 *            the destination array
	 * @param p
	 *            the permutation
	 */
	public static <T> void permute(final T[] in, final T[] dest, final int[] p) {

		final ArrayList<T> tmp = new ArrayList<T>(in.length);
		for (int i = 0; i < in.length; i++)
			tmp.add(in[i]);

		for (int i = 0; i < p.length; i++)
			dest[i] = tmp.get(p[i]);
	}

	public static long[] permute(final long[] in, final int[] p) {

		final long[] out = new long[p.length];
		for (int i = 0; i < p.length; i++)
			out[i] = in[p[i]];

		return out;
	}

	public static int[] permute(final int[] in, final int[] p) {

		final int[] out = new int[p.length];
		for (int i = 0; i < p.length; i++)
			out[i] = in[p[i]];

		return out;
	}

	public static double[] permute(final double[] in, final int[] p) {

		final double[] out = new double[p.length];
		for (int i = 0; i < p.length; i++)
			out[i] = in[p[i]];

		return out;
	}

	public static Axis[] buildAxes( final String... labels )
	{
		return Arrays.stream(labels).map( x -> {
			final String type = getDefaultType( x );
			return new Axis(x, type, "", type.equals(Axis.CHANNEL));
		}).toArray( Axis[]::new );
	}

	/**
	 * Finds and returns a permutation p such that source[p[i]] equals target[i]
	 *
	 * @param <A> the axis type
	 * @param axisMetadata the axis metadata
	 * @return the permutation
	 */
	public static <A extends AxisMetadata > int[] findImagePlusPermutation( final AxisMetadata axisMetadata ) {

		// TODO should use axis types, not just labels.
		// and should consider what to do if an unknown label exists
		return findImagePlusPermutation(axisMetadata.getAxisLabels());
	}

	/**
	 * Finds and returns a permutation p such that source[p[i]] equals xyczt
	 *
	 * @param axisLabels the axis labels
	 * @return the permutation array
	 */
	public static int[] findImagePlusPermutation(final String[] axisLabels) {

		final int[] p = new int[ 5 ];
		p[0] = indexOf( axisLabels, "x" );
		p[1] = indexOf( axisLabels, "y" );
		p[2] = indexOf( axisLabels, "c" );
		p[3] = indexOf( axisLabels, "z" );
		p[4] = indexOf( axisLabels, "t" );
		return p;
	}

	public static int[] findImagePlusSpatialPermutation(final int[] p) {

		final OptionalInt minOpt = Arrays.stream(p).min();
		if (minOpt.isPresent()) {
			final int min = minOpt.getAsInt();
			return Arrays.stream(p).map(x -> x - min).toArray();
		} else
			return p;
	}

	/**
	 * Converters an array of integers to a normalized array of integers such that the smallest
	 * integer is mapped to 0, the second smallest to 1 ... and the largest is mapped to N-1, where
	 * N is the number of unique integers in the array.
	 *
	 * @param indexes
	 *            the indexes
	 *
	 * @return normalized indexes
	 */
	public static int[] normalizeIndexes(final int[] indexes) {

		final TreeSet<Integer> set = new TreeSet<Integer>();
		for (final int i : indexes)
			set.add(i);

		// can't get index from a tree set, use this sad, not scalable workaround for now
		final int[] sortedUniqueIndexes = new int[set.size()];
		final Iterator<Integer> it = set.iterator();
		int i = 0;
		while ( it.hasNext())
			sortedUniqueIndexes[i++] = it.next();

		final int[] out = new int[indexes.length];
		for (i = 0; i < out.length; i++ )
			out[i] = Arrays.binarySearch(sortedUniqueIndexes, indexes[i]);

		return out;
	}

	/**
	 * Replaces "-1"s in the input permutation array
	 * with the largest value.
	 *
	 * @param p the permutation
	 */
	public static void fillPermutation(final int[] p) {
		int j = Arrays.stream(p).max().getAsInt() + 1;
		for (int i = 0; i < p.length; i++)
			if (p[i] < 0)
				p[i] = j++;
	}

	public static AffineGet axisPermutationTransform(final int[] p) {

		final int N = p.length;
		final int[] normalP = normalizeIndexes(p);
		final double[] affineParams = new double[N * (N + 1)];
		for (int i = 0; i < normalP.length; i++)
			affineParams[normalP[i] + (N + 1) * i] = 1.0;

		return new AffineTransform(affineParams);
	}

	public static boolean isIdentityPermutation( final int[] p ) {

		for( int i = 0; i < p.length; i++ )
			if( p[i] != i )
				return false;

		return true;
	}

	public static <T, M extends AxisMetadata & N5Metadata> RandomAccessibleInterval<T> permuteForImagePlus(
			final RandomAccessibleInterval<T> img,
			final M meta) {

		final int[] p = findImagePlusPermutation( meta );
		fillPermutation( p );

		// TODO under what conditions can I return the image directly?

		RandomAccessibleInterval<T> imgTmp = img;
		while( imgTmp.numDimensions() < 5 )
			imgTmp = Views.addDimension(imgTmp, 0, 0 );

		if( isIdentityPermutation( p ))
			return imgTmp;

		return permute(imgTmp, invertPermutation(p));
	}

	public static <M extends AxisMetadata & N5Metadata> M permuteForImagePlus(int[] spatialPermutation, final M meta) {

		if (isIdentityPermutation(spatialPermutation))
			return meta;

		if (meta instanceof SpatialMetadata && meta instanceof SpatialModifiable) {

			final AffineTransform3D tform = ((SpatialMetadata)meta).spatialTransform3d().copy();
			final AffineTransform3D tformInv = ((SpatialMetadata)meta).spatialTransform3d().inverse().copy();

			final AffineGet permTform = AxisUtils.axisPermutationTransform(spatialPermutation);
			tform.concatenate(permTform).preConcatenate(permTform.inverse()); // exchange rows and
			tform.concatenate(tformInv);

			final M out = (M)(((SpatialModifiable)meta).modifySpatialTransform(meta.getPath(), tform));
			return out;
		}

		return meta;
	}

	public static <T, M extends N5Metadata, A extends AxisMetadata & N5Metadata> Pair<RandomAccessibleInterval<T>, M> permuteImageAndMetadataForImagePlus(
			final RandomAccessibleInterval<T> img, final M meta) {

		if (meta != null && meta instanceof AxisMetadata) {

			final int[] p = AxisUtils.findImagePlusPermutation((AxisMetadata)meta);
			AxisUtils.fillPermutation(p);

			RandomAccessibleInterval<T> imgTmp = img;
			while (imgTmp.numDimensions() < 5)
				imgTmp = Views.addDimension(imgTmp, 0, 0);

			if (AxisUtils.isIdentityPermutation(p))
				return new ValuePair<>(imgTmp, meta);

			// do the permutation
			final RandomAccessibleInterval<T> imgOut = permute(imgTmp, invertPermutation(p));
			final int[] spatialPermutation = new int[]{p[0], p[1], p[3]};
			@SuppressWarnings("unchecked")
			final M permutedMeta = (M)permuteForImagePlus(spatialPermutation, (A)meta);

			return new ValuePair<>(imgOut, permutedMeta);
		}

		return new ValuePair<>(img, meta);
	}

	public static <T, M extends N5Metadata, A extends AxisMetadata & N5Metadata> Pair<RandomAccessibleInterval<T>, M> permuteImageAndMetadataForImagePlus(
			final int[] p, final RandomAccessibleInterval<T> img, final M meta) {

		// store the permutation for metadata
		final int[] metadataPermutation = Arrays.stream(p).filter(x -> x >= 0).toArray();

		// pad the image permutation
		AxisUtils.fillPermutation(p);

		RandomAccessibleInterval<T> imgTmp = img;
		while (imgTmp.numDimensions() < 5)
			imgTmp = Views.addDimension(imgTmp, 0, 0);

		RandomAccessibleInterval<T> imgOut;
		M datasetMeta;
		if (AxisUtils.isIdentityPermutation(p)) {
			imgOut = imgTmp;
			datasetMeta = meta;
		} else {
			imgOut = AxisUtils.permute(imgTmp, AxisUtils.invertPermutation(p));
			datasetMeta = (M)MetadataUtils.permuteSpatialMetadata(meta, metadataPermutation);
		}

		return new ValuePair<>(imgOut, datasetMeta);
	}

	public static <T> RandomAccessibleInterval<T> reverseDimensions(final RandomAccessibleInterval<T> img) {

		final int nd = img.numDimensions();
		final int[] p = IntStream.iterate(nd - 1, x -> x - 1).limit(nd).toArray();
		// reversing is its own permutation, so can skip the invert step
		return permute(img, p);
	}

	private static final <T> int indexOf(final T[] arr, final T tgt) {
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
	public static final <T> IntervalView<T> permute(final RandomAccessibleInterval<T> source, final int[] p) {

		final int n = source.numDimensions();

		final long[] min = new long[n];
		final long[] max = new long[n];
		for (int i = 0; i < n; ++i) {
			min[p[i]] = source.min(i);
			max[p[i]] = source.max(i);
		}

		final MixedTransform t = new MixedTransform(n, n);
		t.setComponentMapping(p);

		final IntervalView<T> out = Views.interval(new MixedTransformView<T>(source, t), min, max);
		return out;
	}

	public static int[] invertPermutation( final int[] p )
	{
		final int[] inv = new int[ p.length ];
		for( int i = 0; i < p.length; i++ )
			inv[p[i]] = i;

		return inv;
	}

	public static int[] indexes(final Axis[] axes, final Predicate<Axis> predicate) {

		return IntStream.range(0, axes.length).filter(i -> predicate.test(axes[i])).toArray();
	}

	public static Axis[] defaultAxes( final int N ) {

		return IntStream.range(0, N).mapToObj(i -> {
			return defaultAxis(defaultLabel(i));
		}).toArray(Axis[]::new);
	}

	public static Axis[] defaultAxes( final String... labels) {

		final Axis[] axes = new Axis[labels.length];
		for (int i = 0; i < labels.length; i++)
			axes[i] = defaultAxis(labels[i]);

		return axes;
	}

	public static Axis defaultAxis(final String label) {

		final String type = getDefaultType(label);
		return new Axis(type, label, getDefaultUnit(type));
	}

	public static Axis unknownAxis() {
		return new Axis(unknownType, "", "");
	}

	/**
	 * The default axes for dialects that store only spatial data (n5viewer and
	 * cosem), when time dimension is allowed.
	 *
	 * @param meta
	 *            the metadata
	 * @return axes default axis metadata
	 */
	public static DefaultAxisMetadata defaultN5ViewerAxes(final N5SpatialDatasetMetadata meta) {

		final int nd = meta.getAttributes().getNumDimensions();

		final String[] labels;
		if (nd ==  2)
			labels = new String[]{"x", "y"};
		else if (nd ==  3)
			labels = new String[]{"x", "y", "z"};
		else if( nd == 4)
			labels = new String[]{"x", "y", "z", "t"};
		else
			return null;

		final String[] types = AxisUtils.getDefaultTypes(labels);
		final String[] units = Stream.generate(() -> meta.unit()).limit(nd).toArray(String[]::new);
		return new DefaultAxisMetadata(meta.getPath(), labels, types, units);
	}

	public static String[] getDefaultTypes( final String[] labels ) {
		return Arrays.stream(labels).map( l -> defaultAxisTypes.get(l)).toArray( String[]::new );
	}

	public static String getDefaultType( final String label ) {
		return defaultAxisTypes.get(label);
	}

	public static String getDefaultUnit( final String type ) {
		if( type.equals(Axis.SPACE ))
			return SPACE_UNIT;
		else if( type.equals(Axis.TIME ))
			return TIME_UNIT;
		else
			return "";
	}

	public static String defaultLabel(final int i) {

		if (i == 0)
			return xLabel;
		else if (i == 1)
			return yLabel;
		else if (i == 2)
			return cLabel;
		else if (i == 3)
			return zLabel;
		else if (i == 4)
			return tLabel;
		else
			return String.format("dim_%d", i);
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
	 * @param <T> the type
	 * @param set the set
	 * @param array the array
	 * @return true if any elements of the array are contained in the set.
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
	 * @param <T> the type
	 * @param t some element
	 * @param array the array
	 * @return true if array contains t
	 */
	public static <T> boolean contains( final T t, final T[] array )
	{
		return Arrays.stream(array).anyMatch( x -> x.equals(t) );
	}

}

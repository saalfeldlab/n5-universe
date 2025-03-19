package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AbstractParametrizedFieldTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.RotationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.TranslationCoordinateTransform;

import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.NumericType;

public class TransformUtils
{

	public static double[][] affineToMatrix( final AffineGet affine ) {

		if (affine == null)
			return null;

		// AffineGets always have numSourceDimensions == numTargetDimensions
		final int N = affine.numSourceDimensions();
		final double[][] mtx = new double[N][N+1];
		for( int i = 0; i < N; i++ )
			for( int j = 0; j < (N+1); j++ )
				mtx[i][j] = affine.get(i, j);

		return mtx;
	}

	public static double[][] affineToRotation( final AffineGet affine ) {

		if (affine == null)
			return null;

		// AffineGets always have numSourceDimensions == numTargetDimensions
		final int N = affine.numSourceDimensions();
		final double[][] mtx = new double[N][N];
		for( int i = 0; i < N; i++ )
			for( int j = 0; j < N; j++ )
				mtx[i][j] = affine.get(i, j);

		return mtx;
	}

	public static AffineTransform3D spatialTransform3D( AffineGet affine, Axis[] axes )
	{
		final int numSpatialDims = (int)Arrays.stream(axes)
				.filter( x -> x.getType().equals(Axis.SPACE)).count();

		// can copy the input to the output directly if
		// the input is 3d and all dimensions are spatial
		if( affine.numTargetDimensions() == 3 && numSpatialDims == 3 )
		{
			final AffineTransform3D affine3d = new AffineTransform3D();
			affine3d.set( affine.getRowPackedCopy() );
			return affine3d;
		}

		final int[] spatialIndexes = new int[numSpatialDims];
		int j = 0;
		for( int i = 0; i < affine.numSourceDimensions(); i++ )
		{
			if (axes[i].getType().equals(Axis.SPACE))
				spatialIndexes[j++] = i;
		}

		return spatialTransform3D(affine, spatialIndexes);
	}

	public static AffineTransform3D spatialTransform3D( AffineGet affine, final int[] spatialIndexes )
	{
		final int numSpatialDims = spatialIndexes.length;

		// can copy the input to the output directly if
		// the input is 3d and all dimensions are spatial
		if( affine.numTargetDimensions() == 3 && numSpatialDims == 3 )
		{
			final AffineTransform3D affine3d = new AffineTransform3D();
			affine3d.set( affine.getRowPackedCopy() );
			return affine3d;
		}

		final int numAffineDims = affine.numTargetDimensions();
		if (numAffineDims < 3 || numSpatialDims < 3) {
			final AffineGet spatialAffine = subAffine(affine, spatialIndexes);
			return (AffineTransform3D)superAffine(spatialAffine, 3,
					IntStream.range(0, numSpatialDims).toArray());
		}
		else if (numAffineDims > 3 && numSpatialDims == 3) {
			// TODO what if (N > 3)?
			return (AffineTransform3D)subAffine(affine, spatialIndexes);
		}

		// the above should cover all valid cases
		// return null for invalid case
		return null;
	}

	public static AffineGet subAffine( AffineGet affine, int[] indexes  )
	{
		final int nd = indexes.length;
		if( nd == 2 )
		{
			final AffineTransform2D out = new AffineTransform2D();
			out.set( rowPackedSubMatrix( affine, indexes ) );
			return out;
		}
		else if( nd == 3 )
		{
			final AffineTransform3D out = new AffineTransform3D();
			out.set( rowPackedSubMatrix( affine, indexes ) );
			return out;

		}
		else
		{
			final AffineTransform out = new AffineTransform( nd );
			out.set( rowPackedSubMatrix( affine, indexes ) );
			return out;
		}
	}

	/**
	 * Returns a "row-major" flattened array, i.e., the first index is contiguous in the output.
	 *
	 * @param arr the 2d array
	 * @return a flattened version
	 */
	public static double[] flatten(final double[][] arr) {

		final double[] out = new double[arr.length * arr[0].length];
		int k = 0;
		for (int i = 0; i < arr.length; i++)
			for (int j = 0; j < arr[i].length; j++)
				out[k++] = arr[i][j];

		return out;
	}

	/**
	 * Turn an N x N matrix into a flattened (N+1) x (N+1) matrix in homogeneous coordinates.
	 * 
	 * 
	 * @param matrix a rotation matrix
	 * @return the corresponding flattened (row-major) matrix in homogeneous coordinates.
	 */
	public static double[] flattenRotation( double[][] matrix ) {

		final int nd = matrix.length;
		final double[] out = new double[nd * (nd+1)];
		int pos = 0;
		for( int r = 0; r < nd; r++) {
			System.arraycopy(matrix[r], 0, out, pos, nd);
			pos += (nd + 1);
		}

		return out;
	}

	public static double[][] toAffineMatrix(double[] flatAffine) {

		int N = flatAffine.length;
		int nd = (int) Math.floor(Math.sqrt(N));

		if (N != nd * (nd + 1)) {
			return null;
		}

		double[][] mtx = new double[nd][nd + 1];
		int k = 0;
		for (int i = 0; i < nd; i++)
			for (int j = 0; j < nd + 1; j++)
				mtx[i][j] = flatAffine[k++];

		return mtx;
	}

	public static double[][] toRotationMatrix(double[] flatAffine) {

		int N = flatAffine.length;
		int nd = (int) Math.sqrt(N);

		if (N != nd * (nd + 1)) {
			return null;
		}

		double[][] mtx = new double[nd][nd];
		int k = 0;
		for (int row = 0; row < nd; row++) {
			for (int col = 0; col < nd; col++) {
				mtx[row][col] = flatAffine[k++];
			}
			k++; // bump the index once for for the n+1
		}

		return mtx;
	}

	/**
	 * Returns a "column-major" flattened array, i.e., the second index is contiguous in the output.
	 *
	 * @param arr the 2d array
	 * @return a flattened version
	 */
	public static double[] flattenColMajor( final double[][] arr ) {

		final double[] out = new double[arr.length * arr[0].length];
		int k = 0;
		for (int j = 0; j < arr[0].length; j++)
			for (int i = 0; i < arr.length; i++)
				out[k++] = arr[i][j];

		return out;
	}

	/**
	 * Interpreting the input 2D array as a transformation matrix in homogeneous coordinates,
	 * this returns a new matrix with the equivalent behavior o 
	 * <p> 
	 * For example, if the input matrix operates on column vectors with coordinates [x,y,z],
	 * the output matrix operates on column vectors with coordinates [z,y,x].
	 * <p>
	 * A 2D example, for input matrix:
	 * 		[ xx xy tx ]
	 * 		[ yx yy ty ]
	 * this method returns
	 * 		[ yy yx ty ]
	 * 		[ xy xx tx ]
	 */
	public static double[][] reverseCoordinates( final double[][] arr ) {

		assert arr.length == arr[0].length - 1;

		final int nd = arr.length;
		final double[][] out = new double[nd][nd+1];
		// reverse all rows (ignore last column)
		for (int i = 0; i < nd; i++)
			for (int j = 0; j < nd; j++)
				out[i][j] = arr[nd-i-1][nd-j-1];

		// last column
		for (int i = 0; i < nd; i++)
			out[i][nd] = arr[nd-i-1][nd];

		return out;
	}
	
	/**
	 * Interpreting the input 2D array as a square transformation matrix,
	 * this returns a new matrix with the equivalent behavior o 
	 * <p> 
	 * For example, if the input matrix operates on column vectors with coordinates [x,y,z],
	 * the output matrix operates on column vectors with coordinates [z,y,x].
	 * <p>
	 * A 2D example, for input matrix:
	 * 		[ xx xy ]
	 * 		[ yx yy ]
	 * this method returns
	 * 		[ yy yx ]
	 * 		[ xy xx ]
	 */
	public static double[][] reverseCoordinatesRotation( final double[][] arr ) {

		assert arr.length == arr[0].length;

		final int nd = arr.length;
		final double[][] out = new double[nd][nd];
		// reverse all rows (ignore last column)
		for (int i = 0; i < nd; i++)
			for (int j = 0; j < nd; j++)
				out[i][j] = arr[nd-i-1][nd-j-1];

		return out;
	}

	private static double[] rowPackedSubMatrix( AffineGet affine, int[] indexes )
	{
		final int ndIn = affine.numTargetDimensions();
		final int nd = indexes.length;
		final double[] dat = new double[ nd * ( nd + 1 ) ];
		int k = 0;
		for( int i = 0; i < nd; i++ ) {
			for( int j = 0; j < nd; j++ ) {
				dat[k++] = affine.get( indexes[i], indexes[j] );
			}
			dat[k++] = affine.get( indexes[i], ndIn );
		}
		return dat;
	}

	/**
	 * Returns an affine with a specified dimensionality that
	 * contains
	 *
	 * @param <T> an affine set and affine get
	 * @param affine the input affine
	 * @param nd the dimensionality of the output affine
	 * @param indexes the indexes
	 * @return the resulting affine
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AffineGet & AffineSet> AffineGet superAffine(
			final AffineGet affine, int nd, final int[] indexes) {

		assert( indexes.length == affine.numSourceDimensions() );
		if( affine.numTargetDimensions() == nd )
			return affine;

		final T out;
		if (nd == 2)
			out = (T)new AffineTransform2D();
		else if (nd == 3)
			out = (T)new AffineTransform3D();
		else
			out = (T)new AffineTransform(nd);

		final int N = indexes.length;
		for( int i = 0; i < N; i++ ) {
			for( int j = 0; j < N; j++ ) {
				out.set( affine.get(i, j),
						indexes[i], indexes[j]);
			}
		}

		for (int i = 0; i < N; i++) {
			out.set(affine.get(i, N),
					indexes[i], nd );
		}

		return out;
	}
	
	public static AffineTransform3D toAffine3D(final AffineGet affine) {

		if (affine instanceof AffineTransform3D)
			return (AffineTransform3D) affine;

		final int[] indexes = IntStream.range(0, affine.numSourceDimensions()).toArray();
		return (AffineTransform3D) superAffine(affine, 3, indexes);

	}

	public static AffineTransform3D toAffine3D( SequenceCoordinateTransform seq )
	{
		return toAffine3D(
			Arrays.stream( seq.getTransformations() )
				.map( x -> { return CoordinateTransform.create( x ); 	} )
				.collect( Collectors.toList() ));
	}

//	public static AffineTransform3D toAffine3D( ByDimensionCoordinateTransform byDimension )
//	{
//
//	}

	public static AffineTransform3D toAffine3D( Collection<CoordinateTransform<?>> transforms )
	{
		return toAffine3D( null, transforms );
	}

	public static AffineGet toAffine( CoordinateTransform< ? > transform, int nd ) {
		
		switch (transform.getType()) {
		
		case IdentityCoordinateTransform.TYPE:
			return new AffineTransform(nd);
		case ScaleCoordinateTransform.TYPE:
		case TranslationCoordinateTransform.TYPE:
		case RotationCoordinateTransform.TYPE:
		case AffineCoordinateTransform.TYPE:
			return (AffineGet)transform.getTransform();
		case SequenceCoordinateTransform.TYPE:
			final SequenceCoordinateTransform seq = (SequenceCoordinateTransform) transform;
			if (seq.isAffine())
				return seq.asAffine(nd);
			else
				return null;
		default:
			return null;
		}
	}

	public static AffineTransform3D toAffine3D( N5Reader n5, Collection<CoordinateTransform<?>> transforms )
	{
		final AffineTransform3D total = new AffineTransform3D();
		for( final CoordinateTransform<?> ct : transforms )
		{
			if( ct instanceof IdentityCoordinateTransform )
				continue;
			else if( ct instanceof SequenceCoordinateTransform )
			{
				final AffineTransform3D t = toAffine3D( (SequenceCoordinateTransform)ct );
				if( t == null )
					return null;
				else
					preConcatenate( total, (AffineGet) t  );
			}
			else {
				final Object t = ct.getTransform(n5);
				if( t instanceof AffineGet )
				{
					preConcatenate( total, (AffineGet) t  );
				}
				else
					return null;
			}
		}
		return total;
	}

	public static void preConcatenate( AffineTransform3D tgt, AffineGet concatenate )
	{
		if( concatenate.numTargetDimensions() >= 3 )
			tgt.preConcatenate(concatenate);
		else if( concatenate.numTargetDimensions() == 2 )
		{
			final AffineTransform3D c = new AffineTransform3D();
			c.set(
					concatenate.get(0, 0), concatenate.get(0, 1), 0, concatenate.get(0, 2),
					concatenate.get(1, 0), concatenate.get(1, 1), 0, concatenate.get(1, 2),
					0, 0, 1, 0);

			tgt.preConcatenate(c);
		}
		else if( concatenate.numTargetDimensions() == 1 )
		{
			final ScaleAndTranslation c = new ScaleAndTranslation(
					new double[]{ 1, 1, 1 },
					new double[]{ 0, 0, 0});
			tgt.preConcatenate(c);
		}
	}

	public static < T extends NumericType< T > > InterpolatorFactory< T, RandomAccessible< T > > interpolator( String interpolator )
	{
		switch( interpolator )
		{
		case AbstractParametrizedFieldTransform.LINEAR_INTERPOLATION:
			return new NLinearInterpolatorFactory<T>();
		case AbstractParametrizedFieldTransform.NEAREST_INTERPOLATION:
			return new NearestNeighborInterpolatorFactory<>();
		default:
			return null;
		}
	}

	public static double squaredDistance(Double[] p, RealLocalizable q) {

		double dist = 0;
		for (int j = 0; j < p.length; j++) {
			dist += (p[j].doubleValue() - q.getDoublePosition(j)) * (p[j].doubleValue() - q.getDoublePosition(j));
		}
		return dist;
	}

	public static double distance(Double[] p, RealLocalizable q) {

		return Math.sqrt(squaredDistance(p, q));
	}

	public static double squaredDistance(RealLocalizable p, final RealLocalizable q) {

		double dist = 0;
		for (int j = 0; j < p.numDimensions(); j++)
			dist += (p.getDoublePosition(j) - q.getDoublePosition(j)) * (p.getDoublePosition(j) - q.getDoublePosition(j));

		return dist;
	}

	public static double distance(final RealLocalizable p, final RealLocalizable q) {

		return Math.sqrt(squaredDistance(p, q));
	}
}

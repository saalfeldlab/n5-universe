package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AbstractParametrizedFieldTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.TranslationCoordinateTransform;

import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.NumericType;

public class TransformUtils
{

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

	public static AffineTransform3D toAffine3D( SequenceCoordinateTransform seq )
	{
		return toAffine3D(
			Arrays.stream( seq.getTransformations() )
				.map( x -> { return CoordinateTransform.create( x ); 	} )
				.collect( Collectors.toList() ));
	}

	public static AffineTransform3D toAffine3D( Collection<CoordinateTransform<?>> transforms )
	{
		return toAffine3D( null, transforms );
	}

	public static AffineGet toAffine( CoordinateTransform< ? > transform, int nd )
	{
		if( transform.getType().equals( ScaleCoordinateTransform.TYPE ))
		{
			return ((ScaleCoordinateTransform)transform).getTransform();
		}
		else if( transform.getType().equals( TranslationCoordinateTransform.TYPE ))
		{
			return ((TranslationCoordinateTransform)transform).getTransform();
		}
		else if( transform.getType().equals( SequenceCoordinateTransform.TYPE ))
		{
			final SequenceCoordinateTransform seq = (SequenceCoordinateTransform) transform;
			if( seq.isAffine() )
				return seq.asAffine( nd );
			else
				return null;
		}
		else
			return null;
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
	//				total.preConcatenate((AffineGet) t );
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

}

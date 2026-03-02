package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import java.util.ArrayList;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.RealViewsSimplifyUtils;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

public class TransformationUtils {

	public static AffineGet tranformsToAffine(final OmeNgffDataset dataset, final CoordinateTransformation<?>[] transforms )
	{
		final ArrayList<CoordinateTransformation<?>> l = new ArrayList<>();

		if( dataset.coordinateTransformations != null )
			l.addAll( Arrays.asList( dataset.coordinateTransformations ) );

		if( transforms != null )
			l.addAll( Arrays.asList( transforms ) );

		return buildTransform( l.stream().toArray(CoordinateTransformation[]::new));
	}

	public static AffineGet buildTransform( final CoordinateTransformation<?>[] transforms ) {

		AffineTransform out = null;
		for( final CoordinateTransformation<?> ct : transforms )
		{
			if (out == null)
				out = new AffineTransform(ct.getTransform().numSourceDimensions());

			out.preConcatenate(ct.getTransform());
		}

		if( out == null )
			return null;
		else
			return simplifyAffineGet( out );
	}

	/*
	 * Simplifies an AffineGet to it's most specific class (e.g. Translation2D)
	 */
	private static AffineGet simplifyAffineGet( final AffineGet affineGet )
	{
		final int n = affineGet.numDimensions();

		if (  RealViewsSimplifyUtils.isExlusiveTranslation( affineGet ) )
		{
			final double[] translations = new double[ n ];

			for ( int d = 0; d < n; d++ )
			{
				translations[ d ] = affineGet.get( d, n );
			}

			if ( n == 2 )
			{
				return new Translation2D( translations );
			}
			else if ( n == 3 )
			{
				return new Translation3D( translations );
			}
			else
			{
				return new Translation( translations );
			}
		}
		else if ( RealViewsSimplifyUtils.isExclusiveScale( affineGet ) )
		{

			final double[] scalings = new double[ n ];

			for ( int d = 0; d < n; d++ )
			{
				scalings[ d ] = affineGet.get( d, d );
			}

			if ( n == 2 )
			{
				return new Scale2D( scalings );
			}
			else if ( n == 3 )
			{
				return new Scale3D( scalings );
			}
			else
			{
				return new Scale( scalings );
			}
		}
		else if ( RealViewsSimplifyUtils.isExclusiveScaleAndTranslation( affineGet ) )
		{
			final double[] s = new double[ n ];
			final double[] t = new double[ n ];
			for ( int d = 0; d < n; d++ )
			{
				s[ d ] = affineGet.get( d, d );
				t[ d ] = affineGet.get( d, n );
			}

			return new ScaleAndTranslation( s, t );

		}
		return ( AffineGet ) affineGet.copy();
	}

}

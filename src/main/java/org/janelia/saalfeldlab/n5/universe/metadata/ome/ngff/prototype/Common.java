package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.SequenceCoordinateTransform;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class Common {

	public static AffineTransform3D toAffine3D( final SequenceCoordinateTransform seq )
	{
		return toAffine3D( Arrays.asList( seq.getTransformations()));
	}

	public static AffineTransform3D toAffine3D( final Collection<CoordinateTransform<?>> transforms )
	{
		return toAffine3D( null, transforms );
	}

	public static AffineTransform3D toAffine3D( final N5Reader n5, final Collection<CoordinateTransform<?>> transforms )
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

	public static void preConcatenate( final AffineTransform3D tgt, final AffineGet concatenate )
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

	public static CoordinateSystem makeSpace( final String name, final String type, final String unit, final String... labels)
	{
		return new CoordinateSystem( name,
				Arrays.stream(labels)
					.map( x -> new Axis(x, type, unit ))
					.toArray( Axis[]::new ));
	}

	public static CoordinateSystem makeDfieldSpace( final String name, final String type, final String unit, final String... labels)
	{
		final Axis[] axes = Stream.concat(
				Stream.of( new Axis( "d", "displacement", unit)),
				Arrays.stream(labels).map( x -> new Axis(x, type, unit )))
			.toArray( Axis[]::new );

		return new CoordinateSystem( name, axes);
	}

	public static <T extends NativeType<T> & NumericType<T>> RandomAccessibleInterval<T> open( final N5Reader n5, final String dataset ) throws IOException
	{
		@SuppressWarnings("unchecked")
		final CachedCellImg<T, ?> imgRaw = (CachedCellImg<T, ?>) N5Utils.open(n5, dataset);
		final RandomAccessibleInterval<T> img;
		if( imgRaw.numDimensions() == 2)
			img = Views.addDimension(imgRaw, 0, 0);
		else
			img = imgRaw;

		return img;
	}

	public static <T extends NumericType<T> & NativeType<T>> RealRandomAccessible<T> transformImage(
			final N5Reader n5, final String imageDataset, final String registrationDataset, final String space ) throws IOException {
		final TransformGraph graph = Common.openGraph(n5, registrationDataset, imageDataset );
		final Optional<RealTransform> t = graph.path(space,"").map( p -> p.totalTransform(n5));
		System.out.println( t );

		final RandomAccessibleInterval<T> img = open( n5 , imageDataset );
		final RealRandomAccessible<T> rra = Views.interpolate( Views.extendZero( img ), new NLinearInterpolatorFactory<T>());
		if( t.isPresent() )
			return new RealTransformRandomAccessible<>(rra, t.get() );
		else
			return rra;

//		Source<T> src;
//		try {
//			src = openSource( n5, imageDataset, "" );
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}

//		final WarpedSource<T> wsrc = new WarpedSource<T>( src, imageDataset + " - " + space );
//		t.ifPresent( x -> wsrc.updateTransform(x));
//		wsrc.setIsTransformed(true);
//		return wsrc;

//		WarpedSource<T> ws = graph.path(space,"").map( p -> {
//			Source<T> src;
//			try {
//				src = openSource( n5, imageDataset, "" );
//			} catch (IOException e) {
//				e.printStackTrace();
//				return null;
//			}
//
//			final WarpedSource<T> wsrc = new WarpedSource<T>( src, imageDataset + " - " + space );
//			final RealTransform t = p.totalTransform();
//			wsrc.updateTransform(t);
//			wsrc.setIsTransformed(true);
//			return wsrc;
//		}).orElse(null);

//		return ws;
//		return null;
	}

	public static TransformGraph openGraph( final N5Reader n5 )
	{
		return openGraph( n5, "/" );
	}

	public static TransformGraph openGraph( final N5Reader n5, final String dataset )
	{
		int nd = 5;
		if( n5.datasetExists( dataset ))
			nd = n5.getDatasetAttributes( dataset ).getNumDimensions();

		return openGraph( n5, dataset, nd );
	}

	public static TransformGraph openGraph( final N5Reader n5, final String dataset, final int nd )
	{
		CoordinateSystem[] spaces = n5.getAttribute(dataset, "spaces", CoordinateSystem[].class);
		if( spaces == null )
			spaces = n5.getAttribute(dataset, "coordinateSystems", CoordinateSystem[].class);

		CoordinateTransform<?>[] transforms = n5.getAttribute(dataset, "transformations", CoordinateTransform[].class);
		if( transforms == null )
			transforms = n5.getAttribute(dataset, "coordinateTransformations", CoordinateTransform[].class);

//		return new TransformGraph( Arrays.asList( transforms ), Arrays.asList(spaces));
		return new SpacesTransforms( spaces, transforms ).buildTransformGraph(dataset, nd);
	}

	public static TransformGraph openGraph( final N5Reader n5, final String... datasets ) throws IOException
	{
		TransformGraph graph = null;
		for( final String d : datasets )
			if( graph == null)
				graph = openGraph( n5, d );
			else
				graph.add( openGraph( n5, d ));

		return graph;
	}

	public static double[] translation( final Interval itvl, final double[] centerPhysical )
	{
		final int nd = itvl.numDimensions();
		final double[] tParams = new double[ nd ];
		for( int i = 0; i < nd; i++ ) {
			tParams[ i ] =  centerPhysical[i] - ((itvl.realMax(i) - itvl.realMin(i)) / 2);
//			tParams[ i ] =  ((itvl.realMax(i) - itvl.realMin(i)) / 2) - centerPhysical[i];
		}
		return tParams;
	}

	public static double[] translation( final Interval itvl, final double[] centerPhysical, final double[] resolution )
	{
		final int nd = itvl.numDimensions();
		final double[] tParams = new double[ nd ];
		for( int i = 0; i < nd; i++ ) {
//			tParams[ i ] =  centerPhysical[i] - ((itvl.realMax(i) - itvl.realMin(i)) / 2);
			tParams[ i ] =  (resolution[i] * (itvl.realMax(i) - itvl.realMin(i)) / 2) - centerPhysical[i];
		}
		return tParams;
	}

}

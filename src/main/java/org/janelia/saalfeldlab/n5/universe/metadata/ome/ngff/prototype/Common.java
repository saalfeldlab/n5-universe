package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.SequenceCoordinateTransform;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.KDTree;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorARGBFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.parallel.DefaultTaskExecutor;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.SubsampleIntervalView;
import net.imglib2.view.SubsampleView;
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

	public static <T extends NumericType<T> & NativeType<T>> RealRandomAccessible<T> rra( final Source<T> src ) throws IOException
	{
		if( src instanceof RandomAccessibleIntervalSource )
		{
			final AffineTransform3D xfm = new AffineTransform3D();
			src.getSourceTransform(0, 0, xfm);
			System.out.println( "rra xfm: " + xfm );
			return RealViews.transform( src.getInterpolatedSource(0, 0, Interpolation.NLINEAR), xfm.inverse() );
		}
		else
			return null;

//		else if(src instanceof WarpedSource )
//		{
//
//		}
//		else
//		return null;
	}

	public static <T extends NumericType<T> & NativeType<T>> RandomAccessibleIntervalSource<T> openSource( final N5Reader n5, final String dataset, final String spaceIn ) throws IOException
	{
		final String space = spaceIn == null ? "" : spaceIn;

		final TransformGraph graph = Common.buildGraph(n5, dataset );
		final RandomAccessibleInterval<T> img = open( n5 , dataset);
		final AffineTransform3D xfm = graph.path("", space).get().totalAffine3D();
		System.out.println( "xfm : " + xfm );
		return new RandomAccessibleIntervalSource<T>(img, Util.getTypeFromInterval(img), xfm, dataset + " - " + space );
	}

	public static <T extends NumericType<T> & NativeType<T>> RandomAccessibleIntervalSource<T> openSource( final N5Reader n5, final String dataset, final TransformGraph graph, final String spaceIn ) throws IOException
	{
		final String space = spaceIn == null ? "" : spaceIn;
		final RandomAccessibleInterval<T> img = open( n5 , dataset);
		final AffineTransform3D xfm = graph.path("", space).get().totalAffine3D();
		return new RandomAccessibleIntervalSource<T>(img, Util.getTypeFromInterval(img), xfm, dataset + " - " + space );
	}

	public static <T extends RealType<T> & NativeType<T>> N5Source<T> openMultiscaleSource( final N5Reader n5, final String base ) throws IOException
	{
		final Multiscale[] multiscales = n5.getAttribute(base, "multiscales", Multiscale[].class );
		final Multiscale ms = multiscales[0];

		final int N = ms.datasets.length;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval<T>[] images = new RandomAccessibleInterval[ N ];
		final AffineTransform3D[] transforms = new AffineTransform3D[ N ];

		int i = 0 ;
		for( final DatasetTransform d : ms.datasets )
		{
			RandomAccessibleInterval<T> img = N5Utils.open(n5, d.path);
			if( img.numDimensions() == 2 )
				img = Views.addDimension(img, 0, 0 );

			images[i] = img;
			transforms[i] = Common.toAffine3D(Arrays.asList(d.coordinateTransformations));

			System.out.println( d.path );
			System.out.println( transforms[i]);

			i++;
		}

		return new N5Source<T>(Util.getTypeFromInterval(images[0]), base, images, transforms);
	}

//	public static <T extends NumericType<T> & NativeType<T>> WarpedSource<T> openWarpedSource( N5Reader n5, String dataset, TransformGraph graph, String spaceIn ) throws IOException
//	{
//		String space = spaceIn == null ? "" : spaceIn;
//		final RandomAccessibleInterval<T> img = open( n5 , dataset);
//		RandomAccessibleIntervalSource<T> src = new RandomAccessibleIntervalSource<T>(img, Util.getTypeFromInterval(img), new AffineTransform3D(), dataset );
//		WarpedSource<T> wsrc = new WarpedSource<>( src, dataset + " - " + space );
//
//		final RealTransform xfm = graph.path("", space).get().totalTransorm();
//		wsrc.updateTransform(xfm);
//		wsrc.setIsTransformed(true);
//		return wsrc;
//	}

	public static <T extends NumericType<T> & NativeType<T>> void show(
			final N5Reader n5, final String imageDataset, final String registrationDataset, final String space, final Interval interval,
			final BdvOptions opts )
	{
		RealRandomAccessible<T> rimg;
		try {

			rimg = transformImage( n5, imageDataset, registrationDataset, space );
			BdvFunctions.show(rimg, interval, imageDataset + " - " + space, opts );

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static <T extends NumericType<T> & NativeType<T>> RealRandomAccessible<T> transformImage(
			final N5Reader n5, final String imageDataset, final String registrationDataset, final String space ) throws IOException {
		final TransformGraph graph = Common.buildGraph(n5, registrationDataset, imageDataset );
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

	public static TransformGraph buildGraph( final N5Reader n5 ) throws IOException
	{
		return buildGraph( n5, "/" );
	}

	public static TransformGraph buildGraph( final N5Reader n5, final String dataset ) throws IOException
	{
		int nd = 5;
		if( n5.datasetExists( dataset ))
			nd = n5.getDatasetAttributes( dataset ).getNumDimensions();

		return buildGraph( n5, dataset, nd );
	}

	public static TransformGraph buildGraph( final N5Reader n5, final String dataset, final int nd ) throws IOException
	{
		CoordinateSystem[] spaces = n5.getAttribute(dataset, "spaces", Space[].class);
		if( spaces == null )
			spaces = n5.getAttribute(dataset, "coordinateSystems", Space[].class);

		CoordinateTransformation[] transforms = n5.getAttribute(dataset, "transformations", CoordinateTransform[].class);
		if( transforms == null )
			transforms = n5.getAttribute(dataset, "coordinateTransformations", CoordinateTransform[].class);

//		return new TransformGraph( Arrays.asList( transforms ), Arrays.asList(spaces));
		return new SpacesTransforms( spaces, transforms ).buildTransformGraph(dataset, nd);
	}

	public static TransformGraph buildGraph( final N5Reader n5, final String... datasets ) throws IOException
	{
		TransformGraph graph = null;
		for( final String d : datasets )
			if( graph == null)
				graph = buildGraph( n5, d );
			else
				graph.add( buildGraph( n5, d ));

		return graph;
	}

	/**
	 * Returns an infinite stream of random points on a circle of the given radius.
	 *
	 * @param radius the radius
	 * @return random points
	 */
	public static Stream<RealPoint> circleSamples( final RealPoint center, final double radius, final double radiusJitter )
	{
		return Stream.generate( () -> {
			final double tht = 2 * Math.PI * Math.random();
			final double r = radius + radiusJitter * Math.random();
			return new RealPoint(
					center.getDoublePosition(0) + r * Math.cos(tht),
					center.getDoublePosition(1) + r * Math.sin(tht));
		});
	}

	/**
	 * Returns an infinite stream of random points on a circle of the given radius.
	 *
	 * @param radius the radius
	 * @return random points
	 */
	public static Stream<RealPoint> coneSamples( final RealPoint center, final double radius, final double radiusJitter, final double height )
	{
		return Stream.generate( () -> {
			final double tht = 2 * Math.PI * Math.random();
			final double r = radius + radiusJitter * Math.random();
			final double z = height * ( 1 - quadSampling());
			final double z0 = center.getDoublePosition(2) - (height / 2);
			return new RealPoint(
					center.getDoublePosition(0) + (z/radius) * r * Math.cos(tht),
					center.getDoublePosition(1) + (z/radius) * r * Math.sin(tht),
					z0 + z );
		});
	}

	public static double quadSampling()
	{
		final double x = Math.random();
		return x * x;
	}

	public static <T extends RealType<T>> RealRandomAccessible<T> ptsImage(
			final T type,
			final Stream<RealPoint> pts,
			final double searchDist )
	{
		final List<RealPoint> ptList = pts.collect(Collectors.toList());

		final List<T> valList = Stream.generate( () ->  {
			final T out = type.copy();
			out.setReal( 1.0 );
			return out;
			}).limit(ptList.size()).collect(Collectors.toList());

		final DoubleUnaryOperator rbf = rbf( searchDist );
		final KDTree<T> tree = new KDTree< T >( valList, ptList );
		final RBFInterpolator.RBFInterpolatorFactory< T > interp =
				new RBFInterpolator.RBFInterpolatorFactory< T >(
						rbf, searchDist, false,
						tree.firstElement().copy() );

		return Views.interpolate( tree, interp );
	}

	public static <T extends RealType<T>> void render( final RandomAccessibleInterval<T> img,
			final Stream<RealPoint> pts,
			final double searchDist )
	{
		final RealRandomAccessible<T> rimg = ptsImage( Util.getTypeFromInterval(img),  pts, searchDist );
		final RealRandomAccess<T> rra = rimg.realRandomAccess();

		final Cursor<T> c = Views.flatIterable(img).cursor();
		while( c.hasNext())
		{
			c.fwd();
			rra.setPosition(c);
			c.get().add(rra.get());
		}

	}

	public static DoubleUnaryOperator rbf( final double searchRad ) {
		return new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				if( x > searchRad )
					return 0;
				else if( x <= 0 )
					return 1.0;
				else
					return searchRad - x;
			}
		};
	}
	public static <T> SubsampleIntervalView<T> downsample( final RandomAccessibleInterval<T> img, final long... factors )
	{
		return Views.subsample(img, factors);
	}

	public static <T extends RealType<T>> Img<T> downsampleAvg( final RandomAccessibleInterval<T> img, final int factor )
	{
		final GeneralRectangleShape shp = new GeneralRectangleShape( factor, -factor / 2 , false );
		final SubsampleView<Neighborhood<T>> nbrhoods = Views.subsample( shp.neighborhoodsRandomAccessible(Views.extendZero(img)), factor );
		final RandomAccess<Neighborhood<T>> nbrhoodsRa = nbrhoods.randomAccess();

		final long[] factors = new long[ img.numDimensions()];
		Arrays.fill(factors, factor);

		final SubsampleIntervalView<T> z = Views.subsample( img, factors );
		final Img<T> out = Util.getSuitableImgFactory(z, Util.getTypeFromInterval(img)).create(z);
		final Cursor<T> c = out.cursor();

		double N = 0;
		while( c.hasNext())
		{
			N = 0;
			c.fwd();
			nbrhoodsRa.setPosition(c);
			final Neighborhood<T> n = nbrhoodsRa.get();
			final Cursor<T> nc = n.cursor();
			while( nc.hasNext() ) {
				c.get().add(nc.next());
				N++;
			}
			c.get().mul(1 / N);
		}

		return out;
	}

	public static <T extends NumericType<T>> Img<T> downsampleAvg( final RandomAccessibleInterval<T> img )
	{
		final SubsampleIntervalView<T> z = Views.subsample( img, 2, 2 );
		final SubsampleIntervalView<T> x = Views.subsample( Views.translate(img, 1, 0 ), 2, 2 );
		final SubsampleIntervalView<T> y = Views.subsample( Views.translate(img, 1, 0 ), 2, 2 );
		final SubsampleIntervalView<T> xy = Views.subsample( Views.translate(img, 1, 1 ), 2, 2 );
		final RandomAccessibleInterval<T> vol = Views.stack(z,x,y,xy);
		final RandomAccess<T> ra = vol.randomAccess();

		final Img<T> out = Util.getSuitableImgFactory(z, Util.getTypeFromInterval(img)).create(z);
		final Cursor<T> c = out.cursor();
		final int stackDim = vol.numDimensions()-1;

		while( c.hasNext())
		{
			c.fwd();
			ra.setPosition(0, stackDim );
			ra.setPosition(c);
			for( int i = 0; i < stackDim; i++ ) {
				c.get().add(ra.get());
				ra.fwd(stackDim);
			}
		}
		return out;
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

	public static <T extends RealType<T> & NativeType<T>> RandomAccessibleIntervalSource<T> image(
			final T type, final RealRandomAccessible<T> data,
			final double[] resolution, final double[] centerPhysical, final Interval itvl, final RealTransform distortion,
			final int nThreads )
	{
		assert( data.numDimensions() == itvl.numDimensions() );

//		int nd = data.numDimensions();

//		double[] tParams = translation( itvl, centerPhysical, resolution );
		final double[] tParams = translation( itvl, centerPhysical );
//		double[] tParams = new double[nd];

		final ScaleAndTranslation st = new ScaleAndTranslation( resolution, tParams);
		final AffineTransform3D xfm = new AffineTransform3D();
		xfm.scale( resolution[0], resolution[1], resolution[2] );


		RealTransform totalTransform;
		if( distortion == null )
			totalTransform = st;
		else
		{
			final RealTransformSequence seq = new RealTransformSequence();
			seq.add( distortion );
			seq.add( st );
			totalTransform = seq;
		}

		final Img<T> img = Util.getSuitableImgFactory(itvl, type ).create(itvl);
		final RealTransformRandomAccessible<T,?> rra = new RealTransformRandomAccessible< >( data, totalTransform );
		final IntervalView<T> imgVirt = Views.interval(Views.raster(rra), itvl );

		final DefaultTaskExecutor exec = new DefaultTaskExecutor( Executors.newFixedThreadPool(nThreads));
		LoopBuilder.setImages(imgVirt, img).multiThreaded(exec).forEachPixel( (x,y) -> y.set(x));

		final RandomAccessibleIntervalSource<T> src = new RandomAccessibleIntervalSource<T>(
				img, Util.getTypeFromInterval(img), xfm, "img");

		return src;
	}
}

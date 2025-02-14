package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AffineCoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ReferencedCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public class Common {

	public static AffineGet removeDimension( final int dim, final AffineGet in )
	{
		final int nd = in.numSourceDimensions();
		final int ndout = nd - 1;
		final double[] outMtx = new double[ndout * (ndout + 1)];

		// iterate over the original matrix, taking only the values that belong in the new matrix
		int k = 0;
		for( int i = 0; i < nd; i++ ) {

			if( i == dim )
				continue;

			for( int j = 0; j < nd+1; j++ ) {

				if( j == dim )
					continue;
				else
					outMtx[k++] = in.get(i, j);
			}
		}

		return new AffineTransform(outMtx);
	}

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

	public static CoordinateSystem makeSpace( final String name, final String type, final Unit unit, final String... labels)
	{
		return new CoordinateSystem( name,
				Arrays.stream(labels)
					.map( x -> new Axis(type, x, unit.name() ))
					.toArray( Axis[]::new ));
	}

	public static CoordinateSystem makeSpace( final String name, final String type, final String unit, final String... labels)
	{
		return new CoordinateSystem( name,
				Arrays.stream(labels)
					.map( x -> new Axis(type, x, unit ))
					.toArray( Axis[]::new ));
	}

	public static CoordinateSystem makeDfieldSpace( final String name, final String type, final String unit, final String... labels)
	{
		final Axis[] axes = Stream.concat(
				Stream.of( new Axis( "d", "displacement", unit)),
				Arrays.stream(labels).map( x -> new Axis(type, x, unit )))
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

	public static TransformGraph openGraph( final String uri ) {

		try {

			final N5URI n5uri = new N5URI(uri);
			final N5Reader n5 = new N5Factory().gsonBuilder(gsonBuilder()).openReader(n5uri.getContainerPath());
			return openGraph(n5, n5uri.getGroupPath());

		} catch (URISyntaxException e) {}
		return null;
	}

	public static TransformGraph openGraph( final N5Reader n5 )
	{
		return openGraph( n5, "/" );
	}

	public static TransformGraph openGraph( final N5Reader n5, final String group )
	{
		int nd = 5;
		if( n5.datasetExists( group ))
			nd = n5.getDatasetAttributes( group ).getNumDimensions();

		return openGraph( n5, group, nd );
	}

	public static TransformGraph openGraph( final N5Reader n5, final String group, final int nd )
	{
		CoordinateSystem[] spaces = n5.getAttribute(group, "coordinateSystems", CoordinateSystem[].class);
		if( spaces == null )
			spaces = n5.getAttribute(group, "coordinateSystems", CoordinateSystem[].class);

		if( spaces == null )
			return openGraphImpliedSpaces( n5, group, nd );

		CoordinateTransform<?>[] transforms = n5.getAttribute(group, "coordinateTransformations", CoordinateTransform[].class);
		if( transforms == null )
			transforms = n5.getAttribute(group, "transformations", CoordinateTransform[].class);

//		return new TransformGraph( Arrays.asList( transforms ), Arrays.asList(spaces));
		return new SpacesTransforms( spaces, transforms ).buildTransformGraph(group, nd);
	}

	protected static TransformGraph openGraphImpliedSpaces(final N5Reader n5, final String dataset, final int nd) {

		CoordinateTransform<?>[] transforms = n5.getAttribute(dataset, "coordinateTransformations", CoordinateTransform[].class);
		if (transforms == null)
			transforms = n5.getAttribute(dataset, "transformations", CoordinateTransform[].class);

		final CoordinateSystem[] coordinateSystems = Arrays.stream(transforms).flatMap(x -> {
			return Stream.of(
					new CoordinateSystem(x.getInput(), nd),
					new CoordinateSystem(x.getOutput(), nd));
		}).toArray(N -> {
			return new CoordinateSystem[N];
		});

		return new SpacesTransforms(coordinateSystems, transforms).buildTransformGraph(dataset, nd);
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

	public static Pair<CoordinateTransform<?>,N5Reader> openTransformN5( final String url )
	{
		try
		{
			final N5URI n5url = new N5URI( url );
			final String loc = n5url.getContainerPath();
			if( loc.endsWith( ".json" ))
			{
				return new ValuePair<>( openJson( url ), null);
			}
			else
			{
				final N5Reader n5 = new N5Factory().gsonBuilder( gsonBuilder() ).openReader( loc );
				final String dataset = n5url.getGroupPath() != null ? n5url.getGroupPath() : "/";
				final String attribute = n5url.getAttributePath();
//				final String attribute = n5url.getAttributePath() != null ? n5url.getAttributePath() : "coordinateTransformations[0]";

				try {
					final CoordinateTransform<?> ct = n5.getAttribute(dataset, attribute, CoordinateTransform.class);
					return new ValuePair<>( ct, n5 );
				} catch( N5Exception | ClassCastException e ) {}

				try {
					return openReference( url, n5, dataset, attribute ); // try to open a reference
				} catch( N5Exception | ClassCastException e ) {}

//				final CoordinateTransform<?> nct = CoordinateTransform.create(ct);
//				return new ValuePair<>( nct, n5 );
			}
		}
		catch ( final URISyntaxException e ) { }

		return null;
	}

	@SuppressWarnings("unchecked")
	public static < T extends RealTransform> T open( final N5Reader n5, final String dataset, final String input, final String output )
	{
		// TODO error handling
		final TransformGraph g = Common.openGraph( n5, dataset );
		return (T)g.path( input, output ).get().totalTransform( n5, g );
	}

	public static RealTransform open( final String url )
	{
		final Pair< CoordinateTransform< ? >, N5Reader > pair = openTransformN5( url );
		return pair.getA().getTransform( pair.getB() );
	}

	public static Pair<CoordinateTransform<?>,N5Reader> openReference( final String url, final N5Reader n5, final String dataset, final String attribute) {

		final ReferencedCoordinateTransform ref = n5.getAttribute(dataset, attribute, ReferencedCoordinateTransform.class);
		if( ref == null )
			return null;
		else if( url != null && url.equals( ref.getUrl() ))
			return null; // avoid self-reference
		else
			return openTransformN5( ref.getUrl());
	}

	public static CoordinateTransform<?> openJson( final String url )
	{
		final Path path = Paths.get( url );
		String string;
		try
		{
			string = new String(Files.readAllBytes(path));
		}
		catch ( final IOException e )
		{
			return null;
		}

		final Gson gson = gsonBuilder().create();
		final JsonElement elem = gson.fromJson( string, JsonElement.class );

//		final CoordinateTransformation ct = gson.fromJson( elem.getAsJsonArray().get( 0 ), CoordinateTransformation.class );
		final CoordinateTransform<?> ct = gson.fromJson( elem, CoordinateTransform.class );
		if( ct != null )
		{
			final CoordinateTransform< ? > nct = CoordinateTransform.create( ct );
			return nct;
		} else if( elem.isJsonObject()) {
			// TODO figure out what should be returned here
			final String refUrl = elem.getAsJsonObject().get("url").getAsString();
			if( url.equals( refUrl ))
				return null; //avoid self-reference
			else
				return openTransformN5( refUrl ).getA();
		}
		return null;
	}

	public static GsonBuilder gsonBuilder() {

		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(AffineCoordinateTransform.class, new AffineCoordinateTransformAdapter());
		gb.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter());
		return gb;
	}

}

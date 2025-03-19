package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.exampleData;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.TranslationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffMultiscalesBuilder;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfigAdapter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

import com.google.gson.GsonBuilder;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class CoordinateTransformStitchingExample2d <T extends NativeType<T>>
{

	final static String VERSION = "0.6";

	String path = "/home/john/data/ngff/stitching_examples_2d/stitched_tiles.zarr";

	long[] expandAmount = new long[]{ 12, 12 };

	Img< T > baseImg;

	List< RandomAccessibleInterval< T > > tiles;

	List< CoordinateTransform<?> > tileTransforms;
	
	N5Writer n5;

	public static void main( String[] args ) {

		CoordinateTransformStitchingExample2d ex = new CoordinateTransformStitchingExample2d();
		ex.makeWriter( ex.path );

		ex.readBaseImage();
		ex.generateTiles();
		ex.writeTilesAndTransforms();

//		ex.viewRawTiles();
	}
	
	public void viewRawTiles() {

		final N5Writer n5 = makeWriter(path);

		tiles = new ArrayList<>();

		BdvOptions opts = BdvOptions.options().is2D();
		BdvStackSource< T > bdv = null;

		for( String dset : n5.list( "" )) {
			CachedCellImg< T, ? > tile = N5Utils.open( n5, dset );
			tiles.add( tile );
			
			 BdvStackSource< T > handle = BdvFunctions.show(tile, dset, opts);
			 if( bdv == null ) {
				 bdv = handle;
				 opts.addTo( bdv );
			 }
		}
		
	}

	public void readBaseImage() {
		ImagePlus imp = IJ.openImage("/home/john/tmp/boats.tif");
		baseImg = ( Img< T > ) ImageJFunctions.wrap(imp);	
		System.out.println( Intervals.toString( baseImg ));
	}

	public void generateTiles() { 
		
		// [(0, 0) -- (719, 575) = 720x576]

		ArrayList<Interval> intervals = new ArrayList<>();
		intervals.add( Intervals.createMinMax( 0, 0, 359, 287 ) );
		intervals.add( Intervals.createMinMax( 360, 0, 719, 287 ) );
		intervals.add( Intervals.createMinMax( 0, 288, 359, 575 ) );
		intervals.add( Intervals.createMinMax( 360, 288, 719, 575 ) );

		tiles = intervals.stream().map( i -> {
			final FinalInterval itvl = Intervals.intersect(
				baseImg,
				Intervals.expand( i, expandAmount ));
			return Views.interval( baseImg, itvl );
		}).collect( Collectors.toList() );

	}

	public void writeTilesAndTransforms() {

		CoordinateSystem worldCs = Common.makeSpace("world", "space", Unit.micrometer, "x", "y");

		tileTransforms = new ArrayList< CoordinateTransform<?> >();
		final ArrayList< CoordinateSystem > coordinateSystems = new ArrayList< CoordinateSystem >();
		coordinateSystems.add( worldCs );

		for ( int i = 0; i < tiles.size(); i++ ) {

			final int[] blkSize = Arrays.stream( tiles.get( i ).dimensionsAsLongArray() )
					.mapToInt( j -> ( int ) j ).toArray();

			final String dset = "tile_" + i;
			final String arrayDset = dset + "/0";
			final String physical = "tile_" + i + "_mm";

			N5Utils.save( tiles.get( i ), n5, arrayDset, blkSize, new ZstandardCompression() );

			OmeNgffV06MultiScaleMetadata ms = buildMultiscales( "0", physical,
				new ScaleCoordinateTransform(dset + " to physical", dset, physical, new double[]{1,1}));
			
			coordinateSystems.add( ms.coordinateSystems[0] );

			n5.setAttribute(dset, "version", VERSION);
			n5.setAttribute(dset, "ome/multiscales", ms);

			tileTransforms.add( new TranslationCoordinateTransform(
					physical + " to " + worldCs.getName(), 
					physical, 
					worldCs.getName(),
					tiles.get( i ).minAsDoubleArray()));
		}

		final String ctGroup = "coordinateTransformations";
		n5.createGroup( ctGroup );
		n5.setAttribute( ctGroup, "ome/" + CoordinateTransform.KEY, tileTransforms );
		n5.setAttribute( ctGroup, "ome/" + CoordinateSystem.KEY, coordinateSystems );
	}

	public OmeNgffV06MultiScaleMetadata buildMultiscales(final String dset, final String output, final CoordinateTransform<?> ct) {

		CoordinateSystem cs = Common.makeSpace(output, "space", Unit.micrometer, "x", "y");
		OmeNgffMultiscalesBuilder builder = new OmeNgffMultiscalesBuilder().addCoordinateSystem( cs ).put( dset, ct );
		return builder.build();
	}

	public N5Writer makeWriter(final String path) {

		final GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter());

		final FileSystemKeyValueAccess kva = new FileSystemKeyValueAccess(FileSystems.getDefault());
		n5 = new ZarrV3KeyValueWriter( kva, path, gsonBuilder, false, false, "/", false );
		return n5;
	}
}

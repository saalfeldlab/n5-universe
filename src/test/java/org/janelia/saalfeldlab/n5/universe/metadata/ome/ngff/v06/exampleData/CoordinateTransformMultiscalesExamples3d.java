package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.exampleData;

import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AffineCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.BijectionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ByDimensionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.InverseCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.MapAxisCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.TranslationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffMultiscalesBuilder;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.RotationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfigAdapter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.cell.CellRandomAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.inverse.RealTransformFiniteDerivatives;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.GenericComposite;

public class CoordinateTransformMultiscalesExamples3d {

	public static void main(String[] args) {

		final CoordinateTransformMultiscalesExamples3d ex = new CoordinateTransformMultiscalesExamples3d();

//		ex.writeImageAndTransform( "identity", ex::identity );
//		ex.writeImageAndTransform( "scale", ex::scale );
//		ex.writeImageAndTransform( "translation", ex::translation );
//		ex.writeImageAndTransform( "sequenceScaleTranslation", ex::sequenceScaleTranslation );

//		ex.writeImageAndTransform( "rotation", ex::rotation );
//		ex.writeImageAndTransform( "affine", ex::affine );

//		ex.writeImageAndTransform( "scaleParams", ex::scaleParams ); 
//		ex.writeImageAndTransform( "translationParams", ex::translationParams ); 

//		ex.writeImageAndTransform( "rotationParams", ex::rotationParams ); 
//		ex.writeImageAndTransform( "affineParams", ex::affineParams ); 

//		ex.writeImageAndTransform( "invDisplacements", ex::invDisplacements );
//		ex.writeImageAndTransform( "invCoordinates", ex::invCoordinates );

//		ex.writeImageAndTransform( "displacementsTranslation", ex::displacementsTranslation );
//		ex.writeImageAndTransform( "coordinatesTranslation", ex::coordinatesTranslation );

//		ex.writeImageAndTransform( "bijection", ex::bijectionDisplacements );

		// mapAxis and byDimension applies to array coordinates
//		ex.writeImageAndTransform( "mapAxis", ex::mapAxis, true );
//		ex.writeImageAndTransform( "byDimension", ex::byDimension3d, true );

		/*
		 * MULTISCALES
		 */
//		ex.dataset = "";
//		ex.writeImageMultiscaleAndTransform( "scale_multiscale", ex::scale );
//		ex.writeImageMultiscaleAveragedAndTransform( "affine_multiscale", ex::affine );
//		ex.writeImageMultiscaleAveragedAndTransform( "sequenceScaleTranslation_multiscale", ex::sequenceScaleTranslation );


//		ex.path = ex.parent + "/bijection.zarr";
//		ex.ap();
//		ex.dfInvTest();
//		ex.debugDfieldPfield();
//		ex.validateBijection();
	}


	String parent = "/home/john/data/ngff/transform_examples_3d";

	String path;
	String inputName;
	String outputName;
	String dataset;
	CoordinateSystem arrayCoordinateSystem;
	CoordinateSystem cs;
	CoordinateSystem arrayCs;

	final static String VERSION = "0.6";

	final static double[] defaultScale = new double[] {2, 3, 4};
	final static double[] defaultTranslation = new double[] {10, 20, 30};
	final static double[] dfieldMag = new double[] {4, 3, 2};
	final static double[] dfieldPeriod = new double[] {4, 3, 2};

	Img img;

	public CoordinateTransformMultiscalesExamples3d() {

		dataset = "array";
		// has to be reversed
		arrayCoordinateSystem = Common.makeSpace(dataset, "space", Unit.micrometer, "dim_2", "dim_1", "dim_0");

		outputName = "physical";
		cs = Common.makeSpace(outputName, "space", Unit.micrometer, "x", "y", "z");
	}

	public OmeNgffV06MultiScaleMetadata buildMultiscales(final String dset, final CoordinateTransform<?> ct) {

		OmeNgffMultiscalesBuilder builder = new OmeNgffMultiscalesBuilder().addCoordinateSystem( cs ).put( dset, ct );
		if( arrayCs != null )
			builder.addCoordinateSystem( arrayCs );

		return builder.build();
	}
	
	public OmeNgffV06MultiScaleMetadata buildMultiscales(final int N, final Function<Integer,CoordinateTransform<?>> transformForScale) {

		OmeNgffMultiscalesBuilder builder = new OmeNgffMultiscalesBuilder().addCoordinateSystem( cs );
		IntStream.range( 0, N ).forEach( s -> {
			builder.put( String.format( "s%d", s ), transformForScale.apply( s ));
		});

		if( arrayCs != null )
			builder.addCoordinateSystem( arrayCs );

		return builder.build();
	}

	public RandomAccessibleInterval<DoubleType> buildSinDfield3D(final double[] period, final double[] mag) {

		final double fx = 2 * Math.PI / period[0];
		final double fy = 2 * Math.PI / period[1];
		final double fz = 2 * Math.PI / period[2];
		final FunctionRandomAccessible<DoubleType> dxFun = new FunctionRandomAccessible<>(3, (x, dx) -> {
			dx.set(mag[0] * Math.sin(fx * x.getDoublePosition(0)));
		}, DoubleType::new);

		final FunctionRandomAccessible<DoubleType> dyFun = new FunctionRandomAccessible<>(3, (y, dy) -> {
			dy.set(mag[1] * Math.sin(fy * y.getDoublePosition(1)));
		}, DoubleType::new);
		
		final FunctionRandomAccessible<DoubleType> dzFun = new FunctionRandomAccessible<>(3, (z, dz) -> {
			dz.set(mag[2] * Math.sin(fz * z.getDoublePosition(2)));
		}, DoubleType::new);

		final RandomAccessibleInterval<DoubleType> dx = Views.interval(dxFun, img);
		final RandomAccessibleInterval<DoubleType> dy = Views.interval(dyFun, img);
		final RandomAccessibleInterval<DoubleType> dz = Views.interval(dzFun, img);
		return Views.moveAxis(Views.stack(dx, dy, dz), 3, 0);
	}

	public RandomAccessibleInterval<DoubleType> buildSinDfield3D() {

		final double periodX = img.dimension(0) / dfieldPeriod[0];
		final double periodY = img.dimension(1) / dfieldPeriod[1];
		final double periodZ = img.dimension(2) / dfieldPeriod[2];
		return buildSinDfield3D(new double[]{periodX, periodY, periodZ}, dfieldMag);
	}
	
	public RandomAccessibleInterval<DoubleType> buildSinDfield3DInverse() {

		final RandomAccessibleInterval<DoubleType> dfImg = buildSinDfield3D();
		DisplacementFieldTransform df = new DisplacementFieldTransform(dfImg);

		WrappedIterativeInvertibleRealTransform dfInv = new WrappedIterativeInvertibleRealTransform(df);
		dfInv.getOptimzer().setTolerance(0.5);
		dfInv.getOptimzer().setMaxIters(1000);
		dfInv.getOptimzer().setMaxStep(5.0);

		final RealPoint pt = new RealPoint(3);
		final RealPoint pt2 = new RealPoint(3);
		final ArrayImg<DoubleType, DoubleArray> dfInvImg = ArrayImgs.doubles(dfImg.dimensionsAsLongArray());
		CompositeIntervalView<DoubleType, ? extends GenericComposite<DoubleType>> vecImg = Views.collapse(Views.moveAxis(dfInvImg, 0, 3));

		double maxerr = -1;
		Cursor<? extends GenericComposite<DoubleType>> c = vecImg.cursor();
		while( c.hasNext()) {
			c.fwd();
			dfInv.applyInverse(pt, c);
			df.apply( pt, pt2 );
			
			double err = Util.distance( c, pt2 );
			if( err > maxerr )
				maxerr = err;

			final GenericComposite<DoubleType> vec = c.get();
			// v = pt - c. 
			// goes from c to pt.  
			// because c + v = c + (pt - c) = pt

			vec.get(0).set(pt.getDoublePosition(0) - c.getDoublePosition(0));
			vec.get(1).set(pt.getDoublePosition(1) - c.getDoublePosition(1));
			vec.get(2).set(pt.getDoublePosition(2) - c.getDoublePosition(2));
		}

		System.out.println( "inv dfield done" );
		System.out.println( "max err: " + maxerr );
		return dfInvImg;
	}
	
	public RandomAccessibleInterval<DoubleType> buildSinPfield3D(final double[] period, final double[] mag) {

		final double fx = 2 * Math.PI / period[0];
		final double fy = 2 * Math.PI / period[1];
		final double fz = 2 * Math.PI / period[2];
		final FunctionRandomAccessible<DoubleType> pxFun = new FunctionRandomAccessible<>(3, (p, px) -> {
			final double x = p.getDoublePosition(0);
			px.set(x + mag[0] * Math.sin(fx * x));
		}, DoubleType::new);

		final FunctionRandomAccessible<DoubleType> pyFun = new FunctionRandomAccessible<>(3, (p, py) -> {
			final double y = p.getDoublePosition(1);
			py.set(y + mag[1] * Math.sin(fy * y));
		}, DoubleType::new);

		final FunctionRandomAccessible<DoubleType> pzFun = new FunctionRandomAccessible<>(3, (p, pz) -> {
			final double z = p.getDoublePosition(2);
			pz.set(z + mag[2] * Math.sin(fz * z));
		}, DoubleType::new);

		final RandomAccessibleInterval<DoubleType> px = Views.interval(pxFun, img);
		final RandomAccessibleInterval<DoubleType> py = Views.interval(pyFun, img);
		final RandomAccessibleInterval<DoubleType> pz = Views.interval(pzFun, img);
		return Views.moveAxis(Views.stack(px, py, pz), 3, 0);
	}
	
	public RandomAccessibleInterval<DoubleType> buildTranslationPfield3D() {
		return buildTranslationPfield2D(defaultTranslation);
	}

	public RandomAccessibleInterval<DoubleType> buildTranslationPfield2D(final double[] t) {

		final FunctionRandomAccessible<DoubleType> pxFun = new FunctionRandomAccessible<>(3, (p, px) -> {
			px.set(p.getDoublePosition(0) + t[0]);
		}, DoubleType::new);

		final FunctionRandomAccessible<DoubleType> pyFun = new FunctionRandomAccessible<>(3, (p, py) -> {
			py.set(p.getDoublePosition(1) + t[1]);
		}, DoubleType::new);

		final FunctionRandomAccessible<DoubleType> pzFun = new FunctionRandomAccessible<>(3, (p, pz) -> {
			pz.set(p.getDoublePosition(2) + t[2]);
		}, DoubleType::new);

		final RandomAccessibleInterval<DoubleType> px = Views.interval(pxFun, img);
		final RandomAccessibleInterval<DoubleType> py = Views.interval(pyFun, img);
		final RandomAccessibleInterval<DoubleType> pz = Views.interval(pzFun, img);
		return Views.moveAxis(Views.stack(px, py, pz), 3, 0);
	}

	public RandomAccessibleInterval<DoubleType> buildSinPfield3D() {

		final double periodX = img.dimension(0) / dfieldPeriod[0];
		final double periodY = img.dimension(1) / dfieldPeriod[1];
		final double periodZ = img.dimension(2) / dfieldPeriod[2];
		return buildSinPfield3D(new double[] { periodX, periodY, periodZ }, dfieldMag);
	}

	public static RandomAccessibleInterval<DoubleType> buildConstantDfield3d(final double[] vector) {

		final FinalInterval itvl = new FinalInterval(3,4,5);
		final RandomAccessibleInterval<DoubleType> dx = 
				Views.interval( ConstantUtils.constantRandomAccessible(new DoubleType(vector[0]), 3), itvl);

		final RandomAccessibleInterval<DoubleType> dy = 
				Views.interval( ConstantUtils.constantRandomAccessible(new DoubleType(vector[1]), 3), itvl);

		final RandomAccessibleInterval<DoubleType> dz = 
				Views.interval( ConstantUtils.constantRandomAccessible(new DoubleType(vector[2]), 3), itvl);

		RandomAccessibleInterval<DoubleType> df = Views.moveAxis(Views.stack(dx, dy, dz), 3, 0);
		return df;
	}

	public RandomAccessibleInterval<DoubleType> buildConstantDfield3d() { 

		return buildConstantDfield3d(defaultTranslation);
	}

	public RandomAccessibleInterval<DoubleType> buildTranslationCoordField3D(final double[] translation) { 

		int nd = img.numDimensions();
		final IntervalView<DoubleType> px = Views.interval( 
				new FunctionRandomAccessible<DoubleType>(nd, (x,v) -> {
					v.set(x.getDoublePosition(0) + translation[0]);
				},
				DoubleType::new	),
				img);

		final IntervalView<DoubleType> py = Views.interval( 
				new FunctionRandomAccessible<DoubleType>(nd, (x,v) -> {
					v.set(x.getDoublePosition(1) + translation[1]);
				},
				DoubleType::new	),
				img);

		final IntervalView<DoubleType> pz = Views.interval( 
				new FunctionRandomAccessible<DoubleType>(nd, (x,v) -> {
					v.set(x.getDoublePosition(2) + translation[2]);
				},
				DoubleType::new	),
				img);

		final RandomAccessibleInterval<DoubleType> pf = Views.moveAxis(Views.stack(px, py, pz), 3, 0);
		return pf;
	}

	public static void write( CoordinateSystem[] css, CoordinateTransform<?>[] cts, N5Writer n5, final String dataset ) {

		n5.setAttribute(dataset, "ome/coordinateSystems", css);
		n5.setAttribute(dataset, "ome/coordinateTransformations", cts);
	}

	public static N5Writer makeWriter(final String path) {

//		return new N5Factory().gsonBuilder(new GsonBuilder()
//				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
//				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter()))
//			.openWriter("zarr3:"+path);

		final GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter());

		final FileSystemKeyValueAccess kva = new FileSystemKeyValueAccess(FileSystems.getDefault());
		final ZarrV3KeyValueWriter zarr = new ZarrV3KeyValueWriter( kva, path, gsonBuilder, false, false, "/", false );
		return zarr;
	}

	@SuppressWarnings("unchecked")
	public void writeImage2d( final N5Writer n5, final String dataset ) {
		ImagePlus imp = IJ.openImage("/home/john/tmp/mri-stack.tif");
		img = ImageJFunctions.wrap(imp);

		final int[] blkSize = new int[] { (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2) };
		N5Utils.save(img, n5, dataset, blkSize, new ZstandardCompression());
	}

	public void writeImage3dMultiscaleSampled( final N5Writer n5, final String dataset, final Function<Integer,CoordinateTransform< ?>> transformForScale ) {

		final int N = 3;
		IntStream.range( 0, N ).mapToObj( s -> String.format( "s%d", s )).forEach( s -> {
			copyScaleTo( n5, "mri-sample", dataset, s);
		});

		final OmeNgffV06MultiScaleMetadata ms = buildMultiscales(N, transformForScale);
		n5.setAttribute("", "ome/version", VERSION);
		n5.setAttribute("", "ome/multiscales", ms);
	}

	public void writeImage3dMultiscaleAveraged( final N5Writer n5, final String dataset, final Function<Integer,CoordinateTransform< ?>> transformForScale ) {

		Stream.of( "s0", "s1", "s2" ).forEach( s -> {
			copyScaleTo( n5, "mri-avg", dataset, s);
		});

		final OmeNgffV06MultiScaleMetadata ms = buildMultiscales(3, transformForScale);
		n5.setAttribute("", "ome/version", VERSION);
		n5.setAttribute("", "ome/multiscales", ms);
	}

	private void copyScaleTo(N5Writer n5Dst, final String srcDataset, final String destDataset, final String scale ) {

		try ( final N5Reader n5Src = new N5Factory().openReader( "/home/john/data/ngff/preComputedData/mri.zarr" )) {
			img = N5Utils.open( n5Src, srcDataset + "/" + scale );
			final int[] blkSize = new int[] { (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2)};
			N5Utils.save(img, n5Dst, destDataset + "/" + scale, blkSize, new ZstandardCompression());
		}
	}

	public void writeImageAndTransform(final String suffix, Supplier<CoordinateTransform<?>> transform) {

		writeImageAndTransform( suffix, transform, false );
	}

	public void writeImageMultiscaleAndTransform(final String suffix, final Function<Integer,CoordinateTransform< ?>> transformForScale) {

		path = parent + "/" + suffix + ".zarr";
		System.out.println(path);
		N5Writer n5 = makeWriter(path);

		writeImage3dMultiscaleSampled( n5, dataset, transformForScale );
	}
	
	public void writeImageMultiscaleAveragedAndTransform(final String suffix, final Function<Integer,CoordinateTransform< ?>> transformForScale) {

		path = parent + "/" + suffix + ".zarr";
		System.out.println(path);
		N5Writer n5 = makeWriter(path);

		writeImage3dMultiscaleAveraged( n5, dataset, transformForScale );
	}

	public void writeImageAndTransform(final String suffix, Supplier<CoordinateTransform<?>> transform, final boolean writeArrayCs) {

		path = parent + "/" + suffix + ".zarr";
		System.out.println(path);
		N5Writer n5 = makeWriter(path);

		writeImage2d(n5, dataset);

		if( writeArrayCs )
			arrayCs = CoordinateSystem.defaultArray( dataset, 2 ).reverseAxes();

		final OmeNgffV06MultiScaleMetadata ms = buildMultiscales(dataset, transform.get());
		n5.setAttribute("", "ome/version", VERSION);
		n5.setAttribute("", "ome/multiscales", ms);
	}

	public CoordinateTransform identity() {

		return new IdentityCoordinateTransform("transform-name", dataset, outputName);
	}	

	public TranslationCoordinateTransform translation() {

		final double[] translationParams = defaultTranslation;
		return new TranslationCoordinateTransform("transform-name", dataset, outputName, translationParams);
	}

	public TranslationCoordinateTransform translationParams() {

		final double[] translationParams = defaultTranslation;

		N5Writer n5 = makeWriter(path);
		final String dset = "translationParams";
		final int[] blkSize = new int[] { 3 };
		N5Utils.save(arrayToImg(translationParams), n5, dset, blkSize, new RawCompression());

		return new TranslationCoordinateTransform("transform-name", dataset, outputName, dset);
	}

	public ScaleCoordinateTransform scale() {

		double[] scaleParams = defaultScale;
		return new ScaleCoordinateTransform("transform-name", dataset, outputName, scaleParams);
	}
	
	public ScaleCoordinateTransform scale( int i) {

		final double f = i + 1;
		final double[] scaleParams = Arrays.stream( defaultScale ).map( x -> x * f ).toArray();
		return new ScaleCoordinateTransform("transform-name", dataset, outputName, scaleParams);
	}

	public ScaleCoordinateTransform scaleParams() {

		final double[] scaleParams = defaultScale;

		N5Writer n5 = makeWriter(path);
		final String dset = "scaleParams";
		final int[] blkSize = new int[] { 3 };
		N5Utils.save(arrayToImg(scaleParams), n5, dset, blkSize, new RawCompression());
		return new ScaleCoordinateTransform("transform-name", dataset, outputName, dset);
	}
	
	private RandomAccessibleInterval<DoubleType> arrayToImg( final double[] data ) {

		return ArrayImgs.doubles(data, data.length);
	}

	private RandomAccessibleInterval<DoubleType> rotationToImg( final double[][] data ) {

		final double[] dataFlat = TransformUtils.flatten(TransformUtils.reverseCoordinatesRotation(data));
		return ArrayImgs.doubles(dataFlat, data.length, data[0].length);
	}

	private RandomAccessibleInterval<DoubleType> affineToImg( final double[][] data ) {

		final double[] dataFlat = TransformUtils.flatten(TransformUtils.reverseCoordinates(data));
		return ArrayImgs.doubles(dataFlat, data.length, data[0].length);
	}

	private RandomAccessibleInterval<DoubleType> array2dToImg( final double[][] data ) {

		final double[] dataFlat = TransformUtils.flatten(data);
		return ArrayImgs.doubles(dataFlat, data[0].length, data.length);
	}

	public SequenceCoordinateTransform sequenceScaleTranslation() {

		final ScaleCoordinateTransform scale = new ScaleCoordinateTransform(defaultScale);
		final TranslationCoordinateTransform translation = new TranslationCoordinateTransform(defaultTranslation);
		return new SequenceCoordinateTransform("transform-name", dataset, outputName, scale, translation);
	}
	
	public SequenceCoordinateTransform sequenceScaleTranslation( int i ) {

		final double f = i + 1;
		final double[] s = Arrays.stream( defaultScale ).map( x -> x * f ).toArray();
		final double[] t = IntStream.range( 0, 3 ).mapToDouble( j -> {
			return defaultTranslation[ j ] + ( s[ j ] / 2 - 0.5 );
		} ).toArray();

		final ScaleCoordinateTransform scale = new ScaleCoordinateTransform(s);
		final TranslationCoordinateTransform translation = new TranslationCoordinateTransform(t);
		return new SequenceCoordinateTransform("transform-name", dataset, outputName, scale, translation);
	}

	public AffineCoordinateTransform affine( int i ) {
		
		final double f = i + 1;
		final double[] s = Arrays.stream( defaultScale ).map( x -> x * f ).toArray();
		final double[] t = IntStream.range( 0, 3 ).mapToDouble( j -> {
			return defaultTranslation[ j ] + ( s[ j ] / 2 - 0.5 );
		} ).toArray();
		
		double[] affineParams = new double[] {
				s[0],  0.0,  0.0, t[0], 
				 0.0, s[1],  0.0, t[1],
				 0.0,  0.0, s[2], t[2]
		};
		return new AffineCoordinateTransform("transform-name", dataset, outputName, affineParams);	
	}

	public MapAxisCoordinateTransform mapAxis() {

		// identity is [z = dim_2, y = dim_1, x = dim_0]
		Map<String,String> axisMapping = new HashMap<>();
		axisMapping.put("z", "dim_0");
		axisMapping.put("y", "dim_1");
		axisMapping.put("x", "dim_2");
		return new MapAxisCoordinateTransform("transform-name", arrayCoordinateSystem, cs, axisMapping);
	}

	public RotationCoordinateTransform rotation() {

		double[][] rotationMatrix = new double[][] {
				{0, 1, 0},
				{0, 0, 1},
				{1, 0, 0},
		};
		return new RotationCoordinateTransform("array-to=-physical", dataset, outputName, rotationMatrix);
	}

	public RotationCoordinateTransform rotationParams() {

		double[][] rotationMatrix = new double[][] {
				{0, 1, 0},
				{0, 0, 1},
				{1, 0, 0},
		};

		N5Writer n5 = makeWriter(path);
		final String dset = "rotationParams";
		final int[] blkSize = new int[] { 3, 3 };
		N5Utils.save(rotationToImg(rotationMatrix), n5, dset, blkSize, new RawCompression());

		return new RotationCoordinateTransform("array-to-physical", dataset, outputName, dset);
	}

	public AffineCoordinateTransform affine() {

		final double[] affineParams = new double[] {
				2, 0.3, 0.1, 10,
				0.4, 3, 0.8, 20,
				0.6, 0.8, 4, 30,
		};
		return new AffineCoordinateTransform("array-to-physical", dataset, outputName, affineParams);
	}

	public AffineCoordinateTransform affineParams() {

		final double[][] affineParams = new double[][] {
				{2, 0.3, 0.1, 10},
				{0.4, 3, 0.8, 20},
				{0.6, 0.8, 4, 30}
		};

		N5Writer n5 = makeWriter(path);
		final String dset = "affineParams";
		new AffineCoordinateTransform(TransformUtils.flatten(affineParams))
			.write(n5, dset);

		return new AffineCoordinateTransform("transform-name", inputName, outputName, dset);
	}

	public ByDimensionCoordinateTransform byDimension3d() {

		// identity is [z = dim_2, y = dim_1, x = dim_0]
		final CoordinateTransform[] transforms = new CoordinateTransform[] {
				new ScaleCoordinateTransform(null, new String[] {"dim_0", "dim_1"}, new String[] {"x", "y"}, new double[] {2, 3}),
				new TranslationCoordinateTransform(null, new String[] {"dim_2"}, new String[] {"z"}, new double[] {10}),
		};

		return new ByDimensionCoordinateTransform("transform-name", arrayCoordinateSystem, cs, transforms);
	}

	public CoordinateTransform displacementsTranslation() {

		final RandomAccessibleInterval<DoubleType> df = buildConstantDfield3d();
		final int[] blkSize = new int[] { 2, (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2)};

		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> dfieldTform = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, "displacementField", df, 
				blkSize, new RawCompression(), arrayCoordinateSystem, cs,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, arrayCoordinateSystem.getName(), new double[] {1,1,1})
				});
		
		DisplacementFieldCoordinateTransform<?> dfieldTformInner = new DisplacementFieldCoordinateTransform(
				"inverse-dfield", dfieldTform.getParameterPath(), dfieldTform.getInterpolation());
		
		final InverseCoordinateTransform<?,?> tform = new InverseCoordinateTransform<>("tform-name", arrayCoordinateSystem, cs, dfieldTformInner);

		return tform;
	}

	public CoordinateTransform coordinatesTranslation() {

		final RandomAccessibleInterval<DoubleType> pf = buildTranslationPfield3D();
		final String cfDataset = "coordinatesField";

		final int[] blkSize = new int[] { 3, (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2) };

		N5Writer n5 = makeWriter(path);
		CoordinateFieldCoordinateTransform<?> cFieldTform = CoordinateFieldCoordinateTransform.writeCoordinateField(
				n5, cfDataset, pf, 
				blkSize, new RawCompression(), arrayCoordinateSystem, cs,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, arrayCoordinateSystem.getName(), new double[] {1,1,1,1})
				});

		final CoordinateFieldCoordinateTransform<?> dfieldTformInner = new CoordinateFieldCoordinateTransform(cFieldTform.getParameterPath(), cFieldTform.getInterpolation());
		final InverseCoordinateTransform<?,?> tform = new InverseCoordinateTransform<>("tform-name", arrayCoordinateSystem, cs, dfieldTformInner);
		return tform;
	}
	
	public CoordinateTransform<?> invDisplacements() {

		final RandomAccessibleInterval<DoubleType> df = buildSinDfield3D();
		final int[] blkSize = new int[] { 3, (int)img.dimension(0), (int)img.dimension(1) , (int)img.dimension(2)};

		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> dfieldTform = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, "displacementField", df, 
				blkSize, new ZstandardCompression(), arrayCoordinateSystem, cs,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, arrayCoordinateSystem.getName(), new double[] {1,1,1,1})
				});
		
		DisplacementFieldCoordinateTransform<?> dfieldTformInner = new DisplacementFieldCoordinateTransform(
				"inverse-dfield", dfieldTform.getParameterPath(), dfieldTform.getInterpolation());
		
		final InverseCoordinateTransform<?,?> tform = new InverseCoordinateTransform<>("tform-name", arrayCoordinateSystem, cs, dfieldTformInner);

		return tform;
	}

	public CoordinateTransform<?> invCoordinates() {

		RandomAccessibleInterval<DoubleType> pf = buildSinPfield3D();
		final String cfDataset = "coordinatesField";

		final int[] blkSize = new int[] {3, (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2)};

		N5Writer n5 = makeWriter(path);
		CoordinateFieldCoordinateTransform<?> cFieldTform = CoordinateFieldCoordinateTransform.writeCoordinateField(
				n5, cfDataset, pf, 
				blkSize, new ZstandardCompression(), arrayCoordinateSystem, cs,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, arrayCoordinateSystem.getName(), new double[] {1,1,1,1})
				});

		final CoordinateFieldCoordinateTransform<?> dfieldTformInner = new CoordinateFieldCoordinateTransform(cFieldTform.getParameterPath(), cFieldTform.getInterpolation());
		final InverseCoordinateTransform<?,?> tform = new InverseCoordinateTransform<>("tform-name", arrayCoordinateSystem, cs, dfieldTformInner);
		return tform;
	}

	public InverseCoordinateTransform<?,?> inverseOfDisplacements() {

		RandomAccessibleInterval<DoubleType> df = buildConstantDfield3d();
		final int[] blkSize = new int[] { 3, (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2) };

		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> dfieldTform = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, "displacementField", df, 
				blkSize, new RawCompression(), cs, arrayCoordinateSystem,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, cs.getName(), new double[] {1,1,1,1})
				});

		DisplacementFieldCoordinateTransform<?> dfieldTformInner = new DisplacementFieldCoordinateTransform(
				"inverse-dfield", dfieldTform.getParameterPath(), dfieldTform.getInterpolation());

		final InverseCoordinateTransform<?,?> tform = new InverseCoordinateTransform<>("tform-name", arrayCoordinateSystem, cs, dfieldTformInner);
		return tform;
	}

	public BijectionCoordinateTransform bijectionDisplacements() {

		final String datasetForward = "displacementForward";
		final String datasetInverse = "displacementInverse";

		final int[] blkSize = new int[] { 3, (int)img.dimension(0), (int)img.dimension(1), (int)img.dimension(2) };

		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> dfieldInverse = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, datasetInverse, buildSinDfield3D(),
				blkSize, new RawCompression(), arrayCoordinateSystem, cs,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, datasetInverse, cs.getName(), new double[] {1,1,1,1})
				});

		DisplacementFieldCoordinateTransform<?> dfieldInverseInner = new DisplacementFieldCoordinateTransform(
				"inverse-dfield", dfieldInverse.getParameterPath(), dfieldInverse.getInterpolation());


		DisplacementFieldCoordinateTransform<?> dfieldForward = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, datasetForward, buildSinDfield3DInverse(),
				blkSize, new RawCompression(), cs, arrayCoordinateSystem,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, datasetForward, cs.getName(), new double[] {1,1,1,1})
				});

		DisplacementFieldCoordinateTransform<?> dfieldForwardInner = new DisplacementFieldCoordinateTransform(
				"forward-dfield", dfieldForward.getParameterPath(), dfieldForward.getInterpolation());

		final BijectionCoordinateTransform tform = new BijectionCoordinateTransform("tform-name",
				arrayCoordinateSystem, cs, 
				dfieldForwardInner, dfieldInverseInner);

		return tform;
	}

}

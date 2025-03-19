package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.exampleData;

import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.FileSystemKeyValueAccess;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.SpacesTransforms;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ByDimensionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffMultiscalesBuilder;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfigAdapter;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueReader;
import org.janelia.saalfeldlab.n5.zarr.v3.ZarrV3KeyValueWriter;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

import com.google.gson.GsonBuilder;

import bdv.util.BdvFunctions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.SubsampleIntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.RealComposite;
import net.imglib2.view.fluent.RandomAccessibleIntervalView;

public class CoordinateTransformLensCorrectionExample {

	CoordinateSystem csRaw;
	CoordinateSystem csCorrected;
	
	CoordinateSystem csRaw2d;
	CoordinateSystem csCorrected2d;
	
	Interval itvl;

	String lensCorrectionDataset = "lensCorrection";
	String multiscaleDataset;
	
	public CoordinateTransformLensCorrectionExample() {
		final String raw = "raw";
		csRaw = Common.makeSpace(raw, Axis.SPACE, Unit.nanometer, "x", "y", "z");

		final String corrected = "corrected";
		csCorrected = Common.makeSpace(corrected, Axis.SPACE, Unit.nanometer, "x", "y", "z");	

		csRaw2d = Common.makeSpace("raw2d", Axis.SPACE, Unit.nanometer, "x", "y");
		csCorrected2d = Common.makeSpace("corrected2d", Axis.SPACE, Unit.nanometer, "x", "y");	
		
		itvl = new FinalInterval(new long[] {129, 129, 33});
	}

	public static void main(String[] args) {

		CoordinateTransformLensCorrectionExample ex = new CoordinateTransformLensCorrectionExample();

		try( N5Writer n5 = makeWriter("/home/john/data/ngff/lens_correction.zarr")) {
			
//			ex.writeImg(n5);
			ex.writeTransform(n5);

//			final RandomAccessibleInterval<RealComposite< DoubleType >> dField = ex.buildDfieldComp();
//			DisplacementFieldTransform tform = new DisplacementFieldTransform(
//					Views.interpolate( dField, new NLinearInterpolatorFactory<>()));

//			transformAndShow(buildGridImg(itvl), tform);

		}

		System.out.println("done");
	}


	public void writeTransform(N5Writer n5) {

		final RandomAccessibleInterval<DoubleType> dField3d = buildDfield(itvl);
		final IntervalView<DoubleType> dfield2d = Views.hyperSlice(dField3d, 2, 0);

		final SubsampleIntervalView<DoubleType> dx = Views.subsample(
				Views.hyperSlice(dfield2d, 2, 0),	
				5, 5);

		final SubsampleIntervalView<DoubleType> dy = Views.subsample(
				Views.hyperSlice(dfield2d, 2, 1),
				5, 5);

		RandomAccessibleInterval<DoubleType> df = Views.moveAxis(Views.stack(dx, dy), 2, 0);
		System.out.println(Intervals.toString(df));

		final int[] blockSize = new int[]{2, (int)df.dimension(0), (int)df.dimension(1)};
		DisplacementFieldCoordinateTransform<?> tform = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, lensCorrectionDataset, df, 
				blockSize, new ZstandardCompression(), csRaw2d, csCorrected2d,
				new CoordinateTransform[] {
						new ScaleCoordinateTransform("displacement field sample spacing", lensCorrectionDataset, csRaw.getName(), new double[] {1, 5, 5})
				});

		DisplacementFieldCoordinateTransform subDf = new DisplacementFieldCoordinateTransform("lens correction", tform.getParameterPath(), tform.getInterpolation(),
				new String[] {"y", "x"}, new String[] {"y", "x"});

		final IdentityCoordinateTransform idZ = new IdentityCoordinateTransform("", new String[]{"z"}, new String[]{"z"});
		
		// NOTICE: here we're explicitly labeling the coordinate sytems here such that this is the "inverse" transform (from corrected to raw space)
		// rather than wrapping this in an inverseOf transform
		final ByDimensionCoordinateTransform totalTform = new ByDimensionCoordinateTransform("lens correction 3d", csCorrected, csRaw, subDf, idZ);

		SpacesTransforms st = new SpacesTransforms(
				new CoordinateSystem[]{csRaw, csCorrected},
				new CoordinateTransform[]{totalTform});

		n5.createGroup(CoordinateTransform.KEY);
		st.serialize(n5, CoordinateTransform.KEY);
	}
	
	public void writeImg(N5Writer n5) {

		multiscaleDataset = "image";
		RandomAccessibleInterval<UnsignedByteType> img = buildGridImg(itvl);	
		final int[] blkSize = Arrays.stream( itvl.dimensionsAsLongArray()).mapToInt(x -> (int)x).toArray();
		N5Utils.save(img, n5, multiscaleDataset + "/0", blkSize, new ZstandardCompression());

		final String s0Dset = "0";
		final ScaleCoordinateTransform ct = new ScaleCoordinateTransform(null, s0Dset, csRaw.getName(), new double[] {1,1,1});
		OmeNgffMultiscalesBuilder builder = new OmeNgffMultiscalesBuilder().addCoordinateSystem( csRaw );
		builder.put( s0Dset, ct );

		OmeNgffV06MultiScaleMetadata meta = builder.build();
		n5.setAttribute(multiscaleDataset, "ome/version", "0.6");
		n5.setAttribute(multiscaleDataset, "ome/multiscales", meta);
	}

	public static void transformAndShow(RandomAccessibleInterval<UnsignedByteType> img, RealTransform tform) {

		final RealRandomAccessible<UnsignedByteType> realImg = Views.interpolate(
				Views.extendZero(img),
				new NLinearInterpolatorFactory<UnsignedByteType>());

		final IntervalView<UnsignedByteType> tformedImg = Views.interval(
				Views.raster(new RealTransformRandomAccessible(realImg, tform)),
				img);

		BdvFunctions.show(tformedImg, "tformed");
	}

	public static RandomAccessibleInterval<UnsignedByteType> buildGridImg( Interval itvl) {

		RandomAccessibleIntervalView<UnsignedByteType> funra = new FunctionRandomAccessible<UnsignedByteType>(itvl.numDimensions(), (p, v) -> {
			
			final int x = p.getIntPosition(0);
			final int y = p.getIntPosition(1);
			final int z = p.getIntPosition(2);
			int val = x % 8 == 0 || y % 8 == 0 ? 1 : 0;
			v.set( (z+1) * val);

		}, UnsignedByteType::new).view().interval(itvl);
		
		return funra;
		
//		ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs.unsignedBytes(itvl.dimensionsAsLongArray());
//		LoopBuilder.setImages(funra, img)
//			.forEachPixel((x,y) -> {
//				y.set(x.get());
//			});
//
//		return img;
	}
	
	public static RandomAccessibleInterval< DoubleType > buildDfield( Interval itvl ) {

		final double[] center = itvl.maxAsDoubleArray();
		center[0] /= 2;
		center[1] /= 2;

		final double w = 400 * center[0];
		RandomAccessibleInterval<DoubleType> dx = new FunctionRandomAccessible<>(itvl.numDimensions(), (p, v) -> {
			final double x = p.getDoublePosition(0) - center[0];
			final double y = p.getDoublePosition(1) - center[1];
			final double r2 = x * x + y * y;
			final double f = (r2 / w) + 0.7;
			v.set(x - x * f);
	
		}, DoubleType::new).view().interval(itvl);

		RandomAccessibleInterval<DoubleType> dy = new FunctionRandomAccessible<>(itvl.numDimensions(), (p, v) -> {
			final double x = p.getDoublePosition(0) - center[0];
			final double y = p.getDoublePosition(1) - center[1];
			final double r2 = x * x + y * y;
			final double f = (r2 / w) + 0.7;
			v.set(y - y * f);
		}, DoubleType::new).view().interval(itvl);
		
		RandomAccessibleInterval<DoubleType> dz = new FunctionRandomAccessible<>(itvl.numDimensions(), (p, v) -> {
			v.setZero();
		}, DoubleType::new).view().interval(itvl);
		
		return Views.stack(dx, dy, dz);
	}

	public RandomAccessibleInterval<RealComposite< DoubleType >> buildDfieldComp() {

		final double[] center = itvl.maxAsDoubleArray();
		center[0] /= 2;
		center[1] /= 2;

		final double w = 400 * center[0];
		return new FunctionRandomAccessible<RealComposite< DoubleType >>(itvl.numDimensions(), (p, v) -> {

			final double x = p.getDoublePosition(0) - center[0];
			final double y = p.getDoublePosition(1) - center[1];
			final double r2 = x * x + y * y;

			final double f = (r2 / w) + 0.7;
			v.setPosition(x - x * f, 0);
			v.setPosition(y - y * f, 1);

			v.setPosition(0, 2);

		}, () -> DoubleType.createVector(3)).view().interval(itvl);
		
	}

	public static RandomAccessibleInterval<DoubleType> scaleField( Interval itvl ) {

		final double[] center = itvl.maxAsDoubleArray();
		center[0] /= 2;
		center[1] /= 2;

		final double w = 32 * center[0];
		return new FunctionRandomAccessible<DoubleType>(itvl.numDimensions(), (p, v) -> {

			final double x = p.getDoublePosition(0) - center[0];
			final double y = p.getDoublePosition(1) - center[1];
			final double r2 = x * x + y * y;
			v.set((r2 / w) + 0.7);

		}, DoubleType::new).view().interval(itvl);	

	}

	public static ZarrV3KeyValueWriter makeWriter(final String path) {

		final GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter());

		final FileSystemKeyValueAccess kva = new FileSystemKeyValueAccess(FileSystems.getDefault());
		ZarrV3KeyValueWriter n5 = new ZarrV3KeyValueWriter( kva, path, gsonBuilder, false, false, "/", false );
		return n5;
	}

	public static ZarrV3KeyValueReader makeReader(final String path) {

		final GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeHierarchyAdapter(CoordinateTransform.class, NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))
				.registerTypeAdapter(OmeNgffV06MultiScaleMetadata.class, new OmeNgffV06MultiScaleMetadataAdapter());

		final FileSystemKeyValueAccess kva = new FileSystemKeyValueAccess(FileSystems.getDefault());
		ZarrV3KeyValueReader n5 = new ZarrV3KeyValueReader( kva, path, gsonBuilder, false, false, false );
		return n5;
	}

}

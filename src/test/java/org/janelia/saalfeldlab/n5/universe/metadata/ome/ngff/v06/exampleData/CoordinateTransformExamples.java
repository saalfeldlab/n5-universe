package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.exampleData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.GsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.SpacesTransforms;
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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.RotationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfigAdapter;
import org.janelia.saalfeldlab.n5.universe.serialization.ReflectionUtils;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.position.PositionRandomAccessible;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ConstantUtils;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class CoordinateTransformExamples {

	public static void main(String[] args) {
		
		
		final CoordinateTransformExamples ex = new CoordinateTransformExamples();

//		identity(dest);
//		mapAxis(dest);
//		translation(dest);
//		scale(dest);
//		rotation(dest);
//		affine(dest);
//		byDimension(dest);
//		displacements(dest);
//		coordinates(dest);

//		inverseOfDisplacements(dest);
//		ex.bijectionDisplacements();
//		ex.writeImageAndTransform( "identity", ex::identity );

//		ex.writeImageAndTransform( "bijection_", ex::bijectionDisplacements );

//		nameConfigTest();
		System.out.println( "ack" );
	}
	
	
	String parent = "/home/john/data/ngff/transform_examples";

	String path;
	String inputName;
	String outputName;
	String dataset;
	CoordinateSystem[] css;
	
	public CoordinateTransformExamples() {

		inputName = "in";
		outputName = "physical";
		dataset = "array";
		css = makeCoordinateSystems(dataset, outputName, new String[]{"x", "y", "z"});
	}

	public static void nameConfigTest() { 
		
		GsonBuilder builder = new GsonBuilder().registerTypeHierarchyAdapter(CoordinateTransform.class,
				NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class));

		Gson gson = builder.create();

		final String inputName = "in";
		final String outputName = "out";
		final CoordinateSystem[] css = makeCoordinateSystems(inputName, outputName, new String[]{"x", "y", "z"});


//		double[] params = new double[] { 10, 20, 30 };
//		CoordinateTransform tform = new TranslationCoordinateTransform("transform-name", inputName, outputName, params);
//		CoordinateTransform tform = new ScaleCoordinateTransform("transform-name", inputName, outputName, params);
//		CoordinateTransform tform = new ScaleCoordinateTransform(params);
//		CoordinateTransform tform = new IdentityCoordinateTransform("transform-name", inputName, outputName);
//		CoordinateTransform tform = new IdentityCoordinateTransform("transform-name", inputName, outputName);

//		double[] params = new double[] { 
//				7, 5, 3, 10,
//				2, 8, 6, 11,
//				1, 4, 9, 12 };
//		CoordinateTransform tform = new AffineCoordinateTransform("transform-name", inputName, outputName, params);

//		double[][] params = new double[][]{
//				{0, 1, 0},
//				{0, 0, 1},
//				{1, 0, 0}};
//		CoordinateTransform tform = new RotationCoordinateTransform("transform-name", inputName, outputName, params);
		

		Map<String,String> axisMapping = new HashMap<>();
		axisMapping.put("x", "y");
		axisMapping.put("y", "z");
		axisMapping.put("z", "x");
		CoordinateTransform tform = new MapAxisCoordinateTransform("transform-name", css[0], css[1], axisMapping);

//		CoordinateTransform[] params = new CoordinateTransform[] {
//				new ScaleCoordinateTransform(new double[] { 2, 3, 4 }),
//				new TranslationCoordinateTransform(new double[] { 12, 13, 14 })
//		};
//		CoordinateTransform tform = new SequenceCoordinateTransform("transform-name", inputName, outputName, params);
		
//		CoordinateTransform tform = new InverseCoordinateTransform<>("transform name", new ScaleCoordinateTransform(new double[] { 2, 3, 4 }));

//		CoordinateTransform tform = new BijectionCoordinateTransform("transform name", inputName, outputName,
//				new ScaleCoordinateTransform(new double[] { 1, 1, 1 }),
//				new ScaleCoordinateTransform(new double[] { 1, 1, 1 }));

//		double[] params = new double[] {
//				7, 5, 3, 10,
//				2, 8, 6, 11,
//				1, 4, 9, 12 };
//		CoordinateTransform tform = new AffineCoordinateTransform("transform-name", "array-path", inputName, outputName);
//

//		RandomAccessibleInterval<DoubleType> df = buildConstantDfield2d();
//		final String dataset = "df";
//
//		N5Writer n5 = new N5Factory().openWriter("/home/john/data/ngff/transform_examples/dfield.zarr");
//		DisplacementFieldCoordinateTransform<?> tform = DisplacementFieldCoordinateTransform.writeDisplacementField(n5, dataset, df, 
//				new int[] {2,3,4}, new RawCompression(), 
//				Common.makeSpace("in", "space", "micrometer", "x", "y"),
//				Common.makeSpace("out", "space", "micrometer", "x", "y"),
//				new CoordinateTransform[] { new ScaleCoordinateTransform(null, dataset, "in", new double[] {1,1,1}) });

		JsonElement json = gson.toJsonTree(tform);
		System.out.println(json);

		CoordinateTransform dtform = gson.fromJson(json, CoordinateTransform.class);
		dtform.setInput(css[0]);
		dtform.setOutput(css[1]);
		System.out.println(dtform);

		RealTransform tf = dtform.getTransform();
		System.out.println(tf);

		double[] p = new double[] {1,2,3};
		double[] q = new double[3];
		tf.apply(p, q);
		System.out.println(Arrays.toString(p) + " -> " + Arrays.toString(q));

//		N5Writer n5 = new N5Factory()
//				.gsonBuilder(builder)
//				.openWriter("/home/john/data/ngff/transform_examples/dfield.zarr");
//
//		JsonElement json = n5.getAttribute("", "ome/coordinateTransformations[0]", JsonElement.class);
//		System.out.println(json);

//		CoordinateTransform tform = n5.getAttribute("df", "ome/coordinateTransformations[0]", CoordinateTransform.class);
//		System.out.println(tform);

//		JsonElement jj = n5.getAttribute("df", "ome/" + CoordinateTransform.KEY, JsonElement.class);
//		System.out.println(jj);

		
//		N5Writer n5 = new N5Factory()
//				.gsonBuilder(builder)
//				.openWriter("/home/john/data/ngff/transform_examples/cfield.zarr");
//
//		JsonElement json = n5.getAttribute("", "ome/coordinateTransformations[0]", JsonElement.class);
//		System.out.println(json);
//
//		CoordinateTransform[] tforms = n5.getAttribute("", "ome/" + CoordinateTransform.KEY, CoordinateTransform[].class);
//		CoordinateTransform tform = tforms[0];
//		System.out.println(tform);
//
//		RealTransform tf = tform.getTransform(n5);
//		System.out.println(tf);
//
//		double[] zero = new double[] {0,0,0};
//		double[] result = new double[] {0,0,0};
//		tf.apply(zero, result);
//		System.out.println(Arrays.toString(result));



	}
	
	public static RandomAccessibleInterval<DoubleType> buildConstantDfield2d() { 

		final FinalInterval itvl = new FinalInterval(3,4);
		final RandomAccessibleInterval<DoubleType> dx = 
				Views.interval( ConstantUtils.constantRandomAccessible(new DoubleType(1), 2), itvl);

		final RandomAccessibleInterval<DoubleType> dy = 
				Views.interval( ConstantUtils.constantRandomAccessible(new DoubleType(1), 2), itvl);

		RandomAccessibleInterval<DoubleType> df = Views.moveAxis(Views.stack(dx, dy), 2, 0);
		return df;
	}
	
	public static RandomAccessibleInterval<DoubleType> buildConstantDfield3d( double[] vector ) { 

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
	
	public static RandomAccessibleInterval<DoubleType> buildConstantDfield3d() { 

		return buildConstantDfield3d(new double[] { 1, 2, 3 });
	}
	
	public static RandomAccessibleInterval<DoubleType> buildTranslationCoordField3D() { 

		final FinalInterval itvl = new FinalInterval(3,4,5);

		final IntervalView<DoubleType> px = Views.interval( 
				new FunctionRandomAccessible<DoubleType>(3, (x,v) -> {
					v.set(x.getDoublePosition(0) + 1);
				},
				DoubleType::new	),
				itvl);

		final IntervalView<DoubleType> py = Views.interval( 
				new FunctionRandomAccessible<DoubleType>(3, (x,v) -> {
					v.set(x.getDoublePosition(1) + 2);
				},
				DoubleType::new	),
				itvl);

		final IntervalView<DoubleType> pz = Views.interval( 
				new FunctionRandomAccessible<DoubleType>(3, (x,v) -> {
					v.set(x.getDoublePosition(2) + 3);
				},
				DoubleType::new	),
				itvl);

		RandomAccessibleInterval<DoubleType> pf = Views.moveAxis(Views.stack(px, py, pz), 3, 0);
		return pf;
	}

	public static void write( CoordinateSystem[] css, CoordinateTransform[] cts, N5Writer n5, final String dataset ) {

		new SpacesTransforms(css, cts).serialize(n5, dataset);
	}

	public static N5Writer makeWriter(final String path) {

		return new N5Factory().gsonBuilder(new GsonBuilder().registerTypeHierarchyAdapter(CoordinateTransform.class,
				NameConfigAdapter.getJsonAdapter("type", CoordinateTransform.class))).openWriter(path);
	}

	public static CoordinateSystem[] makeCoordinateSystems(String inputName, String outputName, String[] axisNames) {
		return new CoordinateSystem[] { Common.makeSpace(inputName, "space", Unit.micrometer, "x", "y", "z"),
				Common.makeSpace(outputName, "space", Unit.micrometer, "x", "y", "z") };
	}

	@SuppressWarnings("unchecked")
	public static void writeImage2d( final N5Writer n5, final String dataset ) {
		ImagePlus imp = IJ.openImage("/home/john/tmp/boats.tif");
		final Img img = ImageJFunctions.wrap(imp);

		final int[] blkSize = new int[] { (int)img.dimension(0), (int)img.dimension(1) };
		N5Utils.save(img, n5, dataset, blkSize, new ZstandardCompression());
	}
	
	public void writeImageAndTransform(final String suffix, Runnable writeTransform) {

		path = parent + "/" + suffix + ".zarr";
		System.out.println(path);
		N5Writer n5 = makeWriter(path);

		writeImage2d(n5, dataset);
		writeTransform.run();

	}

	public void identity() {

		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new IdentityCoordinateTransform("transform-name", inputName, outputName) };

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}

	
	public void translation() {

		double[] translationParams = new double[] { 10, 20, 30 };
		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new TranslationCoordinateTransform("transform-name", inputName, outputName, translationParams)};

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}


	public void scale() {

		double[] scaleParams = new double[] { 10, 20, 30 };
		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new ScaleCoordinateTransform("transform-name", inputName, outputName, scaleParams)};

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}

	
	public void mapAxis() {


		Map<String,String> axisMapping = new HashMap<>();
		axisMapping.put("x", "y");
		axisMapping.put("y", "z");
		axisMapping.put("z", "x");
		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new MapAxisCoordinateTransform("transform-name", css[0], css[1], axisMapping)};

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}

	
	public void rotation() {

		double[][] rotationMatrix = new double[][] {
				{0,1,0},
				{0,0,1},
				{1,0,0}
		};
		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new RotationCoordinateTransform("transform-name", inputName, outputName, rotationMatrix)};

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}


	public void affine() {

		double[] affineParams = new double[] {
				2,0,0,10,
				0,3,0,20,
				0,0,4,30
		};
		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new AffineCoordinateTransform("transform-name", inputName, outputName, affineParams)};

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}

	
	public void byDimension() {

		final CoordinateTransform[] transforms = new CoordinateTransform[] {
				new ScaleCoordinateTransform(null, new String[] {"x", "y"}, new String[] {"x", "y"}, new double[] {2,3}),
				new TranslationCoordinateTransform(null, new String[] {"z"}, new String[] {"z"}, new double[] {-10}),
		};

		final CoordinateTransform[] cts = new CoordinateTransform[] {
				new ByDimensionCoordinateTransform("transform-name", css[0], css[1], transforms)
		};

		System.out.println(path);
		N5Writer n5 = makeWriter(path);
		write(css, cts, n5, dataset);
	}

	
	public void displacements() {

		RandomAccessibleInterval<DoubleType> df = buildConstantDfield3d();

		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> tform = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, dataset, df, 
				new int[] {3,3,4,5}, new RawCompression(), css[0], css[1],
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, css[0].getName(), new double[] {1,1,1,1})
				});

		final CoordinateTransform<?>[] cts = new CoordinateTransform[] {tform};
		System.out.println(path);
		write(css, cts, n5, dataset);
	}

	
	public void inverseOfDisplacements() {

		RandomAccessibleInterval<DoubleType> df = buildConstantDfield3d();
		final String dataset = "df";

		final String path = parent + "/inverseOf_dfield.zarr";
		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> dfieldTform = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, dataset, df, 
				new int[] {3,3,4,5}, new RawCompression(), css[1], css[0],
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, css[1].getName(), new double[] {1,1,1,1})
				});

		DisplacementFieldCoordinateTransform<?> dfieldTformInner = new DisplacementFieldCoordinateTransform(
				"inverse-dfield", dfieldTform.getParameterPath(), dfieldTform.getInterpolation());

		final InverseCoordinateTransform<?,?> tform = new InverseCoordinateTransform<>("tform-name", css[0], css[1], dfieldTformInner);
		final CoordinateTransform<?>[] cts = new CoordinateTransform[] {tform};
		System.out.println(path);
		write(css, cts, n5, dataset);
	}


	public void bijectionDisplacements() {

		final String datasetForward = "displacementForward";
		final String datasetInverse = "displacementInverse";

		N5Writer n5 = makeWriter(path);
		DisplacementFieldCoordinateTransform<?> dfieldForward = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, datasetForward, buildConstantDfield3d( new double[] {1,2,3}),
				new int[] {3,3,4,5}, new RawCompression(), css[0], css[1],
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, datasetForward, css[1].getName(), new double[] {1,1,1,1})
				});

		DisplacementFieldCoordinateTransform<?> dfieldForwardInner = new DisplacementFieldCoordinateTransform(
				"fwd-dfield", dfieldForward.getParameterPath(), dfieldForward.getInterpolation());

		DisplacementFieldCoordinateTransform<?> dfieldInverse = DisplacementFieldCoordinateTransform.writeDisplacementField(
				n5, datasetInverse, buildConstantDfield3d( new double[] {-1,-2,-3}),
				new int[] {3,3,4,5}, new RawCompression(), css[1], css[0],
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, datasetInverse, css[1].getName(), new double[] {1,1,1,1})
				});

		DisplacementFieldCoordinateTransform<?> dfieldInverseInner = new DisplacementFieldCoordinateTransform(
				"inverse-dfield", dfieldInverse.getParameterPath(), dfieldInverse.getInterpolation());

		final BijectionCoordinateTransform tform = new BijectionCoordinateTransform("tform-name",
				css[0], css[1], 
				dfieldForwardInner, dfieldInverseInner);

		final CoordinateTransform<?>[] cts = new CoordinateTransform[] {tform};
		System.out.println(path);

		write(css, cts, n5, dataset);
	}

	
	public void coordinates() {

		RandomAccessibleInterval<DoubleType> pf = buildTranslationCoordField3D();
		final String cfDataset = "cf";

		N5Writer n5 = makeWriter(path);
		CoordinateFieldCoordinateTransform<?> tform = CoordinateFieldCoordinateTransform.writeCoordinateField(
				n5, cfDataset, pf, 
				new int[] {3,3,4,5}, new RawCompression(), css[0], css[1],
				new CoordinateTransform[] {
						new ScaleCoordinateTransform(null, dataset, css[0].getName(), new double[] {1,1,1,1})
				});

		final CoordinateTransform<?>[] cts = new CoordinateTransform[] {tform};

		System.out.println(path);
		write(css, cts, n5, dataset);
	}

}

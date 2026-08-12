package org.janelia.saalfeldlab.n5.universe.metadata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.RotationCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.AffineSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.ScaleOffsetSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.ScaleSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SequenceSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.TranslationSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.AbstractScale;
import net.imglib2.realtransform.AbstractTranslation;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;


public class TransformTests {

	private File containerDir;

	private N5Writer n5;

	private ArrayImg<UnsignedByteType, ByteArray> img;

	private final double[] translation = new double[] { 100, 200, 300 };

	private final double[] scale = new double[] { 1.1, 2.2, 3.3 };

	private final double[] so = new double[] { 1.9, 0.1, 2.8, 0.2, 3.7, 0.3 };
	private final double[] soScale = new double[] { 1.9, 2.8, 3.7 };
	private final double[] soOffset = new double[] { 0.1, 0.2, 0.3 };

	private final double[] affine = new double[] {
			1.1, 0.1, 0.2, -10,
			0.3, 2.2, 0.4, -20,
			0.5, 0.6, 3.3, -30 };

	@Before
	public void before()
	{
		final URL configUrl = TransformTests.class.getResource( "/n5.jq" );
		final File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "transforms.n5" );

		try {
			n5 = new N5FSWriter( containerDir.getCanonicalPath(), JqUtils.gsonBuilder(null) );

			int v = 0;
			img = ArrayImgs.unsignedBytes( 3, 4, 5);
			final ArrayCursor<UnsignedByteType> c = img.cursor();
			while( c.hasNext())
				c.next().set( v++ );

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after()
	{
		try {
			n5.remove();
		} catch (final N5Exception e) { }
	}

	@Test
	public void testAxisSelectionInAffineComposition() {

		final double theta = Math.PI / 2; // 90 degrees; chosen so a wrong axis
										   // selection would yield a determinant
										   // of exactly zero (deterministic failure)
		final double cos = Math.cos(theta);
		final double sin = Math.sin(theta);

		// 4x5 flat (row-major) matrix over axes [nonSpatial, z, y, x]:
		// axis 0 (nonSpatial) and axis 1 (z) are identity; the rotation is
		// confined to axes 2,3 (y,x).
		final double[] flat4x5 = new double[] {
				1, 0, 0,    0,   0,
				0, 1, 0,    0,   0,
				0, 0, cos, -sin, 0,
				0, 0, sin,  cos, 0
		};
		final RotationCoordinateTransform rotation = new RotationCoordinateTransform(flat4x5);

		final Axis[] axes = new Axis[] {
				new Axis(Axis.CHANNEL, "nonSpatial"),
				new Axis(Axis.SPACE, "z"),
				new Axis(Axis.SPACE, "y"),
				new Axis(Axis.SPACE, "x"),
		};

		// the correct 3D reduction: drop axis 0 (nonSpatial), keep axes 1,2,3 (z,y,x) in order
		final AffineTransform3D expected = new AffineTransform3D();
		expected.set(
				1, 0,   0,   0,
				0, cos, -sin, 0,
				0, sin,  cos, 0);

		final List<CoordinateTransform<?>> transforms = new ArrayList<>();
		transforms.add(rotation);
		final AffineTransform3D actual = Common.toAffine3D(null, transforms, axes);

		assertArrayEquals(
				"Common.toAffine3D should select the true spatial axes (1,2,3), not naively take the first 3 (0,1,2)",
				expected.getRowPackedCopy(), actual.getRowPackedCopy(), 1e-9);
	}

	@Test
	public void testSequenceReductionWithFewerThan3Axes() {

		// A 2D dataset (axes [y, x], length 2) whose transform is a rotation
		// wrapped in a sequence -- e.g. a "rotation around center". Composing
		// this via the axis-aware Common.toAffine3D used to throw
		// ArrayIndexOutOfBoundsException: the sequence branch re-projected its
		// already-reduced 3D result through spatialTransform3D(.., axes),
		// indexing axes[2] on the 3D result even though axes has only 2 entries.
		final double theta = Math.PI / 2; // 90 degrees
		final double cos = Math.cos(theta);
		final double sin = Math.sin(theta);

		// 2x3 flat (row-major) 2D rotation over axes [y, x]
		final double[] flat2x3 = new double[] {
				cos, -sin, 0,
				sin,  cos, 0
		};
		final RotationCoordinateTransform rotation = new RotationCoordinateTransform(flat2x3);
		final SequenceCoordinateTransform sequence = new SequenceCoordinateTransform(
				new CoordinateTransform<?>[] { rotation });

		final Axis[] axes = new Axis[] {
				new Axis(Axis.SPACE, "y"),
				new Axis(Axis.SPACE, "x"),
		};

		// the 2D rotation embedded in 3D: the y,x block, third dimension identity
		final AffineTransform3D expected = new AffineTransform3D();
		expected.set(
				cos, -sin, 0, 0,
				sin,  cos, 0, 0,
				0,    0,   1, 0);

		final List<CoordinateTransform<?>> transforms = new ArrayList<>();
		transforms.add(sequence);
		final AffineTransform3D actual = Common.toAffine3D(null, transforms, axes);

		assertArrayEquals(
				"Common.toAffine3D should compose a sequence over fewer than 3 axes without re-projecting its 3D result",
				expected.getRowPackedCopy(), actual.getRowPackedCopy(), 1e-9);
	}

	@Test
	public void testScaleTranslation()
	{
		final double[] scale = new double[] { 2.0, 3.0 };
		final double[] translation = new double[] { 10.0, 100.0 };

		assertNull(MetadataUtils.scaleTranslationTransforms(null, null));

		final AffineGet st = MetadataUtils.scaleTranslationTransforms(scale, translation);
		assertTrue(st instanceof ScaleAndTranslation);

		final AffineGet s = MetadataUtils.scaleTranslationTransforms(scale, null);
		assertTrue(s instanceof AbstractScale);

		final AffineGet t = MetadataUtils.scaleTranslationTransforms(null, translation);
		assertTrue(t instanceof AbstractTranslation);
	}

	@Test
	public void testParametrizedTransforms() throws IOException {

		// translation
		final ArrayImg<DoubleType, DoubleArray> translationParams = ArrayImgs.doubles( translation, 3 );
		N5Utils.save( translationParams, n5, "translation", new int[]{3}, new GzipCompression());

		// scale
		final ArrayImg<DoubleType, DoubleArray> scaleParams = ArrayImgs.doubles( scale, 3 );
		N5Utils.save( scaleParams, n5, "scale", new int[]{3}, new GzipCompression());

		// scale-offset
		final ArrayImg<DoubleType, DoubleArray> scaleOffsetParams = ArrayImgs.doubles( so, 2, 3 );
		N5Utils.save( scaleOffsetParams, n5, "scale_offset", new int[]{3,3}, new GzipCompression());

		// affine
		final ArrayImg<DoubleType, DoubleArray> affineParams = ArrayImgs.doubles( affine, 12 );
		N5Utils.save( affineParams, n5, "affine", new int[]{12}, new GzipCompression());

		final ScaleSpatialTransform scaleTransform = new ScaleSpatialTransform( "scale" );
		final TranslationSpatialTransform translationTransform = new TranslationSpatialTransform( "translation" );
		final AffineSpatialTransform affineTransform = new AffineSpatialTransform( "affine" );
		final ScaleOffsetSpatialTransform scaleOffsetTransform = new ScaleOffsetSpatialTransform( "scale_offset" );
		final SequenceSpatialTransform seq = new SequenceSpatialTransform(
				new SpatialTransform[]{ affineTransform, scaleTransform, scaleOffsetTransform, translationTransform });

		// make an image
		N5Utils.save( img, n5, "imgParam", new int[] {5, 5, 5}, new GzipCompression());

		// set the transform metadata
		final SpatialMetadataCanonical transform = new SpatialMetadataCanonical(null, seq, "pixel", null);
		n5.setAttribute("imgParam", "spatialTransform", transform);

		testParsedTransformSeq("/imgParam");
	}

	@Test
	public void testTransforms() throws IOException {

		final ScaleSpatialTransform scaleTransform = new ScaleSpatialTransform( scale );
		final TranslationSpatialTransform translationTransform = new TranslationSpatialTransform( translation );
		final ScaleOffsetSpatialTransform scaleOffsetTransform = new ScaleOffsetSpatialTransform(soScale, soOffset);
		final AffineSpatialTransform affineTransform = new AffineSpatialTransform( affine );
		final SequenceSpatialTransform seq = new SequenceSpatialTransform(
				new SpatialTransform[]{ affineTransform, scaleTransform, scaleOffsetTransform, translationTransform });

		// make an image
		N5Utils.save( img, n5, "img", new int[] {5, 5, 5}, new GzipCompression());

		// set the transform metadata
		final SpatialMetadataCanonical transform = new SpatialMetadataCanonical(null, seq, "pixel", null);
		n5.setAttribute("img", "spatialTransform", transform);

		testParsedTransformSeq("img");
	}

	private void testParsedTransformSeq( final String dataset )
	{
		// canonical parser
		final CanonicalMetadataParser parser = new CanonicalMetadataParser();
		final Optional<CanonicalMetadata> metaOpt = parser.parseMetadata(n5, dataset);
		final CanonicalMetadata meta = metaOpt.get();
		Assert.assertTrue("meta parsed as CanonicalSpatialDatasetMetadata", meta instanceof CanonicalSpatialDatasetMetadata );
		final SpatialMetadataCanonical parsedXfm = ((CanonicalSpatialDatasetMetadata)meta).getSpatialTransform();

		Assert.assertTrue("parsed as sequence", parsedXfm.transform() instanceof SequenceSpatialTransform);
		final SequenceSpatialTransform parsedSeq = (SequenceSpatialTransform)parsedXfm.transform();
		final SpatialTransform transform0 = parsedSeq.getTransformations()[0];
		final SpatialTransform transform1 = parsedSeq.getTransformations()[1];
		final SpatialTransform transform2 = parsedSeq.getTransformations()[2];
		final SpatialTransform transform3 = parsedSeq.getTransformations()[3];

		Assert.assertTrue("transform0 is affine", transform0 instanceof AffineSpatialTransform);
		Assert.assertArrayEquals( "parsed affine parameters", affine, ((AffineSpatialTransform)transform0).affine, 1e-9 );

		Assert.assertTrue("transform1 is scale", transform1 instanceof ScaleSpatialTransform);
		Assert.assertArrayEquals( "parsed scale parameters", scale, ((ScaleSpatialTransform)transform1).scale, 1e-9 );

		Assert.assertTrue("transform2 is scale", transform2 instanceof ScaleOffsetSpatialTransform);
		Assert.assertArrayEquals( "parsed scaleOffset scale parameters", soScale, ((ScaleOffsetSpatialTransform)transform2).scale, 1e-9 );
		Assert.assertArrayEquals( "parsed scaleOffset offset parameters", soOffset, ((ScaleOffsetSpatialTransform)transform2).offset, 1e-9 );

		Assert.assertTrue("transform3 is translation", transform3 instanceof TranslationSpatialTransform);
		Assert.assertArrayEquals( "parsed translation parameters", translation, ((TranslationSpatialTransform)transform3).translation, 1e-9 );
	}

}

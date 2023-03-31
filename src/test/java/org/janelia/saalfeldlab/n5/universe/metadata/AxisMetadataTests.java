package org.janelia.saalfeldlab.n5.universe.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.AffineSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;


public class AxisMetadataTests {

	private Axis xAxis, yAxis, zAxis, cAxis, tAxis;

	private long[] dims3d, dims4d, dims5d;

	private int[] blkSz3d, blkSz4d, blkSz5d;

	private File n5rootF;
	private N5FSReader n5;

	private File containerDir;
	private N5FSWriter n5w;

	@Before
	public void before() {

		final String n5Root = "src/test/resources/canonical.n5";
		n5rootF = new File(n5Root);
		
		URL configUrl = TransformTests.class.getResource( "/n5.jq" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "canonical.n5" );

		try {

			n5 = new N5FSReader( n5rootF.getCanonicalPath(), JqUtils.gsonBuilder(null));
			n5w = new N5FSWriter( containerDir.getCanonicalPath(), JqUtils.gsonBuilder(null));

		}catch( IOException e ) {
			e.printStackTrace();
		}

		xAxis = new Axis("space", "x", "nm");
		yAxis = new Axis("space", "y", "nm");
		zAxis = new Axis("space", "z", "nm");
		cAxis = new Axis("channel", "c", "null");
		tAxis = new Axis("time", "t", "ms");

		dims3d = new long[] { 1, 1, 24 };
		dims4d = new long[] { 1, 1, 4, 6 };
		dims5d = new long[] { 1, 1, 2, 3, 4 };

		blkSz3d = new int[] { 8, 8, 8 };
		blkSz4d = new int[] { 8, 8, 8, 8 };
		blkSz5d = new int[] { 8, 8, 8, 8, 8 };
	}

	@Test
	public void parseTest() throws IOException {
		final double eps = 1e-9;

		final CanonicalMetadataParser parser = new CanonicalMetadataParser();
		Assert.assertTrue("affine dataset exists", n5.exists("affine"));

		Optional<CanonicalMetadata> metaOpt = parser.parseMetadata(n5, "affine");
		Assert.assertTrue("canonical metadata exists", metaOpt.isPresent() );
		
		final CanonicalMetadata metaRaw = metaOpt.get();
		Assert.assertTrue("is CanonicalSpatialDatasetMetadata ", (metaRaw instanceof CanonicalSpatialDatasetMetadata ));
		CanonicalSpatialDatasetMetadata sdMeta = (CanonicalSpatialDatasetMetadata)metaRaw;

		// test intensity
		Assert.assertEquals("min intensity",  12.0, sdMeta.minIntensity(), eps );
		Assert.assertEquals("max intensity", 357.0, sdMeta.maxIntensity(), eps );

		final double[] expectedAffineData = new double[] {
				2,0,0,0, 0,3,0,0, 0,0,4,0 };
		final double[] affineData = new double[12];
		sdMeta.getSpatialTransform().spatialTransform3d().toArray(affineData);

		Assert.assertArrayEquals("affineData", expectedAffineData, affineData, eps);

		Optional<CanonicalMetadata> msMetaOpt = parser.parseMetadata(n5, "multiscaleAffine");
		Assert.assertTrue("canonical ms metadata exists", msMetaOpt.isPresent() );
		
		System.out.println( msMetaOpt );
	}
	
	@Test
	public void readWriteTest() throws IOException {

		final Gson gson = JqUtils.buildGson(n5w);

		CanonicalMetadataParser parser = new CanonicalMetadataParser();
		Optional<CanonicalMetadata> meta = parser.parseMetadata(n5, "axes/xyz");
		System.out.println( meta.get() );
	}

	private CanonicalSpatialDatasetMetadata makeMeta(double[] affine, DatasetAttributes attrs, Axis[] axes) {
		return new CanonicalSpatialDatasetMetadata("",
				new SpatialMetadataCanonical("", new AffineSpatialTransform(affine), "mm", axes), 
				attrs);
	}

}

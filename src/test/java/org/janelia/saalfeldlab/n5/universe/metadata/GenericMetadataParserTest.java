package org.janelia.saalfeldlab.n5.universe.metadata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GenericMetadataParserTest {

	private File containerDir;

	private N5Writer n5;

	private double[] r;

	private double[] o;

	private double[] one;

	private double[] zero;

	@Before
	public void before()
	{
		URL configUrl = TransformTests.class.getResource( "/n5.jq" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "genericMeta.n5" );

		one = new double[]{1,1,1};
		zero = new double[]{0,0,0};
		r = new double[]{2,3,4};
		o = new double[]{9,8,7} ;

		try {
			n5 = new N5FSWriter( containerDir.getCanonicalPath() );

			n5.createDataset("r", new long[] { 2, 3, 4 }, new int[] { 2, 3, 4 }, DataType.UINT8, new RawCompression());
			n5.setAttribute("r", "r", r);

			n5.createDataset("ro", new long[] { 2, 3, 4 }, new int[] { 2, 3, 4 }, DataType.UINT8, new RawCompression());
			n5.setAttribute("ro", "r", r);
			n5.setAttribute("ro", "o", o);

			n5.createDataset("weird", new long[] { 2, 3, 4 }, new int[] { 2, 3, 4 }, DataType.UINT8, new RawCompression());
			n5.setAttribute("weird", "weird", r);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after()
	{
		try {
			n5.remove();
		} catch (IOException e) { }
	}

	@Test
	public void testDefault()
	{
		N5GenericSingleScaleMetadataParser pDefault = N5GenericSingleScaleMetadataParser.builder().build();

		Optional<N5SingleScaleMetadata> wo = pDefault.parseMetadata(n5, "weird");
		assertTrue( wo.isPresent() );
		assertArrayEquals( "def r", one, wo.get().getPixelResolution(), 1e-9 );
		assertArrayEquals( "def o", zero, wo.get().getOffset(), 1e-9 );

		Optional<N5SingleScaleMetadata> ro = pDefault.parseMetadata(n5, "ro");
		assertTrue( ro.isPresent() );
		assertArrayEquals( "def r", one, ro.get().getPixelResolution(), 1e-9 );
		assertArrayEquals( "def o", zero, ro.get().getOffset(), 1e-9 );
	}

	@Test
	public void testCustomKey()
	{
		N5GenericSingleScaleMetadataParser pR = N5GenericSingleScaleMetadataParser.builder()
				.resolution("r").build();

		Optional<N5SingleScaleMetadata> rR = pR.parseMetadata(n5, "r");
		assertTrue( rR.isPresent() );
		assertArrayEquals( "rR r", r, rR.get().getPixelResolution(), 1e-9 );

		N5GenericSingleScaleMetadataParser pRO = N5GenericSingleScaleMetadataParser.builder()
				.resolution("r").offset("o").build();

		Optional<N5SingleScaleMetadata> rRO = pRO.parseMetadata(n5, "r");
		assertTrue( rRO.isPresent() );
		assertArrayEquals( "rRO r", r, rRO.get().getPixelResolution(), 1e-9 );
		assertArrayEquals( "rRO o", zero, rRO.get().getOffset(), 1e-9 );

		Optional<N5SingleScaleMetadata> roRO = pRO.parseMetadata(n5, "ro");
		assertTrue( rRO.isPresent() );
		assertArrayEquals( "roRO r", r, roRO.get().getPixelResolution(), 1e-9 );
		assertArrayEquals( "roRO o", o, roRO.get().getOffset(), 1e-9 );
	}

	@Test
	public void testStrict()
	{
		N5GenericSingleScaleMetadataParser p = N5GenericSingleScaleMetadataParser.builder()
				.resolution("r").resolutionStrict().build();

		Optional<N5SingleScaleMetadata> roRO = p.parseMetadata(n5, "ro");
		assertTrue( roRO.isPresent() );
		assertArrayEquals( "roRO r", r, roRO.get().getPixelResolution(), 1e-9 );

		// ensure the metadata are null when resolution is set to strict
		Optional<N5SingleScaleMetadata> wRO = p.parseMetadata(n5, "weird");
		assertFalse( wRO.isPresent() );
	}
}


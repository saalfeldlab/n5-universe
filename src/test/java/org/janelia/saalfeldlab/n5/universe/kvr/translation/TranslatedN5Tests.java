package org.janelia.saalfeldlab.n5.universe.kvr.translation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.imglib2.img.Img;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.kvr.TranslatedN5Reader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TranslatedN5Tests {

	private File tempDir;
	private N5FSWriter n5;
	private Img<UnsignedByteType> img;

	@Before
	public void before() throws IOException {

		tempDir = Files.createTempDirectory("n5-kvr-translation-test-").toFile();
		tempDir.deleteOnExit();

		final File containerDir = new File(tempDir, "n5Translation.n5");
		n5 = new N5FSWriter(containerDir.getCanonicalPath());
		img = RandomImgs.seed(945).nextImage(new UnsignedByteType(), 3, 4, 5);
		N5Utils.save(img, n5, "img", new int[] {3, 4, 5}, new RawCompression());
	}

	@After
	public void after() {

		if (n5 != null)
			n5.remove();
		if (tempDir != null)
			tempDir.delete();
	}

	@Test
	public void testPathTranslation() {

		n5.createGroup("/pathXlation");
		n5.createDataset("/pathXlation/src",
				new DatasetAttributes( new long[]{16,16}, new int[]{16,16}, DataType.UINT8, new RawCompression()));
		assertTrue("pathXlation src exists", n5.exists("/pathXlation/src"));

		// move "img" dataset to "data"
		final String fwdTranslation = "include \"n5\"; moveSubTree( \"/img\"; \"data\" )";
		final String invTranslation = "include \"n5\"; moveSubTree( \"/data\"; \"img\" )";
		final TranslatedN5Reader n5Xlated = new TranslatedN5Reader(n5, fwdTranslation, invTranslation);

		assertTrue("translated dataset exists", n5Xlated.exists("data"));
		assertNotNull("translated dataset attributes exist", n5Xlated.getDatasetAttributes("data"));
		assertEquals("img", n5Xlated.originalPath("data"));

		final Img<UnsignedByteType> imgFromXlated = N5Utils.open(n5Xlated, "data");
		ImgLib2Assert.assertImageEquals(img, imgFromXlated);
	}

}

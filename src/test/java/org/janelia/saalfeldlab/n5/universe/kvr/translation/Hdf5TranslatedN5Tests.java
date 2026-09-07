package org.janelia.saalfeldlab.n5.universe.kvr.translation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import net.imglib2.img.Img;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.kvr.TranslatedN5Reader;
import org.janelia.saalfeldlab.n5.universe.kvr.TranslatedN5Writer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Translation of an HDF5 container.
 * <p>
 * HDF5 is <em>read-only</em> here: it has no attribute files, so its {@code
 * HierarchyStore} synthesizes a virtual "attributes.json" per group/dataset
 * that can be harvested but not written back. Reading a translated view of an
 * HDF5 container works; {@link TranslatedN5Writer} refuses one.
 */
public class Hdf5TranslatedN5Tests {

	private static final int[] BLOCK_SIZE = {3, 4, 5};

	private File tempDir;
	private N5HDF5Writer n5;
	private Img<UnsignedByteType> img;

	@Before
	public void before() throws IOException {

		tempDir = Files.createTempDirectory("n5-kvr-hdf5-translation-test-").toFile();
		tempDir.deleteOnExit();

		final File containerFile = new File(tempDir, "translation.h5");
		n5 = new N5HDF5Writer(containerFile.getCanonicalPath());

		// NB: several chunks per dimension, so that chunk routing through
		// originalPath() is exercised for more than the origin chunk.
		img = RandomImgs.seed(945).nextImage(new UnsignedByteType(), 6, 8, 10);
		N5Utils.save(img, n5, "img", BLOCK_SIZE, new RawCompression());

		n5.createGroup("/nested/group");
		n5.setAttribute("/nested/group", "answer", 42);
		n5.setAttribute("img", "name", "the image");
	}

	@After
	public void after() {

		if (n5 != null)
			n5.remove();
		if (tempDir != null)
			tempDir.delete();
	}

	/**
	 * Under the identity translation the view has to behave like the HDF5
	 * container it wraps.
	 */
	@Test
	public void testIdentityTranslation() {

		final TranslatedN5Reader n5Xlated = new TranslatedN5Reader(n5, ".", ".");

		assertTrue("dataset exists", n5Xlated.datasetExists("img"));
		assertTrue("group exists", n5Xlated.exists("/nested/group"));
		assertFalse("nonexistent path", n5Xlated.exists("/nope"));

		final String[] rootList = n5Xlated.list("");
		Arrays.sort(rootList);
		assertArrayEquals("root listing", new String[]{"img", "nested"}, rootList);
		assertArrayEquals("nested listing", new String[]{"group"}, n5Xlated.list("/nested"));

		// a dataset is a group with no children
		assertArrayEquals("dataset listing", new String[]{}, n5Xlated.list("img"));

		assertEquals("group attribute", (Integer)42,
				n5Xlated.getAttribute("/nested/group", "answer", Integer.class));
		assertEquals("dataset attribute", "the image",
				n5Xlated.getAttribute("img", "name", String.class));

		// HDF5 derives the dataset attributes rather than storing them; the
		// harvested "attributes.json" carries them, so they survive translation.
		final DatasetAttributes expected = n5.getDatasetAttributes("img");
		final DatasetAttributes actual = n5Xlated.getDatasetAttributes("img");
		assertNotNull("translated dataset attributes exist", actual);
		assertArrayEquals("dimensions", expected.getDimensions(), actual.getDimensions());
		assertArrayEquals("blockSize", expected.getBlockSize(), actual.getBlockSize());
		assertEquals("dataType", expected.getDataType(), actual.getDataType());

		ImgLib2Assert.assertImageEquals(img, N5Utils.open(n5Xlated, "img"));
	}

	/**
	 * Chunks are read from the delegate at the {@link
	 * TranslatedN5Reader#originalPath} of the dataset, so pixels have to come
	 * back through a moved path.
	 */
	@Test
	public void testPathTranslation() {

		final String fwdTranslation = "include \"n5\"; moveSubTree( \"/img\"; \"data\" )";
		final String invTranslation = "include \"n5\"; moveSubTree( \"/data\"; \"img\" )";
		final TranslatedN5Reader n5Xlated = new TranslatedN5Reader(n5, fwdTranslation, invTranslation);

		assertTrue("translated dataset exists", n5Xlated.exists("data"));
		assertFalse("original path is gone", n5Xlated.exists("img"));
		assertNotNull("translated dataset attributes exist", n5Xlated.getDatasetAttributes("data"));
		assertEquals("img", n5Xlated.originalPath("data"));

		ImgLib2Assert.assertImageEquals(img, N5Utils.open(n5Xlated, "data"));
	}

	/**
	 * Writing a translated hierarchy back needs {@code createDirectories} and
	 * {@code writeAttributesJson} on the delegate's {@code HierarchyStore},
	 * which HDF5 does not implement. The refusal happens in the constructor,
	 * before anything is harvested or translated.
	 */
	@Test
	public void testWriterRefusesHdf5() {

		final N5Exception e = assertThrows(N5Exception.class,
				() -> new TranslatedN5Writer(n5, ".", "."));
		assertTrue("message names the dialect: " + e.getMessage(),
				e.getMessage().contains("HDF5"));
	}
}

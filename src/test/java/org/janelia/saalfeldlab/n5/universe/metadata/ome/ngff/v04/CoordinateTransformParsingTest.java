package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.junit.Test;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;

public class CoordinateTransformParsingTest {

	private static final double EPS = 1E-6;

	@Test
	public void testCoordinateTransformParsing() {

		URI rootF = Paths.get("src", "test", "resources", "metadata.zarr").toUri();
		final N5Reader zarr = new N5Factory().openReader(rootF.toString());

		final OmeNgffMetadataParser grpParser = new OmeNgffMetadataParser();
		test(grpParser.parseMetadata(zarr, setupNode("coordTforms/ss", "s0")),
				new double[]{4, 4},
				new double[]{0, 0});

		test(grpParser.parseMetadata(zarr, setupNode("coordTforms/st", "s0")),
				new double[]{2, 2},
				new double[]{10, 10});

		test(grpParser.parseMetadata(zarr, setupNode("coordTforms/ts", "s0")),
				new double[]{2, 2},
				new double[]{20, 20});

		test(grpParser.parseMetadata(zarr, setupNode("coordTforms/tt", "s0")),
				new double[]{1, 1},
				new double[]{20, 20});
	}

	private void test(final Optional<OmeNgffMetadata> metaOpt, final double[] expectedScale, final double[] expectedTranslation) {

		assertTrue("ss not parsable", metaOpt.isPresent());

		final OmeNgffMetadata meta = metaOpt.get();
		final AffineGet[] tforms = meta.spatialTransforms();
		assertTrue("ss has one transform", tforms.length == 1);

		final ScaleAndTranslation tform = (ScaleAndTranslation)tforms[0];
		assertArrayEquals(expectedScale, tform.getScaleCopy(), EPS);
		assertArrayEquals(expectedTranslation, tform.getTranslationCopy(), EPS);
	}

	private N5TreeNode setupNode(final String path, final String childPath) {

		final N5TreeNode node = new N5TreeNode(path);
		final N5TreeNode child = new N5TreeNode(path + "/" + childPath);
		final DatasetAttributes attrs = new DatasetAttributes(new long[]{4, 4}, new int[]{4, 4}, DataType.UINT8, new RawCompression());
		child.setMetadata(new N5SingleScaleMetadata(path + "/" + childPath, new AffineTransform3D(),
				new double[]{1, 1}, new double[]{1, 1}, new double[]{0, 0}, "", attrs, false));

		node.add(child);
		return node;
	}

}

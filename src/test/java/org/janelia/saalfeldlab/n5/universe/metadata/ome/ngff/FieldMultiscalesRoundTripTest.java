package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AbstractParametrizedFieldTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.DisplacementFieldCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ScaleCoordinateTransform;
import org.junit.Test;

import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Round-trips a displacement field written with NGFF multiscales metadata only
 * exercising the Stage 2 reader path in {@link AbstractParametrizedFieldTransform}.
 */
public class FieldMultiscalesRoundTripTest {

	private static final String GROUP = "dfield";

	@Test
	public void testMultiscalesOnlyRoundTrip() throws IOException {

		final String root = Files.createTempDirectory("dfieldRT").resolve("test.zarr").toString();

		// field array: [vector=2, x=3, y=4]
		final float[] data = new float[2 * 3 * 4];
		final int[] blk = new int[] { 2, 3, 4 };

		try (final N5Writer zarr = new N5Factory().openWriter(StorageFormat.ZARR3, root)) {

			N5Utils.save(ArrayImgs.floats(data, 2, 3, 4), zarr, GROUP, blk, new RawCompression());

			// imglib2 order [d, x, y]; scale [vector=1, x=2, y=3]
			final Axis[] axes = new Axis[] {
					new Axis(Axis.DISPLACEMENT, "d", null, true),
					new Axis(Axis.SPACE, "x", "pixel"),
					new Axis(Axis.SPACE, "y", "pixel") };
			final CoordinateSystem[] cs = new CoordinateSystem[] { new CoordinateSystem("field", axes) };

			final OmeNgffMultiScaleMetadata.OmeNgffDataset dset = new OmeNgffMultiScaleMetadata.OmeNgffDataset();
			dset.path = ".";
			dset.coordinateTransformations = new CoordinateTransform[] {
					new ScaleCoordinateTransform("", ".", "field", new double[] { 1, 2, 3 }) };

			final OmeNgffMultiScaleMetadata ms = new OmeNgffMultiScaleMetadata(
					3, "/", null, null, "0.5", null,
					new OmeNgffMultiScaleMetadata.OmeNgffDataset[] { dset },
					cs, null, null, null,
					new NgffSingleScaleAxesMetadata[ 0 ]);

			final OmeNgffMetadata meta = new OmeNgffMetadata("", new OmeNgffMultiScaleMetadata[] { ms });
			try {
				new OmeNgffMetadataParser(zarr).writeMetadata(meta, zarr, GROUP);
			} catch (final Exception e) {
				throw new IOException(e);
			}
		}

		try (final N5Reader zarr = new N5Factory().openReader(StorageFormat.ZARR3, root)) {

			// pixel->physical read from multiscales: scale un-reversed to imglib2 [1,2,3]
			final CoordinateTransform<?> ct = AbstractParametrizedFieldTransform
					.findPixelToPhysicalFromMultiscales(zarr, GROUP, "field");
			assertNotNull("pixel->physical from multiscales", ct);
			assertEquals("scale", ct.getType());
			final double[] scale = ((ScaleCoordinateTransform) ct).scale;
			assertEquals(1.0, scale[0], 1e-9);
			assertEquals(2.0, scale[1], 1e-9);
			assertEquals(3.0, scale[2], 1e-9);

			// full field read: parseVectorAxisIndex reads coordinateSystems from multiscales
			final DisplacementFieldCoordinateTransform<FloatType> df =
					new DisplacementFieldCoordinateTransform<>("df", GROUP, "linear", "field", "world");
			assertEquals("displacement axis index", 0, df.parseVectorAxisIndex(zarr));
			assertNotNull("field parameters resolve end-to-end", df.getParameters(zarr));
		}
	}
}

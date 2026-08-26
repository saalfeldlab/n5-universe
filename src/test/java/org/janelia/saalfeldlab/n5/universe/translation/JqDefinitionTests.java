package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.janelia.saalfeldlab.n5.universe.metadata.FinalVoxelDimensions;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.ScaleOffsetSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the shared jq definitions in {@code /common.jq} that map
 * storage-specific spatial metadata onto the canonical form.
 */
public class JqDefinitionTests {

	private static final Gson gson = JqUtils.buildGson(null);

	/**
	 * Apply {@code definition} to the attributes of a single N5 node holding
	 * {@code attributes}, and read the result back as canonical metadata.
	 */
	private static CanonicalSpatialDatasetMetadata translateAttributes(final String definition, final Object attributes) {

		final Directory dir = new Directory();
		dir.putAttributes("attributes.json", gson.toJsonTree(attributes));

		final JqTranslation translation = new JqTranslation(DialectInfo.N5,
				"include \"n5\"; setAttrs(" + definition + ")");
		final JsonElement translated = translation.translate(dir).getAttributes("attributes.json");

		return gson.fromJson(translated, CanonicalSpatialDatasetMetadata.class);
	}

	@Test
	public void testN5vToCanonicalScaleOffset() {

		final N5vMeta meta = new N5vMeta();
		meta.pixelResolution = new FinalVoxelDimensions("um", 1, 2, 3);

		final SpatialTransform transform = translateAttributes("n5vToCanonicalScaleOffset", meta)
				.getSpatialTransform().transform();
		assertTrue("is ScaleOffsetSpatialTransform", transform instanceof ScaleOffsetSpatialTransform);

		final ScaleOffsetSpatialTransform scaleOffset = (ScaleOffsetSpatialTransform)transform;
		assertArrayEquals(new double[]{1, 2, 3}, scaleOffset.scale, 1e-9);
		assertArrayEquals(new double[]{0, 0, 0}, scaleOffset.offset, 1e-9);
	}

	@Test
	public void testCosemToTransform() {

		final CosemMeta meta = new CosemMeta();
		meta.transform = new CosemTransform(
				new String[]{"z", "y", "x"},
				new double[]{3, 2, 1},
				new double[]{0.3, 0.2, 0.1},
				new String[]{"um", "um", "um"});

		final SpatialTransform transform = translateAttributes("cosemToTransform", meta)
				.getSpatialTransform().transform();
		assertTrue("is ScaleOffsetSpatialTransform", transform instanceof ScaleOffsetSpatialTransform);

		final ScaleOffsetSpatialTransform scaleOffset = (ScaleOffsetSpatialTransform)transform;
		assertArrayEquals(new double[]{1, 2, 3}, scaleOffset.scale, 1e-9);
		assertArrayEquals(new double[]{0.1, 0.2, 0.3}, scaleOffset.offset, 1e-9);
	}

	private static class CosemMeta {

		public CosemTransform transform;
	}

	private static class N5vMeta {

		public FinalVoxelDimensions pixelResolution;
	}
}

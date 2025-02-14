package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.junit.BeforeClass;
import org.junit.Test;

import net.imglib2.transform.integer.MixedTransform;


public class MapAxisTests {

    static CoordinateSystem inXy;
    static CoordinateSystem outXy;
    static CoordinateSystem inXyz;
    static CoordinateSystem outXyz;

    static double[] identity2dFlat;
    static double[] identity3dFlat;

    static double[] permutation2dFlat;
    static double[] permutation3dFlat;

    static double EPSILON = 1e-6;

	@BeforeClass
	public static void before() {

		inXy = Common.makeSpace("in", "space", Unit.micrometer, "x", "y");
		outXy = Common.makeSpace("out", "space", Unit.micrometer, "x", "y");

		inXyz = Common.makeSpace("in", "space", Unit.micrometer, "x", "y", "z");
		outXyz = Common.makeSpace("out", "space", Unit.micrometer, "x", "y", "z");

		identity2dFlat = new double[] { 1, 0, 0, 0, 1, 0 };
		identity3dFlat = new double[] {
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0 };

		permutation2dFlat = new double[] { 0, 1, 0, 1, 0, 0 };
	}

	@Test
	public void testValidateSuccess() {

		final String[] axisNames = new String[] {"x", "y", "z"};
		Map<String, String> identity2d = Stream.of(axisNames).limit(2).collect(Collectors.toMap(x -> x, x -> x));
		Map<String, String> identity3d = Stream.of(axisNames).collect(Collectors.toMap(x -> x, x -> x));

		MapAxisCoordinateTransform id2 = new MapAxisCoordinateTransform(inXy, outXy, identity2d);
		assertArrayEquals(identity2dFlat, id2.getTransform().getRowPackedCopy(), EPSILON);

		MapAxisCoordinateTransform id3 = new MapAxisCoordinateTransform(inXyz, outXyz, identity3d);
		assertArrayEquals(identity3dFlat, id3.getTransform().getRowPackedCopy(), EPSILON);

		final Map<String, String> permutation2d = new HashMap<>();
		permutation2d.put("x", "y"); permutation2d.put("y", "x");

		MapAxisCoordinateTransform perm2d = new MapAxisCoordinateTransform(inXy, outXy, permutation2d);
		assertArrayEquals(permutation2dFlat, perm2d.getTransform().getRowPackedCopy(), EPSILON);

		final Map<String, String> permutation3dTo2d = new HashMap<>();
		permutation3dTo2d.put("x", "z"); permutation3dTo2d.put("y", "x");

		MapAxisCoordinateTransform perm2dTo3d = new MapAxisCoordinateTransform(inXyz, outXy, permutation3dTo2d);
		MixedTransform transform = perm2dTo3d.getDiscreteTransform();

		int[] x = new int[3];
		int[] y = new int[2];

		x[0] = 1; x[1] = 0; x[2] = 0;
		transform.apply(x, y);
		assertArrayEquals(new int[]{ 0, 1 }, y);

		x[0] = 0; x[1] = 1; x[2] = 0;
		transform.apply(x, y);
		assertArrayEquals(new int[]{ 0, 0 }, y);

		x[0] = 0; x[1] = 1; x[2] = 1;
		transform.apply(x, y);
		assertArrayEquals(new int[]{ 1, 0 }, y);
	}

	@Test
	public void testValidateMissingOutputAxis() {
		Map<String, String> axisMapping = new HashMap<>();
		axisMapping.put("x", "x");

		Exception exception = assertThrows(N5Exception.class,
				() -> new MapAxisCoordinateTransform(inXy, outXy, axisMapping));

		assertTrue(exception.getMessage().contains("All output axes must be keys for mapAxis"));
	}

    @Test
    public void testValidateInvalidOutputKey() {
        Map<String, String> axisMapping = new HashMap<>();
        axisMapping.put("a", "X");

		Exception exception = assertThrows(N5Exception.class,
				() -> new MapAxisCoordinateTransform(inXy, outXy, axisMapping));

        assertTrue(exception.getMessage().contains("is not an output axis"));
    }

	@Test
	public void testValidateInvalidInputValue() {
		Map<String, String> axisMapping = new HashMap<>();
		axisMapping.put("x", "A");

		Exception exception = assertThrows(N5Exception.class,
				() -> new MapAxisCoordinateTransform(inXy, outXy, axisMapping));

		assertTrue(exception.getMessage().contains("is not an input axis"));
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.junit.Test;

public class UnitTest {

	@Test
	public void unitParseTest() {

		assertEquals( Unit.meter, Unit.fromString("m"));
		assertEquals( Unit.meter, Unit.fromString("meter"));
		assertEquals( Unit.meter, Unit.fromString("METER"));

		// SI abbreviations are case-sensitive, but whole names are not
		assertEquals( Unit.millimeter, Unit.fromString("millimeter"));
		assertEquals( Unit.millimeter, Unit.fromString("MILLIMETER"));
		assertEquals( Unit.millimeter, Unit.fromString("mm"));
		assertEquals( Unit.megameter, Unit.fromString("Mm"));

		assertEquals(Unit.micrometer, Unit.fromString("micrometer"));
		assertEquals(Unit.micrometer, Unit.fromString("um"));
		assertEquals(Unit.micrometer, Unit.fromString(Unit.MICRO + "m"));
		assertEquals(Unit.micrometer, Unit.fromString(Unit.MU + "m"));

		assertEquals(Unit.yard, Unit.fromString("yard"));
		assertEquals(Unit.yard, Unit.fromString("YARD"));
		assertEquals(Unit.yard, Unit.fromString("yd"));
		assertEquals(Unit.yard, Unit.fromString("YD"));
		assertEquals(Unit.yard, Unit.fromString("yD"));
		assertEquals(Unit.yard, Unit.fromString("Yd"));

		assertEquals(null,  Unit.fromString("not-a-unit"));
	}

}

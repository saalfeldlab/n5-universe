package org.janelia.saalfeldlab.n5.universe.metadata.axes;

/*
 * Allowed units and their corresponding types for OME-NGFF v0.4.
 *
 * see https://ngff.openmicroscopy.org/0.4/#axes-md
 */
public enum Unit {

	angstrom("space"), attometer("space"), centimeter("space"), decimeter("space"), exameter("space"),
	femtometer("space"), foot("space"), gigameter("space"), hectometer("space"), inch("space"), kilometer("space"),
	megameter("space"), meter("space"), micrometer("space"), mile("space"), millimeter("space"), nanometer("space"),
	parsec("space"), petameter("space"), picometer("space"), terameter("space"), yard("space"), yoctometer("space"),
	yottameter("space"), zeptometer("space"), zettameter("space"),

	attosecond("time"), centisecond("time"), day("time"), decisecond("time"), exasecond("time"), femtosecond("time"),
	gigasecond("time"), hectosecond("time"), hour("time"), kilosecond("time"), megasecond("time"), microsecond("time"),
	millisecond("time"), minute("time"), nanosecond("time"), petasecond("time"), picosecond("time"), second("time"),
	terasecond("time"), yoctosecond("time"), yottasecond("time"), zeptosecond("time"), zettasecond("time");

	private final String type;

	Unit(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public boolean isType(final String type) {
		return this.type.equals(type.toLowerCase());
	}

}

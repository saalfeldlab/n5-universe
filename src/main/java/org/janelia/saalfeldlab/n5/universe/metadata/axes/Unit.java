package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Optional;

import javax.annotation.Nullable;

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

	public static final char MU = 'μ';

	Unit(String type) {

		this.type = type;
	}

	public String getType() {

		return type;
	}

	public boolean isType(final String type) {

		return this.type.equals(type.toLowerCase());
	}

	/**
	 * 
	 * @param unitString a string representation
	 * @return a unit, if possible
	 */
	@Nullable
	public static Unit fromString( final String unitString ) {

		final String unitNorm = unitString.trim();
		return tryParse(unitNorm.toLowerCase())
				.orElse(fromAbbreviation(unitNorm));
	}

	private static Optional<Unit> tryParse(final String unit) {

		try {
			return Optional.of(Unit.valueOf(unit.toLowerCase()));
		} catch (Exception ignore) {}

		return Optional.empty();
	}

	@Nullable
	private static Unit fromAbbreviation(final String abbrev) {

		return fromSiAbbreviation(abbrev)
				.orElse(fromOtherAbbreviation(abbrev.toLowerCase())
				.orElse(null));
	}

	private static Optional<Unit> fromSiAbbreviation(final String si) {

		if (si.length() == 1) {
			return tryParse(siUnitFromSymbol(si.charAt(0)));
		}
		else if (si.length() == 2) {
			return tryParse(
					siPrefixFromSymbol(si.charAt(0)) +
					siUnitFromSymbol(si.charAt(1)));
		}
		return Optional.empty();
	}

	private static String siPrefixFromSymbol(char prefix) {

		switch (prefix) {
		case 'Q':
			return "quetta";
		case 'R':
			return "rotta";
		case 'Y':
			return "yotta";
		case 'Z':
			return "zetta";
		case 'E':
			return "exa";
		case 'P':
			return "peta";
		case 'T':
			return "tera";
		case 'G':
			return "giga";
		case 'M':
			return "mega";
		case 'k':
			return "kilo";
		case 'h':
			return "hecto";
		case 'd':
			return "deci";
		case 'c':
			return "centi";
		case 'm':
			return "milli";
		case 'u':
			return "micro";
		case 'μ':
			return "micro";
		case 'n':
			return "nano";
		case 'p':
			return "pico";
		case 'f':
			return "fempto";
		case 'a':
			return "atto";
		case 'z':
			return "zepto";
		case 'y':
			return "yocto";
		}
		return null;
	}

	private static String siUnitFromSymbol(char suffix) {

		// the only currently valid units:
		// * ImageJ supports only space and time
		// * NGFF v0.4 supports only space and time
		switch (suffix) {
		case 'm':
			return "meter";
		case 's':
			return "second";
		}
		return null;
	}

	/**
	 * Covers units that are not SI.
	 * 
	 * @param abbreviation
	 * @return a unit, or null
	 */
	private static Optional<Unit> fromOtherAbbreviation(final String abbreviation) {

		switch (abbreviation) {
		case "in":
			return Optional.of(Unit.inch);
		case "ft":
			return Optional.of(Unit.foot);
		case "yd":
			return Optional.of(Unit.yard);
		case "mi":
			return Optional.of(Unit.mile);
		case "pc":
			return Optional.of(Unit.parsec);
		case "min":
			return Optional.of(Unit.minute);
		case "sec":
			return Optional.of(Unit.second);
		case "hr":
			return Optional.of(Unit.hour);
		case "d":
			return Optional.of(Unit.day);
		}
		return Optional.empty();
	}

}

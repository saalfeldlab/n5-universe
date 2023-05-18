package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

public class Axis {

	private final String name;

	private final String type;

	private final String unit;

	public Axis(final String name, final String type, final String unit) {
		this.name = name;
		this.type = type;
		this.unit = unit;
	}

	public String getLabel() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getUnit() {
		return unit;
	}
}

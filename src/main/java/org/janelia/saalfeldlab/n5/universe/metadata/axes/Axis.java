package org.janelia.saalfeldlab.n5.universe.metadata.axes;

public class Axis {

	public static final String SPACE = "space";
	public static final String CHANNEL = "channel";
	public static final String TIME = "time";

	protected String type;

	protected String label;

	protected String unit;

	public Axis( final String type, final String label, final String unit )
	{
		this.type = type;
		this.label = label;
		this.unit = unit;
	}

	public String getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public String getUnit() {
		return unit;
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Axis) {
			final Axis axis = (Axis) other;
			return label.equals(axis.label) && type.equals(axis.type) && unit.equals(axis.unit);
		}
		return false;
	}

	@Override
	public String toString() {

		return String.format("axis %s: \"%s\" (%s)", type, label, unit );
	}
}

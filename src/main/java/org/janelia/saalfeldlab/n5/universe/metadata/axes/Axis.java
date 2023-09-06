package org.janelia.saalfeldlab.n5.universe.metadata.axes;

public class Axis {

	public static final String SPACE = "space";
	public static final String CHANNEL = "channel";
	public static final String TIME = "time";
	public static final String DISPLACEMENT = "displacement";
	public static final String ARRAY = "array";

	protected String type;

	protected String label;

	protected String unit;

	protected boolean discrete;

	public Axis( final String type, final String label, final String unit, final boolean discrete )
	{
		this.type = type;
		this.label = label;
		this.unit = unit;
		this.discrete = discrete;
	}

	public Axis( final String type, final String label, final String unit )
	{
		this( type, label, unit, false );
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

	public static Axis defaultArray(final int index) {

		return new Axis(String.format("dim_%d", index), ARRAY, null, true);
	}

	public static Axis[] space(final String unit, final String... names) {

		final Axis[] axes = new Axis[names.length];
		for (int i = 0; i < names.length; i++)
			axes[i] = new Axis(names[i], SPACE, unit, false);

		return axes;
	}
}

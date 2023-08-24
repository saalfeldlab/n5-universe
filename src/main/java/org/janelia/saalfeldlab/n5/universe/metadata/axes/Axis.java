package org.janelia.saalfeldlab.n5.universe.metadata.axes;

public class Axis {

	private final String type;

	private final String label;

	private final String unit;

	private final boolean discrete;

	public Axis(final String label, final String type, final String unit, final boolean discrete) {

		this.label = label;
		this.type = type;
		this.unit = unit;
		this.discrete = discrete;
	}

	public Axis(final String label, final String type, final String unit) {

		this(label, type, unit, false);
	}

	public Axis(final String label, final String type) {

		this(label, type, null, false);
	}

	public Axis(final String label) {

		this(label, null, null, false);
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
			final Axis axis = (Axis)other;
			return label.equals(axis.label) && type.equals(axis.type) && unit.equals(axis.unit);
		}
		return false;
	}

	public Axis copy(final String name) {

		return new Axis(name, type, unit, discrete);
	}

	public Axis copy() {

		return new Axis(label, type, unit, discrete);
	}

	public static Axis defaultArray(final int index) {

		return new Axis(String.format("dim_%d", index), "array", null, true);
	}

	public static Axis[] space(final String unit, final String... names) {

		final Axis[] axes = new Axis[names.length];
		for (int i = 0; i < names.length; i++)
			axes[i] = new Axis(names[i], "space", unit, false);

		return axes;
	}
}

package org.janelia.saalfeldlab.n5.universe.metadata.axes;

public class Axis {

	public static final String SPACE = "space";
	public static final String CHANNEL = "channel";
	public static final String TIME = "time";
	public static final String DISPLACEMENT = "displacement";
	public static final String ARRAY = "array";

	protected String type;

	protected String name;

	protected String unit;

	protected boolean discrete;

	/**
	 * see https://ngff.openmicroscopy.org/0.4/index.html#axes-md
	 * 
	 * @param name must be unique across all name fields (typical: t,c,z,y,x)
	 * @param type Axis.SPACE, Axis.CHANNEL, Axis.TIME, Axis.DISPLACEMENT or Axis.ARRAY (optional)
	 * @param unit must be chosen from a list of allowed units, can otherwise lead to issues down the road, e.g. in Neuroglancer (optional)
	 * @param discrete not part of the spec yet, DISCRETE means don't interpolate between values (optional)
	 */
	public Axis( final String type, final String name, final String unit, final boolean discrete )
	{
		this.type = type;
		this.name = name;
		this.unit = unit;
		this.discrete = discrete;
	}

	/**
	 * see https://ngff.openmicroscopy.org/0.4/index.html#axes-md
	 * 
	 * @param name must be unique across all name fields (typical: t,c,z,y,x)
	 */
	public Axis( final String name )
	{
		this( null, name, null, false );
	}

	/**
	 * see https://ngff.openmicroscopy.org/0.4/index.html#axes-md
	 * 
	 * @param name must be unique across all name fields (typical: t,c,z,y,x)
	 * @param type Axis.SPACE, Axis.CHANNEL, Axis.TIME, Axis.DISPLACEMENT or Axis.ARRAY (optional)
	 */
	public Axis( final String type, final String name )
	{
		this( type, name, null, false );
	}

	/**
	 * see https://ngff.openmicroscopy.org/0.4/index.html#axes-md
	 * 
	 * @param name must be unique across all name fields (typical: t,c,z,y,x)
	 * @param type Axis.SPACE, Axis.CHANNEL, Axis.TIME, Axis.DISPLACEMENT or Axis.ARRAY (optional)
	 * @param unit must be chosen from a list of allowed units, can otherwise lead to issues down the road, e.g. in Neuroglancer (optional)
	 */
	public Axis( final String type, final String name, final String unit )
	{
		this( type, name, unit, false );
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getUnit() {
		return unit;
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Axis) {
			final Axis axis = (Axis) other;
			return name.equals(axis.name) && type.equals(axis.type) && unit.equals(axis.unit);
		}
		return false;
	}

	@Override
	public String toString() {

		return String.format("axis %s: \"%s\" (%s)", type, name, unit );
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

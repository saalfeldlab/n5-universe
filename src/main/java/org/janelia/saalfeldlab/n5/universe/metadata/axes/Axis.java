package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Objects;

public class Axis {

	public static final String SPACE = "space";
	public static final String CHANNEL = "channel";
	public static final String TIME = "time";
	public static final String DISPLACEMENT = "displacement";
	public static final String ARRAY = "array";

	protected String type;

	protected String name;

	protected String unit;
	
	protected Orientation orientation;

	protected boolean discrete;

	/**
	 * see https://ngff.openmicroscopy.org/0.4/index.html#axes-md
	 * 
	 * @param type Axis.SPACE, Axis.CHANNEL, Axis.TIME, Axis.DISPLACEMENT or Axis.ARRAY (optional)
	 * @param name must be unique across all name fields (typical: t,c,z,y,x)
	 * @param unit must be chosen from a list of allowed units, can otherwise lead to issues down the road, e.g. in Neuroglancer (optional)
	 * @param discrete not part of the spec yet, DISCRETE means don't interpolate between values (optional)
	 */
	public Axis( final String type, final String name, final String unit, final boolean discrete )
	{
		this( type, name, unit, discrete, null );
	}

	/**
	 * see https://ngff.openmicroscopy.org/rfc/4/index.html
	 * 
	 * @param type Axis.SPACE, Axis.CHANNEL, Axis.TIME, Axis.DISPLACEMENT or Axis.ARRAY (optional)
	 * @param name must be unique across all name fields (typical: t,c,z,y,x)
	 * @param unit must be chosen from a list of allowed units, can otherwise lead to issues down the road, e.g. in Neuroglancer (optional)
	 * @param discrete not part of the spec yet, DISCRETE means don't interpolate between values (optional)
	 * @param orientation only permitted on Axis.SPACE axes, must be valid (optional)
	 * @throws IllegalArgumentException if the orientation is not permitted on this axis
	 */
	public Axis( final String type, final String name, final String unit, final boolean discrete, final Orientation orientation )
	{
		if( orientation != null && !isOrientationPermitted( type, orientation ))
			throw new IllegalArgumentException( String.format( "orientation (%s) not permitted on %s axis \"%s\"", orientation, type, name ));

		this.type = type;
		this.name = name;
		this.unit = unit;
		this.discrete = discrete;
		this.orientation = orientation;
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

	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * Orientations are only permitted on space axes, and must be valid.
	 *
	 * @param type the axis type
	 * @param orientation the orientation
	 * @return true if the orientation is permitted on an axis of the given type
	 */
	public static boolean isOrientationPermitted(final String type, final Orientation orientation) {

		return SPACE.equals(type) && orientation.isValid();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Axis) {
			final Axis axis = (Axis) other;
			return Objects.equals(name, axis.name) && Objects.equals(type, axis.type) && Objects.equals(unit, axis.unit)
					&& Objects.equals(orientation, axis.orientation);
		}
		return false;
	}

	@Override
	public int hashCode() {

		return Objects.hash(name, type, unit, orientation);
	}

	@Override
	public String toString() {

		if (orientation == null)
			return String.format("axis %s: \"%s\" (%s)", type, name, unit );
		else
			return String.format("axis %s: \"%s\" (%s) [%s]", type, name, unit, orientation );
	}

	public static Axis defaultArray(final int index) {

		return new Axis(ARRAY, String.format("dim_%d", index), null, true);
	}

	public static Axis[] space(final String unit, final String... names) {

		final Axis[] axes = new Axis[names.length];
		for (int i = 0; i < names.length; i++)
			axes[i] = new Axis(SPACE, names[i], unit, false);

		return axes;
	}
}

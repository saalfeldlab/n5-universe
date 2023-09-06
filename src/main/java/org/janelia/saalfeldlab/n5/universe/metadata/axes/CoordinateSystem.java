package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CoordinateSystem
{
	public static final String KEY = "coordinateSystems";

	protected final String name;

	protected final Axis[] axes;

	public CoordinateSystem( final String name, final Axis[] axes )
	{
		this.name = name;
		this.axes = axes;
	}

	public String getName()
	{
		return name;
	}

	public Axis[] getAxes()
	{
		return axes;
	}

	public int numDimensions() {
		return axes.length;
	}

	public String[] getAxisLabels() {
		return Arrays.stream(axes).map( Axis::getLabel).toArray(String[]::new);
	}

	public String[] getAxisTypes() {
		return Arrays.stream(axes).map( Axis::getType).toArray(String[]::new);
	}

	public String[] getUnits() {
		return Arrays.stream(axes).map( Axis::getUnit).toArray(String[]::new);
	}

	public Axis getAxis( final int i ) {
		return axes[i];
	}

	public boolean hasAxis( final String label ) {
		return Arrays.stream(axes).map(Axis::getLabel).anyMatch(l -> l.equals(label));
	}

	/**
	 * Is a a super set of b.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isSuperspaceOf( final String[] a, final String[] b )
	{
		for( final String l : b )
			if( !contains( l, a ))
				return false;

		return true;
	}

	public boolean isSuperspaceOf( final CoordinateSystem other ) {
		return isSuperspaceOf(other.getAxisLabels());
	}

	public boolean isSuperspaceOf( final String[] axisLabels )
	{
		final String[] mylabels = getAxisLabels();
		for( final String l : axisLabels )
			if( !contains( l, mylabels ))
				return false;

		return true;
	}

	public boolean isSubspaceOf( final CoordinateSystem other )
	{
		return isSubspaceOf(other.getAxisLabels());
	}

	/**
	 * Is a a subset of b.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isSubspaceOf( final String[] a, final String[] b )
	{
		for( final String l : b )
			if( !contains( l, a ))
				return false;

		return true;
	}

	public boolean isSubspaceOf( final String[] axisLabels )
	{
		for( final String l : getAxisLabels() )
			if( !contains( l, axisLabels ))
				return false;

		return true;
	}

	public CoordinateSystem subSpace( final String name, final String... axisLabels )
	{
		return new CoordinateSystem( name,
				Arrays.stream(axes).filter( x -> {return AxisUtils.contains(x.getLabel(), axisLabels);})
				.toArray( Axis[]::new ));
	}

	public CoordinateSystem union( final String name, final CoordinateSystem space )
	{
		return new CoordinateSystem( name,
				Stream.concat(
						Arrays.stream(axes),
						Arrays.stream(space.getAxes()))
				.toArray( Axis[]::new ));
	}

	public CoordinateSystem intersection( final String name, final CoordinateSystem space )
	{
		return subSpace( name, space.getAxisLabels() );
	}

	/**
	 * Returns a space containing the axes of this space
	 * not also part of the given space
	 *
	 * @param name a name for the new space
	 * @param space the space whose axis will be "subtracted"
	 * @return the resulting space
	 */
	public CoordinateSystem diff( final String name, final CoordinateSystem space )
	{
		final String[] axisLabels = space.getAxisLabels();
		return new CoordinateSystem( name,
				Arrays.stream(axes).filter( x -> {return !AxisUtils.contains(x.getLabel(), axisLabels);})
				.toArray( Axis[]::new ));
	}

	/**
	 * Returns true if these two spaces contain the same set of axes,
	 * in any order.
	 *
	 * @param other the other space
	 * @return contain same axes
	 */
	public boolean axesEquals( final CoordinateSystem other ) {

		return axesEquals(other.getAxisLabels());
	}

	/**
	 * Returns true if these two spaces contain the same set of axes,
	 * in any order.
	 *
	 * @param axes the axis labels
	 * @return contain same axes
	 */
	public boolean axesEquals( final String[] axes ) {
		if( axes.length != this.getAxisLabels().length )
			return false;

		return isSubspaceOf(axes);
	}

	private static boolean contains( final String q, final String[] array )
	{
		for( final String t : array )
			if( t.equals(q))
				return true;

		return false;
	}

	public boolean axesLabelsMatch( final String[] labels ) {
		return Arrays.equals(labels, getAxisLabels());
	}

	public boolean hasAllLabels( final String[] labels ) {
		if( getAxisLabels().length != labels.length )
			return false;

		for( final String l : labels )
			if( ! Arrays.stream(axes).map( Axis::getLabel).anyMatch( x -> x.equals(l)))
				return false;

		return true;
	}

	/**
	 *
	 * @param label the label
	 * @return the first index corresponding to that label
	 */
	public int indexOf(final String label) {
		for (int i = 0; i < getAxisLabels().length; i++)
			if (getAxisLabels()[i].equals(label))
				return i;

		return -1;
	}

	public int[] indexesOfType( final String type ) {
		final String[] types = getAxisTypes();
		return IntStream.range(0, getAxisTypes().length )
			.filter( i -> types[i].equals(type))
			.toArray();
	}

	@Override
	public boolean equals( final Object other )
	{
		if ( other instanceof CoordinateSystem )
		{
			return ( ( CoordinateSystem ) other ).getName().equals( getName() );
		}
		else
			return false;
	}

	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}

	public static CoordinateSystem defaultArray( final String name, final int nd )
	{
		return new CoordinateSystem( name,
				IntStream.range( 0, nd ) .mapToObj( i -> Axis.defaultArray( i ) ).toArray( Axis[]::new ) );
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AbstractCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;

public class CoordinateSystems {


	private final static String PREFIX = "DEFAULTSPACE-";

	private final HashMap<String,CoordinateSystem> nameToSpace;

	private final HashMap<String,Axis> nameToAxis;

	private final HashMap<Axis,ArrayList<CoordinateSystem>> axesToSpaces;

	public CoordinateSystems() {
		nameToSpace = new HashMap<>();
		nameToAxis = new HashMap<>();
		axesToSpaces = new HashMap<>();
	}

	public CoordinateSystems( final Collection<CoordinateSystem> spaceList ) {
		this();
		addAll( spaceList );
	}

	public CoordinateSystems( final Stream<CoordinateSystem> spaces ) {
		this();
		addAll( spaces );
	}

	public CoordinateSystems( final CoordinateSystem[] coordinateSystems ) {
		this();
		for( final CoordinateSystem s : coordinateSystems )
			add( s );
	}

	public Stream<CoordinateSystem> coordinateSystems() {
		return nameToSpace.entrySet().stream().map(e -> e.getValue());
	}

	public Collection<CoordinateSystem> getCollection() {
		return nameToSpace.values();
	}

	public Stream<Axis> axes() {
		return nameToAxis.entrySet().stream().map(e -> e.getValue());
	}

	public Axis[] axesFromLabels(final String... axisLabels )
	{
		return Arrays.stream(axisLabels).map( l -> {

			if( nameToAxis.containsKey( l ))
				return nameToAxis.get(l);
			else
			{
				final Axis a = makeDefaultAxis( l );
				add( a );
				return a;
			}
		}).toArray( Axis[]::new );
	}

	public void addAll( final Collection<CoordinateSystem> spaces )
	{
		for( final CoordinateSystem s : spaces )
			add( s );
	}

	public void addAll( final Stream<CoordinateSystem> spaces )
	{
		spaces.forEach( s -> add(s) );
	}

	/**
	 *
	 * @param s the space to add
	 * @return true of the space was added
	 */
	public boolean add( final CoordinateSystem s ) {

		if( nameToSpace.containsKey(s.getName()) ) {
			final CoordinateSystem other = nameToSpace.get(s.getName());
			if( !s.equals(other))
				return false;
		}
		else
			nameToSpace.put( s.getName(), s);

		for( final Axis a : s.getAxes() ) {
			if( add( a ))
			{
				final ArrayList<CoordinateSystem> list = new ArrayList<>();
				list.add(s);
				axesToSpaces.put(a, list);
			}
		}
		return true;
	}

	public boolean add( final Axis a ) {
		if( nameToAxis.containsKey(a.getName()) ) {
			final Axis other = nameToAxis.get(a.getName());
			if( !a.equals(other))
				return false;
		}
		else
			nameToAxis.put(a.getName(), a);

		return true;
	}

	public CoordinateSystem getSpace( final String name ) {
		return nameToSpace.get(name);
	}

	public String[] axesForSpace( final String name )
	{
		return getSpace( name ).getAxisNames();
	}

	public boolean hasSpace( final String name ) {
		return nameToSpace.containsKey(name);
	}

	public String defaultSpaceName( final String... axisLabels ) {
		return PREFIX + Arrays.stream(axisLabels).collect(Collectors.joining("-"));
	}

	public CoordinateSystem makeDefaultSpace( final String... axisLabels ) {
		return makeDefaultSpace( defaultSpaceName( axisLabels ), axisLabels );
	}

	public static Axis makeDefaultAxis( final String name )
	{
		return new Axis( name, "", "", false );
	}

	public CoordinateSystem makeDefaultSpace( final String name, final String... axisLabels ) {
		final Axis[] axes = new Axis[ axisLabels.length ];
		for( int i = 0; i < axisLabels.length; i++ ) {
			final Axis a = nameToAxis.get( axisLabels[i]);
			if( a != null ) {
				axes[i] = a;
			}
			else {
				return null;
			}
		}
		final CoordinateSystem s =  new CoordinateSystem(name, axes);
		add( s );
		return s;
	}

	/**
	 * Returns the first space that contains exactly the given axis labels, in the given order.
	 *
	 * @param axisLabels the axis labels
	 * @return the list of spaces
	 */
	public CoordinateSystem getSpaceFromAxes( final String... axisLabels ) {
		return getSpacesFromAxes(axisLabels).get(0);
	}

	/**
	 * Returns all spaces that contains exactly the given axis labels, in the given order.
	 *
	 * @param axisLabels the axis labels
	 * @return the list of spaces
	 */
	public ArrayList<CoordinateSystem> getSpacesFromAxes( final String... axisLabels ) {
		final ArrayList<CoordinateSystem> list = new ArrayList<CoordinateSystem>(
				coordinateSystems()
				.filter( s -> s.axesLabelsMatch(axisLabels))
				.collect( Collectors.toList()));

		if( list.isEmpty())
			list.add(makeDefaultSpace(axisLabels));

		return list;
	}

	/**
	 * Returns all spaces that contain the given axis labels, in any order.
	 * Spaces in the returned list may contain axes not in the list.
	 *
	 * @param axisLabels the axis labels
	 * @return the list of spaces
	 */
	public ArrayList<CoordinateSystem> getSpacesContainingAxes( final String... axisLabels ) {
		final ArrayList<CoordinateSystem> list = new ArrayList<CoordinateSystem>(
				coordinateSystems()
				.filter( s -> s.hasAllLabels(axisLabels))
				.collect( Collectors.toList()));

		if( list.isEmpty())
			list.add(makeDefaultSpace(axisLabels));

		return list;
	}

	public ArrayList<CoordinateSystem> getSpacesOld( final String... axisLabels ) {
		ArrayList<CoordinateSystem> candidates = null;
		for( final String l : axisLabels )
		{
			final Axis a = nameToAxis.get(l);
			if( a != null ) {
				final ArrayList<CoordinateSystem> spaces = axesToSpaces.get(a);
				if( candidates == null) {
					candidates = new ArrayList<>();
					candidates.addAll(spaces);
				}
				else {
					for( final CoordinateSystem s : spaces )
						if( !candidates.contains(s))
							candidates.remove(s);
				}
			}
		}

		if( candidates.isEmpty())
			candidates.add(makeDefaultSpace(axisLabels));

		return candidates;
	}

	public Axis getAxis( final String name ) {
		return nameToAxis.get(name);
	}

	public CoordinateSystem spaceFrom( final String[] axes ) {
		return getSpaceFromAxes(axes);
	}

	public CoordinateSystem spaceFrom( final String name ) {
		return getSpace(name);
	}

	public String[] getInputAxes( final CoordinateTransform<?> t )
	{
		String[] inAxes = null;
		if( t.getInputAxes() != null )
			inAxes = t.getInputAxes();
		else if( t.getInput() != null )
			inAxes = getSpace(t.getInput()).getAxisNames();

		return inAxes;
	}

	public String[] getOutputAxes( final CoordinateTransform<?>  t )
	{
		String[] outAxes = null;
		if( t.getOutputAxes() != null )
			outAxes = t.getOutputAxes();
		else if( t.getOutput() != null )
			outAxes = getSpace(t.getOutput()).getAxisNames();

		return outAxes;
	}

	public boolean inputIsSubspace( final CoordinateTransform<?>  t, final CoordinateSystem s ) {
		return s.isSubspaceOf( getSpace(t.getInput()));
	}

	public boolean inputHasAxis( final CoordinateTransform<?>  t, final String axisLabel ) {
		return getSpace(t.getInput()).hasAxis(axisLabel);
	}

	public boolean outputIsSubspace( final CoordinateTransform<?>  t, final CoordinateSystem s ) {
		if ( t.getOutputAxes() != null )
			return s.isSubspaceOf(t.getOutputAxes());
		else if( t.getOutput() != null )
			return s.isSubspaceOf( getSpace(t.getOutput()));
		else
			return false;
	}

	public boolean outputIsSuperspace( final CoordinateTransform<?>  t, final CoordinateSystem s ) {
		if ( t.getOutputAxes() != null )
			return s.isSuperspaceOf(t.getOutputAxes());
		else if( t.getOutput() != null )
			return s.isSuperspaceOf( getSpace(t.getOutput()));
		else
			return false;
	}

	public boolean outputHasAxis( final CoordinateTransform<?>  t, final String axisLabel ) {
		if( t.getOutputAxes() != null )
			return Arrays.stream( t.getOutputAxes() ).anyMatch( x -> x.equals(axisLabel));
		else if( t.getOutput() != null )
			return getSpace(t.getOutput()).hasAxis(axisLabel);
		else
			return false;
	}

	public boolean outputMatchesAny( final CoordinateTransform<?>  t, final Set<String> axisLabels ) {
		String[] outAxes = null;
		if( t.getOutputAxes() != null )
			outAxes = t.getOutputAxes();
		else if( t.getOutput() != null )
			outAxes = getSpace(t.getOutput()).getAxisNames();
		else
			return false;

		for( final String axisLabel : t.getOutputAxes())
			if( axisLabels.contains(axisLabel ))
				return true;

		return false;
	}

	public boolean outputMatchesAny( final CoordinateTransform<?>  t, final String[] axisLabels ) {
		String[] outAxes = null;
		if( t.getOutputAxes() != null )
			outAxes = t.getOutputAxes();
		else if( t.getOutput() != null )
			outAxes = getSpace(t.getOutput()).getAxisNames();
		else
			return false;

		for( final String axisLabel : t.getOutputAxes())
			if( Arrays.stream( axisLabels ).filter( x -> x.equals(axisLabel)).count() > 0 )
				return true;

		return false;
	}

	public <T extends CoordinateTransform<?> > void updateTransforms( final Stream<T> transforms )
	{
		transforms.forEach( s -> {
			if( s instanceof AbstractCoordinateTransform) {
				updateTransform( (AbstractCoordinateTransform<?>)s );
			}

			if( s instanceof SequenceCoordinateTransform )
			{
				final SequenceCoordinateTransform t = (SequenceCoordinateTransform)s;
				updateTransforms( Arrays.stream( t.getTransformations() ));
			}
		});

	}

	public < T extends AbstractCoordinateTransform< ? > > void updateTransform( final T t )
	{
//		if( t.getInputSpace() == null && t.getInputAxes() == null )
//		{
//			t.setInputSpace( nameToSpace.get( t.getInputSpace() ) );
//		}
		if ( t.getInput() != null )
			t.setInput( nameToSpace.get( t.getInput() ));
		else if ( t.getInputAxes() != null )
			t.setInput( makeDefault( t.getInputAxes() ));

		if ( t.getOutput() != null )
			t.setOutput( nameToSpace.get( t.getOutput() ));
		else if ( t.getOutputAxes() != null )
			t.setOutput( makeDefault( t.getOutputAxes() ));
	}

	public static String defaultName( final String[] axes ) {
		return String.join( "", axes ) + "(DEFAULT)";
	}

	public CoordinateSystem makeDefault( final String[] axes )
	{
		final Axis[] a = axesFromLabels( axes );
		if ( Arrays.stream( a ).allMatch( x -> x != null ) )
			return new CoordinateSystem( defaultName( axes ), a );
		else
			return null;
	}

	public CoordinateSystem addDefault( final String[] axes )
	{
		final CoordinateSystem space = makeDefault( axes );
		add( space );
		return space;
	}


}

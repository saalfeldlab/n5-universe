package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.SpacesTransforms;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.InverseCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.InvertibleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ByDimensionCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.SequenceCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.StackedCoordinateTransform;

import com.google.gson.Gson;


public class TransformGraph
{
	public final Gson gson;

	private final ArrayList< CoordinateTransform<?> > transforms;

	private final CoordinateSystems spaces;

	private final HashMap< CoordinateSystem, CTNode > spacesToNodes;

	public TransformGraph() {
		spaces = new CoordinateSystems();
		transforms = new ArrayList<>();
		spacesToNodes = new HashMap< CoordinateSystem, CTNode >();
		gson = SpacesTransforms.buildGson();
	}

	public TransformGraph( final List<CoordinateTransform<?>> transforms, final CoordinateSystems spaces ) {

		gson = SpacesTransforms.buildGson();
		this.spaces = spaces;
		this.transforms = new ArrayList<>();

		spacesToNodes = new HashMap< CoordinateSystem, CTNode >();
		for( final CoordinateTransform<?> t : transforms )
			addTransform(t);

		updateTransforms();
	}

	public TransformGraph( final List< CoordinateTransform<?> > transforms, final List<CoordinateSystem> spacesIn ) {
		this( transforms, new CoordinateSystems(spacesIn) );
	}

	public List<CoordinateTransform<?>> getTransforms() {
		return transforms;
	}

	public CoordinateSystems getCoordinateSystems() {
		return spaces;
	}

	public Optional<CoordinateTransform<?>> getTransform( final String name ) {
		return transforms.stream().filter( x -> x.getName().equals(name)).findAny();
	}

	public HashMap< CoordinateSystem, CTNode > getSpaceNodes() {
		return spacesToNodes;
	}

	public CoordinateSystem getInput( final CoordinateTransform<?> t ) {
		return spaces.getSpace(t.getInput().getName());
	}

	public CoordinateSystem getOutput( final CoordinateTransform<?> t ) {
		return spaces.getSpace(t.getOutput().getName());
	}

	public void addTransform( final CoordinateTransform<?> t ) {
		addTransform( t, true );
	}

	private void addTransform( final CoordinateTransform<?> t, final boolean addInverse ) {
		if( transforms.stream().anyMatch( x -> {
			if( x != null && x.getName() != null )
				return x.getName().equals(t.getName());
			else
				return false;
		}))
			return;

		if( spaces.hasSpace(t.getInput().getName()) && spaces.hasSpace(t.getOutput().getName()))
		{
			final CoordinateSystem src = getInput( t );
			if( spacesToNodes.containsKey( src ))
				spacesToNodes.get( src ).edges().add( t );
			else
			{
				final CTNode node = new CTNode( src );
				node.edges().add( t );
				spacesToNodes.put( src, node );
			}

			transforms.add(t);
		}
		else
		{
			transforms.add( t );
		}

		if (addInverse) {
			final CoordinateTransform<?> invT = inverse(t);
			if (invT != null)
				addTransform(invT, false);
		}
	}

	private CoordinateTransform<?> inverse(CoordinateTransform<?> t) {

		if (t instanceof InvertibleCoordinateTransform)
			return new InverseCoordinateTransform((InvertibleCoordinateTransform)t);
		else if (t instanceof SequenceCoordinateTransform)
			return ((SequenceCoordinateTransform)t).inverse();
		else if (t instanceof ByDimensionCoordinateTransform)
			return ((ByDimensionCoordinateTransform)t).inverse();

		return null;
	}

	public void updateTransforms()
	{
		getCoordinateSystems().updateTransforms( getTransforms().stream() );
	}

	public void addSpace( final CoordinateSystem space )
	{
		if( spaces.add(space) ) {
			spacesToNodes.put( space, new CTNode(space));
		}
	}

	public void add( final TransformGraph g )
	{
		g.spaces.coordinateSystems().forEach( s -> addSpace(s));
		g.transforms.stream().forEach( t -> addTransform(t));
	}

	/**
	 * Returns all transforms that have all input axis in the "input" {@link CoordinateSystem}
	 * and all output axis in the "output" CoordinateSystem.
	 *
	 * @param input the input coordinate system
	 * @param output the output coordinate system
	 * @return the "sub-transformations"
	 */
	public List<CoordinateTransform<?>> subTransforms( final CoordinateSystem input, final CoordinateSystem output) {
		return getTransforms().stream().filter( t ->
			{
				return spaces.inputIsSubspace( t, input ) && spaces.outputIsSubspace( t, output );
			}
		).collect( Collectors.toList());
	}

	public CoordinateTransform<?> buildTransformFromAxes( final CoordinateSystem from, final CoordinateSystem to )
	{
		final List<CoordinateTransform<?>> tList = new ArrayList<>();
		final String[] outputAxes = to.getAxisNames();

		for( final CoordinateTransform<?> t : getTransforms() )
		{
			if( spaces.outputMatchesAny(t, outputAxes))
				tList.add(t);
		}

		final StackedCoordinateTransform totalTransform = new StackedCoordinateTransform(
				from.getName() + " > " + to.getName(), from.getName(), to.getName(), tList);

		totalTransform.setSpaces(spaces);
		totalTransform.buildTransform();

		return totalTransform;
	}

	public CoordinateTransform<?> buildImpliedTransform( final CoordinateSystem from, final CoordinateSystem to )
	{
		final List<CoordinateTransform<?>> tList = new ArrayList<>();

		// order list
		for( final String outLabels : to.getAxisNames() )
		{
			 getTransforms().stream().filter( t -> {
					return spaces.outputHasAxis( t, outLabels );
				}).findAny().ifPresent( t -> tList.add( t ));
		}

		final StackedCoordinateTransform totalTransform = new StackedCoordinateTransform(
				"name", from.getName(), to.getName(), tList);

		return totalTransform;
	}


	public Optional<TransformPath> pathFromAxes(final String from, final String[] toAxes ) {
		return path( spaces.spaceFrom(from), spaces.spaceFrom(toAxes));
	}

	public Optional<TransformPath> pathFromAxes(final String[] fromAxes, final String to ) {
		return path( spaces.spaceFrom(fromAxes), spaces.spaceFrom(to));
	}

	public Optional<TransformPath> pathFromAxes(final String[] fromAxes, final String[] toAxes ) {
		return path( spaces.spaceFrom(fromAxes), spaces.spaceFrom(toAxes));
	}

	public Optional<TransformPath> path(final String from, final String to ) {
		return path( spaces.spaceFrom(from), spaces.spaceFrom(to));
	}

	public Optional<TransformPath> path(final CoordinateSystem from, final CoordinateSystem to ) {

		if( from == null || to == null )
			return Optional.empty();
		else if( from.equals(to))
			return Optional.of( new TransformPath(
					new IdentityCoordinateTransform("identity", from.getName(), to.getName())));

		return allPaths( from ).stream().filter( p -> spaces.getSpace(p.getEnd()).equals(to)).findAny();
	}

	public List<TransformPath> paths(final CoordinateSystem from, final CoordinateSystem to ) {

		return allPaths( from ).stream().filter( p -> p.getEnd().equals(to)).collect(Collectors.toList());
	}

	public List<TransformPath> allPaths(final String from) {
		return allPaths(spaces.getSpace(from));
	}

	public List<TransformPath> allPaths(final CoordinateSystem from) {

		final ArrayList<TransformPath> paths = new ArrayList<TransformPath>();
		allPathsHelper( paths, from, null );
		return paths;
	}

	private void allPathsHelper(final List<TransformPath> paths, final CoordinateSystem start, final TransformPath pathToStart) {

		final CTNode node = spacesToNodes.get(start);

		List<CoordinateTransform<?>> edges = null;
		if( node != null )
			edges = spacesToNodes.get(start).edges();

		if( edges == null || edges.size() == 0 )
			return;

		for (final CoordinateTransform<?> t : edges) {
			final CoordinateSystem end = getOutput( t );

			if( pathToStart != null && pathToStart.hasSpace( end ))
				continue;

			final TransformPath p;
			if (pathToStart == null )
				p = new TransformPath( t );
			else
				p = new TransformPath( pathToStart, t );

			paths.add(p);
			allPathsHelper(paths, end, p);
		}
	}

}

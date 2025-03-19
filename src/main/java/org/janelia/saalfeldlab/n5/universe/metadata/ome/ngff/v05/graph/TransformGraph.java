package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.SpacesTransforms;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.AbstractCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.InvertibleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.ByDimensionCoordinateTransform;

import com.google.gson.Gson;

import net.imglib2.realtransform.InvertibleRealTransform;

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
		inferSpacesFromAxes();

		spacesToNodes = new HashMap< CoordinateSystem, CTNode >();
		for( final CoordinateTransform<?> t : transforms )
			addTransform(t);

		updateTransforms();
	}

	public TransformGraph( final List< CoordinateTransform<?> > transforms, final List<CoordinateSystem> spacesIn ) {
		this( transforms, new CoordinateSystems(spacesIn) );
	}

	protected void inferSpacesFromAxes()
	{
		for( final CoordinateTransform<?> ct : transforms )
		{
			if( ct instanceof AbstractCoordinateTransform<?> )
				if( ! ((AbstractCoordinateTransform<?>)ct).inferSpacesFromAxes(spaces))
				{
					System.out.println( "uh oh - removing " + ct );
					transforms.remove(ct);
				}
		}
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
		return spaces.getSpace(t.getInput());
	}

	public CoordinateSystem getOutput( final CoordinateTransform<?> t ) {
		return spaces.getSpace(t.getOutput());
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

		if( spaces.hasSpace(t.getInput()) && spaces.hasSpace(t.getOutput()))
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

		if( addInverse && t instanceof InvertibleCoordinateTransform )
			addTransform( new InverseCT( (InvertibleCoordinateTransform) t ), false );
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
		final HashSet<String> outAxes = new HashSet<>();
		final HashSet<String> inAxes = new HashSet<>();
		final String[] outputAxes = to.getAxisNames();

		for( final CoordinateTransform<?> t : getTransforms() )
		{
			// if
			if( spaces.outputMatchesAny(t, outputAxes))
			{
				if( AxisUtils.containsAny( outAxes, spaces.getOutputAxes(t) ))
				{
					System.err.println( "warning: multiple transforms define same output axes");
					return null;
				}

				if( AxisUtils.containsAny( inAxes, spaces.getInputAxes(t) ))
				{
					System.err.println( "warning: multiple transforms define same output axes");
					return null;
				}

				tList.add(t);
				outAxes.addAll( Arrays.asList(t.getOutputAxes()) );
				inAxes.addAll( Arrays.asList(t.getInputAxes()) );
			}

		}

		CoordinateTransform<?>[] tforms = tList.stream().toArray( n -> new CoordinateTransform[n]);
		final ByDimensionCoordinateTransform totalTransform = new ByDimensionCoordinateTransform(
				from.getName() + " > " + to.getName(), from, to, tforms);

		totalTransform.setInput(from);
		totalTransform.setOutput(to);
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

		CoordinateTransform<?>[] tforms = tList.stream().toArray( n -> new CoordinateTransform[n]);
		final ByDimensionCoordinateTransform totalTransform = new ByDimensionCoordinateTransform(
				from.getName() + " > " + to.getName(), from, to, tforms);

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

	private static class InverseCT extends AbstractCoordinateTransform<InvertibleRealTransform>
		implements InvertibleCoordinateTransform<InvertibleRealTransform> {

		InvertibleCoordinateTransform<?> ict;

		public InverseCT( final InvertibleCoordinateTransform<?> ict ) {
			super("invWrap", "inv-" + ict.getName(), ict.getOutput(), ict.getInput());
			this.ict = ict;
		}

		public InverseCT(final String type, final String name, final String inputSpace, final String outputSpace,
				final InvertibleCoordinateTransform<?> ict ) {
			super(type, name, inputSpace, outputSpace);
			this.ict = ict;
		}

		@Override
		public InvertibleRealTransform getTransform() {
			return ict.getInvertibleTransform();
		}

		@Override
		public InvertibleRealTransform getTransform( final N5Reader n5 ) {
			return ict.getInvertibleTransform( n5 );
		}

		@Override
		public InvertibleRealTransform getInvertibleTransform() {
			return ict.getTransform();
		}

		@Override
		public InvertibleRealTransform getInvertibleTransform( final N5Reader n5 ) {
			return ict.getTransform( n5 );
		}
	}

}

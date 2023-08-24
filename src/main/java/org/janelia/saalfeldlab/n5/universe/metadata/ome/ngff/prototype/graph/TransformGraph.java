package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.AbstractCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.IdentityCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.InvertibleCoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.StackedCoordinateTransform;

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
//		this.transforms.addAll(transforms);
//		this.transforms = transforms = new ArrayList<>();;
		inferSpacesFromAxes();

		spacesToNodes = new HashMap< CoordinateSystem, CTNode >();
		for( final CoordinateTransform<?> t : transforms )
		{
			addTransform(t);

//			final Space src = getInputSpace( t );
//			if( spacesToNodes.containsKey( src ))
//				spacesToNodes.get( src ).edges().add( t );
//			else
//			{
//				SpaceNode node = new SpaceNode( src );
//				node.edges().add( t );
//				spacesToNodes.put( src, node );
//			}
		}
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

	public CoordinateSystems getSpaces() {
		return spaces;
	}

	public Optional<CoordinateTransform<?>> getTransform( final String name ) {
		return transforms.stream().filter( x -> x.getName().equals(name)).findAny();
	}

	public HashMap< CoordinateSystem, CTNode > getSpaceNodes() {
		return spacesToNodes;
	}

	public CoordinateSystem getInputSpace( final CoordinateTransform<?> t ) {
//		return namesToSpaces.get( t.getInputSpace());
		return spaces.getSpace(t.getInput());
	}

	public CoordinateSystem getOutputSpace( final CoordinateTransform<?> t ) {
//		return namesToSpaces.get( t.getOutputSpace());
		return spaces.getSpace(t.getOutput());
	}

	public void addTransform( final CoordinateTransform<?> t ) {
		addTransform( t, true );
	}

	private void addTransform( final CoordinateTransform<?> t, final boolean addInverse ) {
		if( transforms.stream().anyMatch( x -> x.getName().equals(t.getName())) )
			return;

		if( spaces.hasSpace(t.getInput()) && spaces.hasSpace(t.getOutput()))
		{
			final CoordinateSystem src = getInputSpace( t );
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
			System.out.println( "adding despite missing space: " + t );
//			spaces.makeDefault( t.getInputAxes() )
			transforms.add( t );
		}

		if( addInverse && t instanceof InvertibleCoordinateTransform )
			addTransform( new InverseCT( (InvertibleCoordinateTransform) t ), false );
	}

	public void updateTransforms()
	{
		getSpaces().updateTransforms( getTransforms().stream() );
	}

//	private void addInverse( InvertibleCoordinateTransform<?> ict )
//	{
//		addTransform( new InverseCT(ict));
//	}

	public void addSpace( final CoordinateSystem space )
	{
		if( spaces.add(space) ) {
			spacesToNodes.put( space, new CTNode(space));
		}
	}

	public void add( final TransformGraph g )
	{
		g.spaces.spaces().forEach( s -> addSpace(s));
		g.transforms.stream().forEach( t -> addTransform(t));
	}

	/**
	 * Returns all transforms that have all input axis in the from space
	 * and all output axis in their to space.
	 *
	 * @return
	 */
	public List<CoordinateTransform<?>> subTransforms( final CoordinateSystem from, final CoordinateSystem to) {
		return getTransforms().stream().filter( t ->
			{
				return spaces.inputIsSubspace( t, from ) && spaces.outputIsSubspace( t, to );
			}
		).collect( Collectors.toList());
	}

	public CoordinateTransform<?> buildTransformFromAxes( final CoordinateSystem from, final CoordinateSystem to )
	{
		final List<CoordinateTransform<?>> tList = new ArrayList<>();
		final HashSet<String> outAxes = new HashSet<>();
		final HashSet<String> inAxes = new HashSet<>();
		final String[] outputAxes = to.getAxisLabels();

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

		final StackedCoordinateTransform totalTransform = new StackedCoordinateTransform(
				from.getLabel() + " > " + to.getLabel(), from.getLabel(), to.getLabel(), tList);

		totalTransform.setSpaces(spaces);
		totalTransform.buildTransform();

		return totalTransform;
	}

	public CoordinateTransform<?> buildImpliedTransform( final CoordinateSystem from, final CoordinateSystem to )
	{
//		final List<CoordinateTransform<?>> tList = subTransforms( from, to );

		final List<CoordinateTransform<?>> tList = new ArrayList<>();

		// order list
		for( final String outLabels : to.getAxisLabels() )
		{
			 getTransforms().stream().filter( t -> {
					return spaces.outputHasAxis( t, outLabels );
				}).findAny().ifPresent( t -> tList.add( t ));
		}

		final StackedCoordinateTransform totalTransform = new StackedCoordinateTransform(
				"name", from.getLabel(), to.getLabel(), tList);

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
					new IdentityCoordinateTransform("identity", from.getLabel(), to.getLabel())));


//		return allPaths( from ).stream().filter( p -> p.getEnd().equals(to))
//				.reduce( (x,y) -> {
//					if( x.getCost() < y.getCost() )
//						return x;
//					else
//						return y;
//				});

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
			final CoordinateSystem end = getOutputSpace( t );

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
			super("invWrap", "inv-" + ict.getName(), ict.getOutput(), ict.getInputSpace());
			this.ict = ict;
		}

		public InverseCT(final String type, final String name, final String inputSpace, final String outputSpace,
				final InvertibleCoordinateTransform<?> ict ) {
			super(type, name, inputSpace, outputSpace);
			this.ict = ict;
		}

		@Override
		public InvertibleRealTransform getTransform() {
			return ict.getInverseTransform();
		}

		@Override
		public InvertibleRealTransform getTransform( final N5Reader n5 ) {
			return ict.getInverseTransform( n5 );
		}

		@Override
		public InvertibleRealTransform getInverseTransform() {
			return ict.getTransform();
		}

		@Override
		public InvertibleRealTransform getInverseTransform( final N5Reader n5 ) {
			return ict.getTransform( n5 );
		}
	}

}

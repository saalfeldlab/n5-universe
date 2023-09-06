package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.axes.ArrayCoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations.CoordinateTransformAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SpacesTransforms {

	public CoordinateSystem[] spaces;

	public CoordinateTransform<?>[] transforms;

	public SpacesTransforms( final CoordinateSystem[] spaces, final CoordinateTransform<?>[] transforms)
	{
		this.spaces = spaces;
		this.transforms = transforms;
	}

	public static Gson buildGson()
	{
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(null));
		final Gson gson = gsonBuilder.create();
		return gson;
	}

	public static SpacesTransforms load( final Reader reader )
	{
		final Gson gson = buildGson();
		final SpacesTransforms st = gson.fromJson(reader, SpacesTransforms.class );
		return st;
	}

	public static SpacesTransforms load( final File f ) throws FileNotFoundException {
		return load( new FileReader( f.getAbsolutePath() ));
	}

	public static SpacesTransforms loadFile( final String path ) throws FileNotFoundException {
		return load( new FileReader( path ));
	}

	public CoordinateSystems buildSpaces() {
		return buildSpaces( 0 );
	}

	public CoordinateSystems buildSpaces( final String name, final int nd ) {
		final CoordinateSystems s = new CoordinateSystems( spaces );
		if( nd > 0 )
			s.add(new ArrayCoordinateSystem(name, nd));

		return s;
	}

	public CoordinateSystems buildSpaces( final int nd ) {
		return buildSpaces( "", nd );
	}

	public TransformGraph buildTransformGraph() {
		return buildTransformGraph(0);
	}

	public TransformGraph buildTransformGraph( final int nd ) {
		return buildTransformGraph( "", nd );
	}

	public TransformGraph buildTransformGraph( final String dataset ) {
		return buildTransformGraph( dataset, 0);
	}

	public TransformGraph buildTransformGraph( final String dataset, final int nd ) {
		if( transforms == null )
			return new TransformGraph();
		else
			return new TransformGraph( Arrays.asList(transforms), buildSpaces( dataset, nd ));
	}

}

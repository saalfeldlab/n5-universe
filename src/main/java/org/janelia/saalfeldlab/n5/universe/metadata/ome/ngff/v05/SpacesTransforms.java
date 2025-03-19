package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.axes.ArrayCoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;

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

	public static boolean synchronize( CoordinateTransform[] coordinateTransformations, CoordinateSystem[] coordinateSystems ) {

		return synchronize( coordinateTransformations, new CoordinateSystems(coordinateSystems));
	}

	public static boolean synchronize( CoordinateTransform[] coordinateTransformations, CoordinateSystems coordinateSystems ) {

		return Arrays.stream( coordinateTransformations ).allMatch( t -> {
			return synchronize( t, coordinateSystems );
		});
	}
	
	public static boolean synchronize( CoordinateTransform ct, CoordinateSystem[] coordinateSystems ) {

		return synchronize( ct, new CoordinateSystems(coordinateSystems));
	}

	public static boolean synchronize( final CoordinateTransform ct, final CoordinateSystems coordinateSystems ) {

		final CoordinateSystem inputCs = coordinateSystems.getSpace( ct.getInput() );
		final CoordinateSystem outputCs = coordinateSystems.getSpace( ct.getOutput() );

		boolean success = true;
		if( inputCs == null )
			success = false;
		else
			ct.setInput( inputCs );

		if( outputCs == null )
			success = false;
		else
			ct.setOutput( outputCs );

		return success;
	}
	
	public void serialize(N5Writer n5, final String dataset) {
		serialize(n5, dataset, "ome/");
	}

	public void serialize(N5Writer n5, final String dataset, final String attributePrefix ) {

		final CoordinateSystem[] reversedCss = Arrays.stream(spaces).map(x -> {
			return x.reverseAxes();
		}).toArray(N -> new CoordinateSystem[N]);

		n5.setAttribute( dataset, attributePrefix + "/" + CoordinateTransform.KEY, transforms );
		n5.setAttribute( dataset, attributePrefix + "/" + CoordinateSystem.KEY, reversedCss );
	}

	public static SpacesTransforms deserialize(N5Reader n5, final String dataset) {
		return deserialize(n5, dataset, "ome/");
	}

	public static SpacesTransforms deserialize(N5Reader n5, final String dataset, final String attributePrefix ) {

		final CoordinateSystem[] reversedCss = n5.getAttribute( dataset, attributePrefix+"/"+"coordinateSystems", CoordinateSystem[].class);
		final CoordinateSystem[] css = Arrays.stream(reversedCss).map(x -> {
			return x.reverseAxes();
		}).toArray(N -> new CoordinateSystem[N]);

		CoordinateTransform<?>[] transforms = n5.getAttribute(dataset, attributePrefix+"/"+"coordinateTransformations", CoordinateTransform[].class);
		return new SpacesTransforms(css, transforms);
	}

}

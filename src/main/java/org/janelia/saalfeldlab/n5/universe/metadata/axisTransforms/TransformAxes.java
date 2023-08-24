package org.janelia.saalfeldlab.n5.universe.metadata.axisTransforms;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import net.imglib2.realtransform.RealTransform;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.IndexedAxis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.IndexedAxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.IdentitySpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.ScaleSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;

/**
 * A transformation whose axes are labeled.
 * The number of input and output dimensions must be the same.
 *
 * @author John Bogovic
 */
public class TransformAxes implements SpatialTransform, IndexedAxisMetadata {

	private SpatialTransform transform;

	private IndexedAxis[] inputAxes;

	private IndexedAxis[] outputAxes;

	public TransformAxes( final SpatialTransform transform,
			final IndexedAxis[] inputAxes,
			final IndexedAxis[] outputAxes ) {

		this.transform = transform;
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
	}

	public TransformAxes( final SpatialTransform transform, final String... labels ) {
		this( transform, IndexedAxis.dataAxes( labels.length ), IndexedAxis.axesFromLabels( labels ));
	}

	public TransformAxes( final int[] indexes, final String... labels ) {
		this( new IdentitySpatialTransform(),
				IndexedAxis.dataAxes( labels.length ),
				IndexedAxis.axesFromLabels( labels, indexes, "none" ));
	}

	public TransformAxes( final String... labels ) {
		this( new IdentitySpatialTransform(),
				IndexedAxis.dataAxes( labels.length ),
				IndexedAxis.axesFromLabels( labels, "none" ));
	}

	protected void setDefaults( final int firstIndex ) {
		int nd = -1;
		if( transform != null )
			nd = transform.getTransform().numSourceDimensions();

		// ugly - Identity transformations can return 0 dimension
		// (since they can apply to points of any size)
		if( nd < 1 && outputAxes != null )
			nd = outputAxes.length;
		else if( nd < 1 && inputAxes != null )
			nd = inputAxes.length;

		// set default values
		if (this.transform == null)
			this.transform = new IdentitySpatialTransform();

		if (this.inputAxes == null)
			this.inputAxes = IndexedAxis.dataAxes(nd, firstIndex);

		if (this.outputAxes == null)
			this.outputAxes = IndexedAxis.defaultAxes(nd, firstIndex);

		int i = firstIndex;
		for (final IndexedAxis ia : inputAxes)
			ia.setDefaults(true, i++);

		i = firstIndex;
		for (final IndexedAxis oa : outputAxes)
			oa.setDefaults(false, i++);
	}

	public IndexedAxis[] getInputAxes() {
		return inputAxes;
	}

	public IndexedAxis[] getOutputAxes() {
		return outputAxes;
	}

	@Override
	public Axis[] getAxes() {

		return getOutputAxes();
	}

	@Override
	public int[] getIndexes() {
		return Arrays.stream(outputAxes).mapToInt(IndexedAxis::getIndex).toArray();
	}

	@Override
	public RealTransform getTransform() {
		return transform.getTransform();
	}

	public static void main( final String[] args ) throws JsonSyntaxException, JsonIOException, FileNotFoundException {

		final Gson gson = JqUtils.buildGson(null);
		final ScaleSpatialTransform xfm = new ScaleSpatialTransform( new double[] {2, 3, 4});

////		IndexedAxis[] inputs = dataAxes( new int[]{0,1,2});
////		IndexedAxis[] inputs = dataAxes( 0, 2 );
//		IndexedAxis[] inputs = dataAxes( 3 );
//		IndexedAxis[] outputs = axesFromLabels( "x", "y", "z" );
//
//		System.out.println( gson.toJson(inputs));
//		System.out.println( "" );
//		System.out.println( gson.toJson(outputs));

//		TransformAxesMetadata xfmAxis = new TransformAxes( xfm, "z","y","c");
//		System.out.println( gson.toJson( xfmAxis ));

		final File f = new File("/home/john/dev/n5/n5-imglib2-translation/src/test/resources/transforms/transformsWithAxes.json");
		final TransformAxes tam = gson.fromJson( new FileReader( f ), TransformAxes.class );
		System.out.println( tam );

	}

}

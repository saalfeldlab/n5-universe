package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;

public class MapAxisCoordinateTransform extends AbstractCoordinateTransform<AffineGet> 
	implements InvertibleCoordinateTransform<AffineGet> {

	public static final String TYPE = "mapAxis";

	protected int[] mapAxis;

	protected transient String[] inputAxes;
	protected transient String[] outputAxes;

	protected MapAxisCoordinateTransform() {
		super(TYPE);
	}

	public MapAxisCoordinateTransform(MapAxisCoordinateTransform ct) {
		super(ct);
	}

	public MapAxisCoordinateTransform( final String name,
			final CoordinateSystem input, final CoordinateSystem output,
			final int[] axisMapping) {
		super(TYPE, name, input, output);

		inputAxes = input.getAxisNames();
		outputAxes = output.getAxisNames();
		this.mapAxis = axisMapping;

		validate(axisMapping, inputAxes, outputAxes);
	}

	public MapAxisCoordinateTransform(final CoordinateSystem input, CoordinateSystem output,
			final int[] axisMapping) {
		this(null, input, output, axisMapping);
	}

	public int[] getAxisMapping() {
		return mapAxis;
	}

	public void setInputAxes(final String[] inputAxes) {
		this.inputAxes = inputAxes;
	}

	public void setOutputAxes(final String[] outputAxes) {
		this.outputAxes = outputAxes;
	}
	
	public void setInput(final CoordinateSystem inputCoordinateSystem) {
		super.setInput(inputCoordinateSystem);
		setInputAxes(inputCoordinateSystem.getAxisNames());
	}

	public void setOutput(final CoordinateSystem outputCoordinateSystem) {
		super.setOutput(outputCoordinateSystem);
		setOutputAxes(outputCoordinateSystem.getAxisNames());
	}

	@Override
	public AffineGet getTransform() {

		if (inputAxes == null || outputAxes == null)
			return null;

		return realTransformFromMapping(mapAxis, inputAxes, outputAxes);
	}

	private static AffineGet realTransformFromMapping(final int[] axisMapping,
			final String[] inputAxes, final String[] outputAxes) {

		if (inputAxes.length != outputAxes.length) {
			// TODO support the general case
			throw new N5Exception("A continuous non-invertible transform from a mapAxis is not yet supported.");
		}

		int nd = inputAxes.length;
		final double[][] affine = new double[nd][nd+1];
		for (int i = 0; i < nd; i++ ) {
			affine[i][axisMapping[i]] = 1;
		}

		return new AffineTransform(TransformUtils.flatten(affine));
	}

	private static void validate(final AffineGet affine, final double eps) {

		// imglib2 AffineGet always ave numSourceDimensions == numTargetDimensions
		int nd = affine.numSourceDimensions();
		for( int i = 0; i < nd; i++)
			for( int j = 0; j < nd; j++) {
				double val = affine.get(i, j);
				if( Math.abs(1 - val) > eps && Math.abs(val) > eps ) {
					throw new IllegalArgumentException(
							String.format("Matrix representation for mapAxis must be a permutation matrix"));
				}
			}
	}
	
	public static void validate(int[] axisMapping, String[] inputAxes, String[] outputAxes) {
		
		final int numOutput = outputAxes.length;
		final boolean[] outputExists = new boolean[numOutput];

		if (axisMapping.length != outputAxes.length)
			throw new N5Exception("The length of mapAxis (" + axisMapping.length
					+ ") must equal the number of output axes (" + outputAxes.length + ")");
		
		for( int i = 0; i < axisMapping.length; i++ ) {
			
			if( axisMapping[i] < 0 )
				throw new N5Exception("The entry at [" + i + "] is less than zero.");
			else if (axisMapping[i] >= outputAxes.length)
				throw new N5Exception("The entry at [" + i + "] " + axisMapping[i] + " > " + outputAxes.length + " (the number of output axes).");

		}

	}

}

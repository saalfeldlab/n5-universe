package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.codec.transpose.TransposeCodec;
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

	private MapAxisCoordinateTransform(final Options options) {
		super(TYPE, options);
		this.mapAxis = options.axisMapping == null ? null : options.axisMapping.clone();

		// the mapping can be checked against the coordinate systems, if they were given
		if (inputCoordinateSystem != null && outputCoordinateSystem != null) {
			inputAxes = inputCoordinateSystem.getAxisNames();
			outputAxes = outputCoordinateSystem.getAxisNames();
			validate(mapAxis, inputAxes, outputAxes);
		}
	}

	public static class Options extends TransformOptions<Options> {

		private int[] axisMapping;

		public Options axisMapping(final int[] axisMapping) {
			this.axisMapping = axisMapping;
			return this;
		}

		public MapAxisCoordinateTransform build() {
			return new MapAxisCoordinateTransform(this);
		}
	}

	public static Options options() {
		return new Options();
	}

	protected MapAxisCoordinateTransform(MapAxisCoordinateTransform other, TransformOptions<?> copyParams) {
		super(other, copyParams);
		this.mapAxis = other.mapAxis == null ? null : other.mapAxis.clone();
		this.inputAxes = other.inputAxes == null ? null : other.inputAxes.clone();
		this.outputAxes = other.outputAxes == null ? null : other.outputAxes.clone();
	}

	@Override
	public MapAxisCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public MapAxisCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new MapAxisCoordinateTransform(this, copyParams);
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

	public static void reverseParameters(MapAxisCoordinateTransform ct) {
		ct.mapAxis = TransposeCodec.conjugateWithReverse(ct.mapAxis);
	}
}

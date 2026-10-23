package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

/**
 * A transformation that applies to a subset of input / output dimensions.
 * <p>
 * Used by {@link ByDimensionCoordinateTransform} and
 * {@link StackedCoordinateTransform}.
 */
public class SubAxisTransformation {

	private CoordinateTransform<?> transformation;

	private int[] inputAxes;

	private int[] outputAxes;

	// for serialization
	protected SubAxisTransformation() {}

	public SubAxisTransformation(final CoordinateTransform<?> transformation, final int[] inputAxes, final int[] outputAxes) {
		this.transformation = transformation;
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
	}

	/**
	 * @return a copy of this, with a copy of its transformation
	 */
	public SubAxisTransformation copy() {
		return new SubAxisTransformation(
				transformation == null ? null : transformation.copy(),
				inputAxes == null ? null : inputAxes.clone(),
				outputAxes == null ? null : outputAxes.clone());
	}

	public CoordinateTransform<?> getTransformation() {
		return transformation;
	}

	public int[] getInputAxes() {
		return inputAxes;
	}

	public int[] getOutputAxes() {
		return outputAxes;
	}
}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealComponentMappingTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.StackedRealTransform;

/**
 * TODO doc this
 */
public class ByDimensionCoordinateTransform extends AbstractCoordinateTransform<RealTransform> {

	public static final String TYPE = "byDimension";

	private SubAxisTransformation[] transformations;

	private transient CoordinateTransform<?>[] transforms;

	private transient RealTransform totalTransform;

	// for serialization
	protected ByDimensionCoordinateTransform() {
		super(TYPE);
		transformations = null;
	}

	private ByDimensionCoordinateTransform(final ByDimensionOptions options) {
		super(TYPE, options);
		this.transformations = options.transformations == null
				? new SubAxisTransformation[0]
				: options.transformations.clone();
		setTransformsAfterDeserialization();

		// the entries can be checked against the coordinate systems, if they were given
		if (inputCoordinateSystem != null && outputCoordinateSystem != null)
			validate(transformations, inputCoordinateSystem.numDimensions(), outputCoordinateSystem.numDimensions());
	}

	public static class ByDimensionOptions extends TransformOptions<ByDimensionOptions> {

		private SubAxisTransformation[] transformations;

		public ByDimensionOptions withTransformations(final SubAxisTransformation... transformations) {
			this.transformations = transformations;
			return this;
		}

		public ByDimensionCoordinateTransform build() {
			return new ByDimensionCoordinateTransform(this);
		}
	}

	public static ByDimensionOptions options() {
		return new ByDimensionOptions();
	}

	protected ByDimensionCoordinateTransform( final ByDimensionCoordinateTransform other, final TransformOptions<?> copyParams ) {
		super(other, copyParams);
		if (other.transformations != null) {
			this.transformations = Arrays.stream(other.transformations)
					.map(SubAxisTransformation::copy)
					.toArray(SubAxisTransformation[]::new);
			setTransformsAfterDeserialization();
		}
	}

	@Override
	public ByDimensionCoordinateTransform copy() {
		return copy(new BasicOptions());
	}

	@Override
	public ByDimensionCoordinateTransform copy( final TransformOptions<?> copyParams ) {
		return new ByDimensionCoordinateTransform(this, copyParams);
	}

	/**
	 * @param n5 the reader
	 * @return the transform
	 * @deprecated use {@link #buildTransform()}, see {@link CoordinateTransform#getTransform(N5Reader)}
	 */
	@Deprecated
	public RealTransform buildTransform( final N5Reader n5 ) {

		ParameterBinding.bindUnbound(this, n5);
		return buildTransform();
	}

	public RealTransform buildTransform() {

		final RealTransform[] transformArray = Arrays.stream(transformations)
				.map(ByDimensionCoordinateTransform::buildChild)
				.toArray(RealTransform[]::new);

	    final StackedRealTransform stackedTransform = new StackedRealTransform(transformArray);

	    // the axis routing is intrinsic to the sub-transforms' declared axes, so it does not
	    // depend on the input/output coordinate systems being resolved. deriving the
	    // dimensionality from the axes lets this transform build standalone (e.g. opened
	    // directly by bigwarp), not only when resolved through the scene graph.
	    final RealTransform preTransform = createPreTransform();
	    final RealTransform postTransform = createPostTransform();

	    totalTransform = combineTransforms(stackedTransform, preTransform, postTransform);
	    return totalTransform;
	}

	private RealTransform createPreTransform() {

		final int[] transformInputAxes = inputAxesFromTransforms();
		final int n = transformInputAxes.length;

		for (int i = 0; i < n; i++) {
			if (transformInputAxes[i] != i)
				// transformInputAxes[i] is the overall input axis index for stacked position i
				return new RealComponentMappingTransform(n, transformInputAxes);
		}
		return null;
	}

	private RealTransform createPostTransform() {

		final int[] transformOutputAxes = outputAxesFromTransforms();
		final int n = transformOutputAxes.length;

		// build inverse permutation: invPerm[outputAxis] = stackedPosition
		final int[] invPerm = new int[n];
		boolean isNatural = true;
		for (int i = 0; i < n; i++) {
			invPerm[transformOutputAxes[i]] = i;
			if (transformOutputAxes[i] != i)
				isNatural = false;
		}
		if (isNatural)
			return null;
		return new RealComponentMappingTransform(n, invPerm);
	}

	private RealTransform combineTransforms(RealTransform stacked, RealTransform pre, RealTransform post) {

	    if (pre == null && post == null) {
	        return stacked;
	    }

	    RealTransformSequence sequence = new RealTransformSequence();
		if (pre != null)
			sequence.add(pre);

		sequence.add(stacked);

		if (post != null)
			sequence.add(post);

	    return sequence;
	}

	// an identity has no dimensionality of its own, it takes the number of axes it is given here
	private static RealTransform buildChild(final SubAxisTransformation entry) {

		if (entry.getTransformation() instanceof IdentityCoordinateTransform)
			return ((IdentityCoordinateTransform)entry.getTransformation()).getTransform(entry.getInputAxes().length);

		return (RealTransform)entry.getTransformation().getTransform();
	}

	protected int[] inputAxesFromTransforms() {
		return Arrays.stream(transformations).flatMapToInt(e -> Arrays.stream(e.getInputAxes())).toArray();
	}

	protected int[] outputAxesFromTransforms() {
		return Arrays.stream(transformations).flatMapToInt(e -> Arrays.stream(e.getOutputAxes())).toArray();
	}

	@Override
	public RealTransform getTransform() {

	    if (totalTransform != null)
	        return totalTransform;

		buildTransform();
		return totalTransform;
	}

	public CoordinateTransform<?>[] getTransformations() {
		return transforms;
	}

	/**
	 * @return the transformations along with the axes each one applies to
	 */
	public SubAxisTransformation[] getEntries() {
		return transformations;
	}

	/**
	 * @return true if every transformation is invertible (may read parameters)
	 */
	public boolean isInvertible() {
		return inverse() != null;
	}

	/**
	 * @return the inverse, or null if any transformation is not invertible
	 */
	public ByDimensionCoordinateTransform inverse() {

		// the inverse of each transformation maps its output axes back to its input axes
		final SubAxisTransformation[] invEntries = new SubAxisTransformation[transformations.length];
		for (int i = 0; i < transformations.length; i++) {
			final CoordinateTransform<?> inv = CoordinateTransform.inverseOf(transformations[i].getTransformation());
			if (inv == null)
				return null;

			invEntries[i] = new SubAxisTransformation(inv, transformations[i].getOutputAxes(), transformations[i].getInputAxes());
		}

		return options()
				.withName(getName() == null ? null : getName() + "-inv")
				.withInput(getOutput())
				.withOutput(getInput())
				.withTransformations(invEntries)
				.build();
	}

	public boolean isAffine() {

		return Arrays.stream(transforms)
				.allMatch(x -> x.getTransform() instanceof AffineGet);
	}

	/**
	 * Sets this objects CoordinateTransformations from its internal
	 * deserialized entries.
	 *
	 * Generally not appropriate to call this manually.
	 */
	public void setTransformsAfterDeserialization() {

		transforms = new CoordinateTransform[transformations.length];
		for (int i = 0; i < transformations.length; i++)
			transforms[i] = transformations[i].getTransformation();
	}

	private static void validate(final SubAxisTransformation[] entries, final int numInput, final int numOutput) {

		final boolean[] outputExists = new boolean[numOutput];

		int ctIndex = 0;
		for (SubAxisTransformation subTransform : entries) {

			if (subTransform.getInputAxes() == null)
				throw new N5Exception("Coordinate transform at index " + ctIndex + " does not declare input axes");

			if (subTransform.getOutputAxes() == null)
				throw new N5Exception("Coordinate transform at index " + ctIndex + " does not declare output axes");

			for (final int ctIn : subTransform.getInputAxes())
				if (ctIn < 0 || ctIn >= numInput)
					throw new N5Exception("Coordinate transform at index " + ctIndex + " has input axis index " + ctIn
							+ " out of range [0, " + (numInput - 1) + "].");

			for (final int ctOut : subTransform.getOutputAxes()) {
				if (ctOut < 0 || ctOut >= numOutput)
					throw new N5Exception("Coordinate transform at index " + ctIndex + " has output axis index " + ctOut
							+ " out of range [0, " + (numOutput - 1) + "].");
				outputExists[ctOut] = true;
			}

			ctIndex++;
		}

		final ArrayList<Integer> outputsNotPresent = new ArrayList<>();
		for (int i = 0; i < numOutput; i++) {
			if (!outputExists[i])
				outputsNotPresent.add(i);
		}

		if (outputsNotPresent.size() > 0)
			throw new N5Exception("All output axes must be covered for byDimension.\n"
					+ "The axis indices\n[" + outputsNotPresent.stream().map(String::valueOf).collect(Collectors.joining(","))
					+ "]\nwere not present.");
	}

	public static void reverseParameters(SubAxisTransformation[] entries) {

		int inMax = -1;
		int outMax = -1;
		for( SubAxisTransformation e : entries) {
			inMax = updateMax( inMax, e.getInputAxes());
			outMax = updateMax( outMax, e.getOutputAxes());
		}

		for( SubAxisTransformation e : entries) {
			indexReversal(inMax, e.getInputAxes());
			indexReversal(outMax, e.getOutputAxes());
		}
	}

	private static int updateMax(int current, int[] values) {

		int max = current;
		for (int v : values)
			if (v > max)
				max = v;

		return max;
	}

	private static void indexReversal(int max, int[] indexes) {

		int N = indexes.length;
		int[] tmp = new int[indexes.length];
		for (int i = 0; i < indexes.length; i++) {
			tmp[N - i - 1] = max - indexes[i];
		}
		System.arraycopy(tmp, 0, indexes, 0, N);
	}

}

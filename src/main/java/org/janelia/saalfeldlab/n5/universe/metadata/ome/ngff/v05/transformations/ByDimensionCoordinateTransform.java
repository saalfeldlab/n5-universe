package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealComponentMappingTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.StackedRealTransform;

public class ByDimensionCoordinateTransform extends AbstractCoordinateTransform<RealTransform> {

	public static final String TYPE = "byDimension";

	private TransformWithIndexes[] transformations;

	private transient CoordinateTransform<?>[] transforms;

	private transient RealTransform totalTransform;

	// for serialization
	protected ByDimensionCoordinateTransform() {
		super(TYPE);
		transformations = null;
	}

	public ByDimensionCoordinateTransform(
			final String name,
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final CoordinateTransform<?>... transformations ) {
		super(TYPE, name, inputSpace, outputSpace);
		this.transforms = transformations;
		setTransformationsForSerialization();
		validate(transformations, inputSpace.numDimensions(), outputSpace.numDimensions());
	}

	public ByDimensionCoordinateTransform(
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final CoordinateTransform<?>... transformations ) {
		this(null, inputSpace, outputSpace, transformations);
	}

	public ByDimensionCoordinateTransform(
			final String name,
			final String inputSpace, final String outputSpace,
			final CoordinateTransform<?>... transformations ) {
		super(TYPE, name, inputSpace, outputSpace);
		this.transforms = transformations;
		setTransformationsForSerialization();
	}

	public ByDimensionCoordinateTransform(
			final String name,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef,
			final CoordinateTransform<?>... transformations ) {
		super(TYPE, name, inputRef, outputRef);
		this.transforms = transformations;
		setTransformationsForSerialization();
	}

	public RealTransform buildTransform()
	{
		return buildTransform( null );
	}

	public RealTransform buildTransform( final N5Reader n5 ) {

		final RealTransform[] transformArray = Arrays.stream(transforms)
				.map(x -> (RealTransform) x.getTransform(n5))
				.toArray(RealTransform[]::new);

	    final StackedRealTransform stackedTransform = new StackedRealTransform(transformArray);
	    final CoordinateSystem inputCs = getInputCoordinateSystem();
	    final CoordinateSystem outputCs = getOutputCoordinateSystem();

	    if (inputCs == null || outputCs == null) {
	        return null;
	    }

	    final RealTransform preTransform = createPreTransform(inputCs);
	    final RealTransform postTransform = createPostTransform(outputCs);

	    totalTransform = combineTransforms(stackedTransform, preTransform, postTransform);
	    return totalTransform;
	}

	private RealTransform createPreTransform(CoordinateSystem inputCoordinateSystem) {

		final int n = inputCoordinateSystem.numDimensions();
		final int[] transformInputAxes = inputAxesFromTransforms();

		for (int i = 0; i < n; i++) {
			if (transformInputAxes[i] != i)
				// transformInputAxes[i] is the overall input axis index for stacked position i
				return new RealComponentMappingTransform(n, transformInputAxes);
		}
		return null;
	}

	private RealTransform createPostTransform(CoordinateSystem outputCoordinateSystem) {

		final int n = outputCoordinateSystem.numDimensions();
		final int[] transformOutputAxes = outputAxesFromTransforms();

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

	protected int[] inputAxesFromTransforms() {
		return Arrays.stream(transforms).flatMapToInt(t -> Arrays.stream(t.getInputAxes())).toArray();
	}

	protected int[] outputAxesFromTransforms() {
		return Arrays.stream(transforms).flatMapToInt(t -> Arrays.stream(t.getOutputAxes())).toArray();
	}

	@Override
	public RealTransform getTransform( N5Reader n5) {

	    if (totalTransform != null)
	        return totalTransform;

		buildTransform(n5);
		return totalTransform;
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

	public boolean isInvertible() {
		return Arrays.stream(transformations).allMatch(t -> t instanceof InvertibleCoordinateTransform);
	}

	public ByDimensionCoordinateTransform inverse() {
		if (!isInvertible())
			return null;

		final CoordinateTransform<?>[] invTransforms = Arrays.stream(transforms)
				.map(ct -> new InverseCoordinateTransform((InvertibleCoordinateTransform<?>) ct))
				.toArray(CoordinateTransform<?>[]::new);

		return new ByDimensionCoordinateTransform(
				getName() == null ? null : getName() + "-inv",
				getOutput(), getInput(),
				invTransforms);
	}

	public boolean isAffine() {

		return Arrays.stream(transforms)
				.map(CoordinateTransform::create)
				.allMatch(x -> x.getTransform() instanceof AffineGet);
	}

	/**
	 * Sets this objects CoordinateTransformations from its internal
	 * deserialized TransformWithIndexes.
	 * 
	 * Generally not appropriate to call this manually.
	 */
	public void setTransformsAfterDeserialization() {

		transforms = new CoordinateTransform[transformations.length];
		for (int i = 0; i < transformations.length; i++) {

			TransformWithIndexes ti = transformations[i];
			CoordinateTransform<?> t = ti.transformation;
			t.setInputAxes(ti.input_axes);
			t.setOutputAxes(ti.output_axes);
			transforms[i] = t;
		}
	}

	private void setTransformationsForSerialization() {

		transformations = new TransformWithIndexes[transforms.length];
		for (int i = 0; i < transforms.length; i++) {

			TransformWithIndexes ti = new TransformWithIndexes();
			CoordinateTransform<?> t = transforms[i];

			ti.transformation = t;
			ti.input_axes = t.getInputAxes();
			ti.output_axes = t.getOutputAxes();
			transformations[i] = new TransformWithIndexes();
		}

	}

	private static void validate(final CoordinateTransform<?>[] transforms, final int numInput, final int numOutput) {

		final boolean[] outputExists = new boolean[numOutput];

		int ctIndex = 0;
		for (CoordinateTransform<?> ct : transforms) {

			if (ct.getInputAxes() == null)
				throw new N5Exception("Coordinate transform at index " + ctIndex + " does not declare input axes");

			if (ct.getOutputAxes() == null)
				throw new N5Exception("Coordinate transform at index " + ctIndex + " does not declare output axes");

			for (final int ctIn : ct.getInputAxes())
				if (ctIn < 0 || ctIn >= numInput)
					throw new N5Exception("Coordinate transform at index " + ctIndex + " has input axis index " + ctIn
							+ " out of range [0, " + (numInput - 1) + "].");

			for (final int ctOut : ct.getOutputAxes()) {
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
	
	public static void reverseParameters(CoordinateTransform<?>[] transforms) { 

		int inMax = -1;
		int outMax = -1;
		for( CoordinateTransform<?> t : transforms) {
			inMax = updateMax( inMax, t.getInputAxes());
			outMax = updateMax( outMax, t.getOutputAxes());
		}

		for( CoordinateTransform<?> t : transforms) {
			indexReversal(inMax, t.getInputAxes());
			indexReversal(outMax, t.getOutputAxes());
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
	
	private static class TransformWithIndexes {
		
		CoordinateTransform<?> transformation;
		int[] input_axes;
		int[] output_axes;
	}

}

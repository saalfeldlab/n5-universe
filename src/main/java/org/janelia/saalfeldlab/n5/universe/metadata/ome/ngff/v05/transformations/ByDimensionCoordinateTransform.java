package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealComponentMappingTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.StackedRealTransform;

@NameConfig.Name("byDimension")
public class ByDimensionCoordinateTransform extends AbstractCoordinateTransform<RealTransform> {

	public static final String TYPE = "byDimension";

	@NameConfig.Parameter
	private final CoordinateTransform<?>[] transformations;

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
		this.transformations = transformations;
		validate(transformations, inputSpace.getAxisNames(), outputSpace.getAxisNames());
	}

	public ByDimensionCoordinateTransform(
			final CoordinateSystem inputSpace, final CoordinateSystem outputSpace,
			final CoordinateTransform<?>... transformations ) {
		this(null, inputSpace, outputSpace, transformations);
	}

	public RealTransform buildTransform()
	{
		return buildTransform( null );
	}

	public RealTransform buildTransform( final N5Reader n5 ) {

		final RealTransform[] transformArray = Arrays.stream(transformations)
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

		String[] inputAxisLabels = inputCoordinateSystem.getAxisNames();
	    String[] transformInputAxisLabels = inputAxesFromTransforms();
	    
		if (Arrays.equals(inputAxisLabels, transformInputAxisLabels)) {
			return null;
		}

		final int[] inPermParams = AxisUtils.findPermutation(inputAxisLabels, transformInputAxisLabels);
		return new RealComponentMappingTransform(inPermParams.length, inPermParams);
	}

	private RealTransform createPostTransform(CoordinateSystem outputCoordinateSystem) {

		String[] outputAxisLabels = outputCoordinateSystem.getAxisNames();
		String[] transformOutputAxisLabels = outputAxesFromTransforms();

		if (Arrays.equals(outputAxisLabels, transformOutputAxisLabels)) {
			return null;
		}

		int[] outPermParams = AxisUtils.findPermutation(transformOutputAxisLabels, outputAxisLabels);
		return new RealComponentMappingTransform(outPermParams.length, outPermParams);
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

	protected String[] inputAxesFromTransforms() {
		return Arrays.stream(transformations).flatMap(t -> Arrays.stream(t.getInputAxes())).toArray(String[]::new);
	}

	protected String[] outputAxesFromTransforms() {
		return Arrays.stream(transformations).flatMap(t -> Arrays.stream(t.getOutputAxes())).toArray(String[]::new);
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

	public boolean isAffine() {

		return Arrays.stream(transformations)
				.map(CoordinateTransform::create)
				.allMatch(x -> x.getTransform() instanceof AffineGet);
	}

	private static void validate(final CoordinateTransform<?>[] transforms, final String[] inputAxes, final String[] outputAxes) {

		final int numOutput = outputAxes.length;
		final boolean[] outputExists = new boolean[numOutput];

		int ctIndex = 0;
		for (CoordinateTransform<?> ct : transforms ) {

			if( ct.getInputAxes() == null )
				throw new N5Exception("Coordinate transform at index " +  ctIndex + " does not declare input axes");

			if( ct.getOutputAxes() == null )
				throw new N5Exception("Coordinate transform at index " +  ctIndex + " does not declare output axes");

			for (final String ctIn : ct.getInputAxes())
				if (!contains(ctIn, inputAxes))
					throw new N5Exception("Coordinate transform at index " + ctIndex + " has input name " + ctIn
							+ " that is not an input axis.");

			for (final String ctOut : ct.getOutputAxes()) {
				final int i = firstIndexOf(ctOut, outputAxes);
				if (i < 0)
					throw new N5Exception("Coordinate transform at index " + ctIndex + " has output name " + ctOut
							+ " that is not an output axis.");
				else
					outputExists[i] = true;
			}
	
			ctIndex++;
		}

		final ArrayList<String> outputsNotPresent = new ArrayList<>();
		for (int i = 0; i < numOutput; i++) {
			if (!outputExists[i])
				outputsNotPresent.add(outputAxes[i]);
		}

		if( outputsNotPresent.size() > 0 )
			throw new N5Exception("All output axes must be keys for byDimension.\n"
					+ "The axes\n[" + outputsNotPresent.stream().collect(Collectors.joining(","))
					+ "]\nwere not present.");
	}

	private static boolean contains(final String query, final String[] set) {
		return Arrays.stream(set).anyMatch(x -> x.equals(query));
	}
	
	private static int firstIndexOf(String query, String[] values) {
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(query))
				return i;
		}
		return -1;
	}

}

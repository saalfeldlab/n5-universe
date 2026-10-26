package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

public class SequenceCoordinateTransform extends AbstractCoordinateTransform<RealTransformSequence> {

	public static final String TYPE = "sequence";

	private final CoordinateTransform<?>[] transformations;

	protected SequenceCoordinateTransform() {
		super(TYPE);
		transformations = null;
	}

	public SequenceCoordinateTransform(
			final CoordinateTransform<?>[] transformations) {
		this(new SequenceOptions()
				.withName("")
				.withInput(OmeNgffReference.DUMMY)
				.withOutput(OmeNgffReference.DUMMY)
				.withTransformations(transformations));
	}

	public SequenceCoordinateTransform(final SequenceOptions options) {
		super(TYPE, options);
		// the transformations themselves are not copied, only the array that holds them
		this.transformations = options.transformations == null
				? new CoordinateTransform<?>[0]
				: options.transformations.clone();
	}

	protected SequenceCoordinateTransform(final SequenceCoordinateTransform other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		if (copyParams instanceof SequenceOptions && ((SequenceOptions) copyParams).isTransformationsSet()) {
			final CoordinateTransform<?>[] replacement = ((SequenceOptions) copyParams).getTransformations();
			this.transformations = replacement == null ? null : replacement.clone();
		} else if (other.transformations == null) {
			this.transformations = null;
		} else {
			this.transformations = new CoordinateTransform[other.transformations.length];
			for (int i = 0; i < transformations.length; i++)
				this.transformations[i] = other.transformations[i].copy();
		}
	}

	public static class SequenceOptions extends TransformOptions<SequenceOptions> {

		private boolean transformationsSet = false;
		private CoordinateTransform<?>[] transformations;

		public SequenceOptions withTransformations(final CoordinateTransform<?>... transformations) {
			this.transformations = transformations;
			this.transformationsSet = true;
			return this;
		}

		public SequenceOptions withTransformations(final List<? extends CoordinateTransform<?>> transformations) {
			return withTransformations(transformations == null ? null
					: transformations.toArray(new CoordinateTransform<?>[0]));
		}

		public boolean isTransformationsSet() {
			return transformationsSet;
		}

		public CoordinateTransform<?>[] getTransformations() {
			return transformations;
		}
	}

	public static SequenceOptions options() {
		return new SequenceOptions();
	}

	@Override
	public SequenceCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public SequenceCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new SequenceCoordinateTransform(this, copyParams);
	}

	@Override
	public RealTransformSequence getTransform()
	{
		// with permutations
//		final ArrayList< String[] > axOrders = axisOrdersForTransform( getTransformations(), getOutputSpaceObj().getAxisNames() );
//		for( final String[] axes : axOrders )
//			System.out.println(String.join(" ", axes ));

		final RealTransformSequence totalTransform = new RealTransformSequence();
		final CoordinateTransform<?>[] tforms = getTransformations();
		for( int i = 0; i < tforms.length; i++ )
			totalTransform.add( tforms[i].getTransform() );

		return totalTransform;
	}

	public boolean isAffine() {

		return Arrays.stream(transformations)
				.allMatch(x -> x.getTransform() instanceof AffineGet);
	}

	public AffineGet asAffine( int nd )
	{
		final AffineTransform affine = new AffineTransform( nd );
		for (final CoordinateTransform<?> t : transformations) {
			final RealTransform tform = t.getTransform();
			if (tform instanceof AffineGet)
				affine.preConcatenate((AffineGet)tform);
			else
				return null;
		}

		return affine;
	}

	public RealTransformSequence getTransformNoSubsets()
	{
		final RealTransformSequence transform = new RealTransformSequence();
		for( final CoordinateTransform<?> t : getTransformations() )
			transform.add( t.getTransform() );

		return transform;
	}

	public RealTransformSequence getTransform( final String[] axes )
	{
		return getTransform();
	}

	public CoordinateTransform<?>[] getTransformations() {
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
	public SequenceCoordinateTransform inverse() {

		final ArrayList<CoordinateTransform<?>> invTransforms = new ArrayList<CoordinateTransform<?>>();
		for( CoordinateTransform<?> ct : getTransformations()) {
			final CoordinateTransform<?> inv = CoordinateTransform.inverseOf(ct);
			if( inv == null )
				return null;
			invTransforms.add(inv);
		}

		// the inverse of a sequence needs to apply the inverse transformations in reverse order
		Collections.reverse(invTransforms);

		return new SequenceCoordinateTransform(options()
				.withName(getName() + "inv")
				.withInput(getOutput())
				.withOutput(getInput())
				.withTransformations(invTransforms));
	}

}

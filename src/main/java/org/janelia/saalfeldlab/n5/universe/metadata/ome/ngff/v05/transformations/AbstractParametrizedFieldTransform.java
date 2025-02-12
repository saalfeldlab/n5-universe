package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

public abstract class AbstractParametrizedFieldTransform<T extends RealTransform, S extends RealType<S>>
	extends AbstractParametrizedTransform<T,RealRandomAccessible<RealComposite<S>>> {

	public static final String NEAREST_INTERPOLATION = "nearest";
	public static final String LINEAR_INTERPOLATION = "linear";
	public static final String CUBIC_INTERPOLATION = "cubic";

	@NameConfig.Parameter
	protected final String interpolation;

	protected transient RealRandomAccessible<RealComposite<S>> field;

	protected transient int vectorAxisIndex;

	public AbstractParametrizedFieldTransform(final String type) {
		super(type);
		interpolation = "linear";
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String input, final String output) {
		this( type, name, null, LINEAR_INTERPOLATION, input, output);
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String parameterPath, final String interpolation,
			final String input, final String output) {
		super( type, name, parameterPath, input, output);
		this.interpolation = interpolation;
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String parameterPath, final String interpolation,
			final String[] inputAxes, final String[] outputAxes ) {
		super( type, name, parameterPath, inputAxes, outputAxes );
		this.interpolation = interpolation;
	}

	public int getVectorAxisIndex() {
		return vectorAxisIndex;
	}

	public String getInterpolation() {
		return interpolation;
	}

	public abstract int parseVectorAxisIndex( N5Reader n5 );

	public RealRandomAccessible<RealComposite<S>> getField() {
		return field;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RealRandomAccessible<RealComposite<S>> getParameters(final N5Reader n5) {

		final String path = getParameterPath();

		vectorAxisIndex = parseVectorAxisIndex( n5 );

		// TODO generalize
		if( vectorAxisIndex != 0 )
			throw new N5Exception("Vector axis is in index " + vectorAxisIndex + " but only index 0 currently supported" );

		final RandomAccessibleInterval<S> fieldRaw;
		try {
			fieldRaw = (RandomAccessibleInterval<S>)N5Utils.open(n5, path );
		} catch (final N5Exception e) {
			return null;
		}

		final RandomAccessibleInterval<S> fieldRev = reverseCoordinates(fieldRaw);
		final CompositeIntervalView< S, RealComposite< S > >  collapsedFirst =
				Views.collapseReal(
						Views.moveAxis(fieldRev, 0, fieldRev.numDimensions() - 1 ) );

		final RealRandomAccessible<RealComposite<S>> fieldInterp = Views.interpolate(
				Views.extendBorder(collapsedFirst),
				new NLinearInterpolatorFactory<>());

		CoordinateTransform<?> pixelToPhysicalCt = findPixelToPhysicalTransformStrict( n5, path, getInput());
		if( pixelToPhysicalCt == null ) {
			pixelToPhysicalCt = findPixelToPhysicalTransformCheckSelfRef( n5, path, getInput());
		}

		if( pixelToPhysicalCt == null ) {
			field = fieldInterp;
			return field;
		}

		AffineGet affine = null;
		if( pixelToPhysicalCt.getType().equals( SequenceCoordinateTransform.TYPE ))
		{
			final int nd = fieldRaw.numDimensions();
			final AffineGet affineTotal = ((SequenceCoordinateTransform) pixelToPhysicalCt).asAffine(nd);
			affine = Common.removeDimension(vectorAxisIndex, affineTotal);
		} else if( pixelToPhysicalCt instanceof LinearCoordinateTransform ) {
			// this can be optimized if necessary
			AffineGet affineTotal = ((LinearCoordinateTransform)pixelToPhysicalCt).getTransform();
			affine = Common.removeDimension(vectorAxisIndex, affineTotal);
		}

		if( affine != null ) {
			field = RealViews.affine(fieldInterp, affine);
			return field;
		}

		System.err.println("Warning: only affine pixel to physical transforms are currently supported");
		return null;

		// TODO should eventually support the general case below
		// but need to handle removing a dimension from an arbitrary transform.
		// not dealing with it now

//		final RealTransform tform = pixelToPhysicalCt.getTransform();
//		if( tform == null ) {
//			field = fieldInterp;
//			return field;
//		}
//		else if ( tform instanceof InvertibleRealTransform) {
//			field = RealViews.transform(fieldInterp, (InvertibleRealTransform)tform);
//			return field;
//		}
//		else
//		{
//			field = new RealTransformRealRandomAccessible< >( fieldInterp, tform );
//			return field;
//		}
//
//		RandomAccessible<V>[] rawfields;
//		int nv = 1;
//		if( vectorAxisIndex < 0 ) {
//			rawfields = new RandomAccessible[]{ fieldRaw };
//		}
//		else {
//			nv = (int)fieldRaw.dimension(getVectorAxisIndex());
//			rawfields = new RandomAccessible[nv];
//			for( int i = 0; i < nv; i++ )
//			{
//				rawfields[i] = Views.extendZero(Views.hyperSlice( fieldRaw, vectorAxisIndex, i));
//			}
//		}
//
//		fields = new RealRandomAccessible[ rawfields.length ];
//		for( int i = 0; i < nv; i++ ) {
//			if( ixfm == null )
//			{
//				fields[i] = Views.interpolate( rawfields[i], new NLinearInterpolatorFactory<>());
//			}
//			else {
//				fields[i] = new RealTransformRealRandomAccessible(
//						Views.interpolate( rawfields[i], new NLinearInterpolatorFactory<>()),
//						ixfm );
//			}
//		}
//		return field;
	}

	public static CoordinateTransform<?> findPixelToPhysicalTransformStrict(final N5Reader n5, final String group, final String output ) {

		final String normGrp = N5URI.normalizeGroupPath(group);
		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, "ome/"+CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		for (final CoordinateTransform<?> ct : transforms) {
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput());
			if (nrmInput.equals(normGrp) && ct.getOutput().equals(output) ) {
				return ct;
			}
		}
		return null;
	}

	/*
	 * Permute (reverse) the coordinates of the first dimension.
	 * 
	 * @param dfield the displacement field.
	 */
	public static <T extends RealType<T>> RandomAccessibleInterval<T> reverseCoordinates(
			final RandomAccessibleInterval<T> dfield) {

		final long numCoordinates = dfield.dimension(0);
		final int[] reversePermutation = IntStream.iterate((int) numCoordinates - 1, x -> x-1).limit(numCoordinates)
				.toArray();
		return Views.permuteCoordinates(dfield, reversePermutation, 0);
	}

	/**
	 * Returns the first coordinate transformation found at this group that is either not a {@link ParametrizedTransform},
	 * or is a ParametrizedTransform that does not reference this group.
	 *
	 * @param n5 n5 reader
	 * @param group group
	 * @param output the name of the output coordinate system
	 * @return a transformation or null
	 */
	public static CoordinateTransform<?> findPixelToPhysicalTransformCheckSelfRef(final N5Reader n5, final String group, final String output ) {
		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, "ome/" + CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		for (final CoordinateTransform<?> ct : transforms) {
			if( ct instanceof ParametrizedTransform )
			{
				@SuppressWarnings("rawtypes")
				final ParametrizedTransform pct = (ParametrizedTransform)ct;
				final String path = pct.getParameterPath();
				if( pct.getParameterPath().equals("."))
					continue;
				else if( N5URI.normalizeGroupPath(group).equals(N5URI.normalizeGroupPath(path)))
					continue;
				else
					return ct;
			}
			else
				return ct;
		}
		return null;
	}

	public static RealTransform findFieldTransformStrict(final N5Reader n5, final String group, final String output ) {

		final String normGrp = N5URI.normalizeGroupPath(group);
		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		for (final CoordinateTransform<?> ct : transforms) {
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput());
			if (nrmInput.equals(normGrp) && ct.getOutput().equals(output) ) {
				return ct.getTransform(n5);
			}
		}
		return null;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.Objects;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

public abstract class AbstractParametrizedFieldTransform<T extends RealTransform, S extends RealType<S>>
	extends AbstractParametrizedTransform<T,RealRandomAccessible<RealComposite<S>>> {

	public static final String NEAREST_INTERPOLATION = "nearest";
	public static final String LINEAR_INTERPOLATION = "linear";
	public static final String CUBIC_INTERPOLATION = "cubic";

	protected final String interpolation;

	protected transient RealRandomAccessible<RealComposite<S>> field;

	protected transient int vectorAxisIndex;

	public AbstractParametrizedFieldTransform(final String type) {
		super(type);
		this.interpolation = LINEAR_INTERPOLATION;
	}

	protected AbstractParametrizedFieldTransform(final String type, final AbstractFieldOptions<?> options) {
		super(type, options);
		this.interpolation = options.getInterpolation();
	}

	protected AbstractParametrizedFieldTransform(final AbstractParametrizedFieldTransform<T,S> other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		if (copyParams instanceof AbstractFieldOptions) {
			final AbstractFieldOptions<?> options = (AbstractFieldOptions<?>) copyParams;
			// the field was read with the interpolation of the original, so it can not be changed here
			if (options.isInterpolationSet() && !Objects.equals(other.interpolation, options.getInterpolation()))
				throw new UnsupportedOperationException("The interpolation of a field transform can not be changed when copying");
		}

		this.interpolation = other.interpolation;
		// the field is immutable, so it can be shared. Subclasses rebuild their transform from it.
		this.field = other.field;
		this.vectorAxisIndex = other.vectorAxisIndex;
	}

	/**
	 * The options shared by field transforms. The interpolation defaults to
	 * {@link AbstractParametrizedFieldTransform#LINEAR_INTERPOLATION linear}. Each
	 * transform type has a concrete subclass that builds that type.
	 *
	 * @param <O> the type of the concrete options class
	 */
	public static abstract class AbstractFieldOptions<O extends AbstractFieldOptions<O>> extends TransformOptions<O> {

		private boolean interpolationSet = false;
		private String interpolation = LINEAR_INTERPOLATION;

		public O interpolation(String interpolation) {
			this.interpolation = interpolation;
			this.interpolationSet = true;
			return self();
		}

		public boolean isInterpolationSet() {
			return interpolationSet;
		}

		public String getInterpolation() {
			return interpolation;
		}
	}

	public String getInterpolation() {
		return interpolation;
	}

	public abstract String getVectorAxisType();

	@Override
	protected RealRandomAccessible<RealComposite<S>> readParameters(final N5Reader n5, final String path, final boolean reverse) {

		return FieldTransformParsing.getParameters(n5, path, getVectorAxisType(), getInput(), getInterpolation(), reverse);
	}

	public RealRandomAccessible<RealComposite<S>> getField() {

		return field;
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public RealRandomAccessible<RealComposite<S>> getParameters(final N5Reader n5) {
//
//		final String group = getParameterPath();
//
//		vectorAxisIndex = parseVectorAxisIndex( n5 );
//
//		// We allow the transform's path may be the field array itself (self-referencing shortcut) or a
//		// multiscales group whose array is a child dataset (standard OME-Zarr multiscales).
//		// Resolve the array path and its pixel->physical transform together from the group's
//		// multiscales. The pixel->physical is matched by the field's input coordinate-system name 
//		final OmeNgffReference inputRef = getInput();
//		final String inputName = inputRef == null ? null : inputRef.getName();
//		final ResolvedField resolved = resolveFieldFromMultiscales( n5, group, inputName );
//		final String arrayPath = resolved != null ? resolved.arrayPath : group;
//
//		final RandomAccessibleInterval<S> fieldRaw;
//		try {
//			fieldRaw = (RandomAccessibleInterval<S>)N5Utils.open(n5, arrayPath );
//		} catch (final N5Exception e) {
//			return null;
//		}
//
//		// The vector components are stored reversed only for zarr, matching the
//		// reversal the writer applies; see OmeNgffMetadataParser.reverse.
//		// TODO this will need generalizing if vectorAxisIndex != 0
//		final RandomAccessibleInterval<S> fieldRev = OmeNgffMetadataParser.reverse(n5)
//				? reverseCoordinates(fieldRaw)
//				: fieldRaw;
//
//		final CompositeIntervalView< S, RealComposite< S > >  collapsedFirst =
//				Views.collapseReal(
//						Views.moveAxis(fieldRev, 0, fieldRev.numDimensions() - 1 ) );
//
//		final RealRandomAccessible<RealComposite<S>> fieldInterp = Views.interpolate(
//				Views.extendBorder(collapsedFirst), getInterpolator());
//
//		// pixel->physical: prefer the transform resolved from multiscales, then the flat attributes.
//		CoordinateTransform<?> pixelToPhysicalCt = resolved != null ? resolved.pixelToPhysical : null;
//		if( pixelToPhysicalCt == null ) {
//			pixelToPhysicalCt = findPixelToPhysicalTransformStrict( n5, group, inputName );
//		}
//		if( pixelToPhysicalCt == null ) {
//			pixelToPhysicalCt = findPixelToPhysicalTransformCheckSelfRef( n5, group, inputName );
//		}
//
//		if( pixelToPhysicalCt == null ) {
//			field = fieldInterp;
//			return field;
//		}
//
//		// TODO is this general enough?
//		final AffineGet storedAffine = TransformUtils.toAffine(pixelToPhysicalCt, fieldRev.numDimensions());
//		if( storedAffine == null )
//			throw new N5Exception("Warning: only invertible affine pixel to physical transforms are currently supported");
//
//		/*
//		 * Make sure to handle the case when the field's coordinate transforms
//		 * are both nD and (n+1)D. Since earlier versions of the spec had 
//		 * the coordinate transforms apply to the vector dimension.
//		 * Ignore that component of the transformation, if present. 
//		 */
//		final int spatialNd = fieldRev.numDimensions() - 1;
//		final AffineGet affine = storedAffine.numSourceDimensions() == fieldRev.numDimensions()
//				? Common.removeDimension(vectorAxisIndex, storedAffine)
//				: storedAffine;
//
//		// fail here rather than hand back an under-dimensioned field
//		if( affine.numSourceDimensions() != spatialNd )
//			throw new N5Exception(String.format(
//					"pixel to physical transform at \"%s\" is %dD; the field at \"%s\" is %dD (%dD once its vector axis is collapsed)",
//					group, storedAffine.numSourceDimensions(), arrayPath, fieldRev.numDimensions(), spatialNd));
//
//		return RealViews.affine(fieldInterp, affine);
//	}


}

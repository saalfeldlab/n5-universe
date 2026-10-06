package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

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

	public AbstractParametrizedFieldTransform( final String type, final String name,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef) {
		this( type, name, null, LINEAR_INTERPOLATION, inputRef, outputRef);
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String parameterPath, final String interpolation,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef) {
		super( type, name, parameterPath, inputRef, outputRef);
		this.interpolation = interpolation;
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String parameterPath, final String interpolation,
			final int[] inputAxes, final int[] outputAxes ) {
		super( type, name, parameterPath, inputAxes, outputAxes );
		this.interpolation = interpolation;
	}

	public String getInterpolation() {
		return interpolation;
	}

	public abstract String getVectorAxisType();

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

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.Optional;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.Common;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
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

	public int getVectorAxisIndex() {
		return vectorAxisIndex;
	}

	public abstract String getVectorAxisType();

	public int parseVectorAxisIndex( N5Reader n5 ) {

		try {

			// Prefer NGFF multiscales metadata (ome/multiscales); its coordinate
			// systems are already un-reversed (imglib2 order) by MultiscalesAdapter.
			CoordinateSystem[] spaces = readMultiscalesCoordinateSystems(n5, getParameterPath());

			if( spaces == null ) {
				// for backward compatibility check whether the coordinate sytems exist at  "ome/coordinateSystems" 
				// These were always stored reversed, so reverse each back to imglib2 order.
				final CoordinateSystem[] tmpSpaces = n5.getAttribute(getParameterPath(), "ome/" + CoordinateSystem.KEY, CoordinateSystem[].class);
				if( tmpSpaces != null && tmpSpaces.length > 0 ) {
					spaces = new CoordinateSystem[tmpSpaces.length];
					for (int i = 0; i < tmpSpaces.length; i++)
						spaces[i] = tmpSpaces[i].reverseAxes();
				}
			}

			if( spaces == null || spaces.length == 0)
				throw new N5Exception("No coordinate systems found at: " + getParameterPath());

			final String vectorAxisType = getVectorAxisType();
			final CoordinateSystem space = spaces[0];
			for( int i = 0; i < space.numDimensions(); i++ )
				if( space.getAxis(i).getType().equals(vectorAxisType))
					return i;

		} catch (final N5Exception e) { }

		throw new N5Exception("No displacement axis found at: " + getParameterPath());
	}

	public RealRandomAccessible<RealComposite<S>> getField() {

		return field;
	}

	@SuppressWarnings("rawtypes")
	private InterpolatorFactory getInterpolator() {

		switch(interpolation) {
		case LINEAR_INTERPOLATION:
			return new NLinearInterpolatorFactory();
		case NEAREST_INTERPOLATION:
			return new NearestNeighborInterpolatorFactory<>();
		}
		return new NLinearInterpolatorFactory();
	}

	@SuppressWarnings("unchecked")
	@Override
	public RealRandomAccessible<RealComposite<S>> getParameters(final N5Reader n5) {

		final String group = getParameterPath();

		vectorAxisIndex = parseVectorAxisIndex( n5 );

		// We allow the transform's path may be the field array itself (self-referencing shortcut) or a
		// multiscales group whose array is a child dataset (standard OME-Zarr multiscales).
		// Resolve the array path and its pixel->physical transform together from the group's
		// multiscales. The pixel->physical is matched by the field's input coordinate-system name 
		final OmeNgffReference inputRef = getInput();
		final String inputName = inputRef == null ? null : inputRef.getName();
		final ResolvedField resolved = resolveFieldFromMultiscales( n5, group, inputName );
		final String arrayPath = resolved != null ? resolved.arrayPath : group;

		final RandomAccessibleInterval<S> fieldRaw;
		try {
			fieldRaw = (RandomAccessibleInterval<S>)N5Utils.open(n5, arrayPath );
		} catch (final N5Exception e) {
			return null;
		}

		// The vector components are stored reversed only for zarr, matching the
		// reversal the writer applies; see OmeNgffMetadataParser.reverse.
		// TODO this will need generalizing if vectorAxisIndex != 0
		final RandomAccessibleInterval<S> fieldRev = OmeNgffMetadataParser.reverse(n5)
				? reverseCoordinates(fieldRaw)
				: fieldRaw;

		final CompositeIntervalView< S, RealComposite< S > >  collapsedFirst =
				Views.collapseReal(
						Views.moveAxis(fieldRev, 0, fieldRev.numDimensions() - 1 ) );

		final RealRandomAccessible<RealComposite<S>> fieldInterp = Views.interpolate(
				Views.extendBorder(collapsedFirst), getInterpolator());

		// pixel->physical: prefer the transform resolved from multiscales, then the flat attributes.
		CoordinateTransform<?> pixelToPhysicalCt = resolved != null ? resolved.pixelToPhysical : null;
		if( pixelToPhysicalCt == null ) {
			pixelToPhysicalCt = findPixelToPhysicalTransformStrict( n5, group, inputName );
		}
		if( pixelToPhysicalCt == null ) {
			pixelToPhysicalCt = findPixelToPhysicalTransformCheckSelfRef( n5, group, inputName );
		}

		if( pixelToPhysicalCt == null ) {
			field = fieldInterp;
			return field;
		}

		// TODO is this general enough?
		final AffineGet storedAffine = TransformUtils.toAffine(pixelToPhysicalCt, fieldRev.numDimensions());
		if( storedAffine == null )
			throw new N5Exception("Warning: only invertible affine pixel to physical transforms are currently supported");

		/*
		 * Make sure to handle the case when the field's coordinate transforms
		 * are both nD and (n+1)D. Since earlier versions of the spec had 
		 * the coordinate transforms apply to the vector dimension.
		 * Ignore that component of the transformation, if present. 
		 */
		final int spatialNd = fieldRev.numDimensions() - 1;
		final AffineGet affine = storedAffine.numSourceDimensions() == fieldRev.numDimensions()
				? Common.removeDimension(vectorAxisIndex, storedAffine)
				: storedAffine;

		// fail here rather than hand back an under-dimensioned field
		if( affine.numSourceDimensions() != spatialNd )
			throw new N5Exception(String.format(
					"pixel to physical transform at \"%s\" is %dD; the field at \"%s\" is %dD (%dD once its vector axis is collapsed)",
					group, storedAffine.numSourceDimensions(), arrayPath, fieldRev.numDimensions(), spatialNd));

		return RealViews.affine(fieldInterp, affine);
	}

	public static CoordinateTransform<?> findPixelToPhysicalTransformStrict(final N5Reader n5, final String group, final String output ) {

		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, "ome/"+CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		for (final CoordinateTransform<?> ct : transforms) {
			// output == null: the field transform names no input space (positional in a
			// sequence), so take the dataset's sole pixel->physical transform.
			if (output == null)
				return ct;
			// TODO properly handle reference
			if (ct.getOutput() != null && ct.getOutput().getName().equals(output) )
				return ct;
		}
		return null;
	}

	/**
	 * Reads the {@code ome/multiscales} metadata at {@code group} via the canonical
	 * {@link OmeNgffMetadataParser} (reverse-aware: coordinate systems and transform
	 * parameters come back in imglib2 order). Returns {@code null} when there is none.
	 */
	private static OmeNgffMultiScaleMetadata[] readMultiscalesMetadata(final N5Reader n5, final String group) {

		final Optional<OmeNgffMetadata> meta;
		try {
			meta = new OmeNgffMetadataParser(n5).parseMetadata(n5, new N5TreeNode(group));
		} catch (final Exception e) {
			return null;
		}
		return meta.isPresent() ? meta.get().multiscales : null;
	}

	/**
	 * Returns the coordinate systems declared in {@code group}'s {@code ome/multiscales}
	 * metadata (imglib2 order), or {@code null} if there are none.
	 */
	private static CoordinateSystem[] readMultiscalesCoordinateSystems(final N5Reader n5, final String group) {

		final OmeNgffMultiScaleMetadata[] ms = readMultiscalesMetadata(n5, group);
		if (ms == null)
			return null;

		for (final OmeNgffMultiScaleMetadata m : ms)
			if (m.coordinateSystems != null && m.coordinateSystems.length > 0)
				return m.coordinateSystems;

		return null;
	}

	/** The field array's path and its pixel-&gt;physical transform, resolved from a group's multiscales. */
	private static final class ResolvedField {

		final String arrayPath;
		final CoordinateTransform<?> pixelToPhysical;

		ResolvedField(final String arrayPath, final CoordinateTransform<?> pixelToPhysical) {
			this.arrayPath = arrayPath;
			this.pixelToPhysical = pixelToPhysical;
		}
	}

	/**
	 * Resolves the field array path and its pixel-&gt;physical transform from {@code group}'s
	 * {@code ome/multiscales}, picking the highest-resolution dataset ({@code datasets[0]}).
	 * Handles both the standard layout (a group with a child array, {@code datasets[0].path = "s0"}
	 * &rarr; {@code group + "/s0"}) and the self-referencing shortcut ({@code datasets[0].path = "."}
	 * / {@code ""} &rarr; the group itself, array co-located with the multiscales).
	 *
	 * @param n5 the reader
	 * @param group the multiscales group (the field transform's path)
	 * @param output the field's input coordinate-system name, matched against the pixel-&gt;physical
	 *            transform's output; {@code null} takes the dataset's sole transform
	 * @return the resolved field, or {@code null} if {@code group} has no multiscales (flat/legacy:
	 *            the caller opens the array at {@code group})
	 */
	private static ResolvedField resolveFieldFromMultiscales(final N5Reader n5, final String group, final String output) {

		final OmeNgffMultiScaleMetadata[] ms = readMultiscalesMetadata(n5, group);
		if (ms == null)
			return null;

		for (final OmeNgffMultiScaleMetadata m : ms) {
			final OmeNgffDataset[] datasets = m.getDatasets();
			if (datasets == null || datasets.length == 0)
				continue;

			final OmeNgffDataset d = datasets[0]; // highest resolution
			final String arrayPath = (d.path == null || d.path.isEmpty() || d.path.equals("."))
					? group
					: group + "/" + d.path;

			CoordinateTransform<?> pixelToPhysical = null;
			if (d.coordinateTransformations != null)
				for (final CoordinateTransform<?> ct : d.coordinateTransformations)
					// output == null: the field transform names no input space (positional in a
					// sequence), so take the dataset's sole pixel->physical transform.
					if (output == null || (ct.getOutput() != null && output.equals(ct.getOutput().getName()))) {
						pixelToPhysical = ct;
						break;
					}

			return new ResolvedField(arrayPath, pixelToPhysical);
		}
		return null;
	}

	/**
	 * The pixel-&gt;physical transform for the field array in {@code group}'s {@code ome/multiscales}
	 * (the highest-resolution dataset's transform whose output matches {@code output}, or its sole
	 * transform when {@code output} is {@code null}).
	 *
	 * @param n5 the reader
	 * @param group the multiscales group
	 * @param output the output coordinate-system name to match, or {@code null}
	 * @return the transform, or {@code null} if not found
	 */
	public static CoordinateTransform<?> findPixelToPhysicalFromMultiscales(final N5Reader n5, final String group, final String output ) {

		final ResolvedField resolved = resolveFieldFromMultiscales(n5, group, output);
		return resolved == null ? null : resolved.pixelToPhysical;
	}

	/*
	 * Permute (reverse) the coordinates of the first dimension.
	 * 
	 * @param dfield the displacement field.
	 */
	public static <T extends RealType<T>> RandomAccessibleInterval<T> reverseCoordinates(
			final RandomAccessibleInterval<T> dfield) {

		final long numCoordinates = dfield.dimension(0);

		if (numCoordinates == 1)
			return dfield;

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
				if( path == null )
					return pct;

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
			// TODO properly handle reference
			final String nrmInput = N5URI.normalizeGroupPath(ct.getInput().getName());
			if (nrmInput.equals(normGrp) && ct.getOutput().equals(output) ) {
				return ct.getTransform(n5);
			}
		}
		return null;
	}

}

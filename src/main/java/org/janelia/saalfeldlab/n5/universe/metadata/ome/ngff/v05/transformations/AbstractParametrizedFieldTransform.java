package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.MultiscalesAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

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

		final String path = getParameterPath();

		vectorAxisIndex = parseVectorAxisIndex( n5 );

//		// TODO generalize
//		if( vectorAxisIndex != 0 )
//			throw new N5Exception("Vector axis is in index " + vectorAxisIndex + " but only index 0 currently supported" );

		final RandomAccessibleInterval<S> fieldRaw;
		try {
			fieldRaw = (RandomAccessibleInterval<S>)N5Utils.open(n5, path );
		} catch (final N5Exception e) {
			return null;
		}

		// TODO this will need generalizing if vectorAxisIndex != 0
		final RandomAccessibleInterval<S> fieldRev = reverseCoordinates(fieldRaw);

		final CompositeIntervalView< S, RealComposite< S > >  collapsedFirst =
				Views.collapseReal(
						Views.moveAxis(fieldRev, 0, fieldRev.numDimensions() - 1 ) );

		final RealRandomAccessible<RealComposite<S>> fieldInterp = Views.interpolate(
				Views.extendBorder(collapsedFirst), getInterpolator());

		// Prefer the pixel->physical transform stored in NGFF multiscales metadata
		// (on the self-referencing dataset), then fall back to the flat attributes.
		CoordinateTransform<?> pixelToPhysicalCt = findPixelToPhysicalFromMultiscales( n5, path, getInput().getName());
		if( pixelToPhysicalCt == null ) {
			pixelToPhysicalCt = findPixelToPhysicalTransformStrict( n5, path, getInput().getName());
		}
		if( pixelToPhysicalCt == null ) {
			pixelToPhysicalCt = findPixelToPhysicalTransformCheckSelfRef( n5, path, getInput().getName());
		}

		if( pixelToPhysicalCt == null ) {
			field = fieldInterp;
			return field;
		}

		// TODO is this general enough?
		AffineGet affine = TransformUtils.toAffine(pixelToPhysicalCt, fieldRev.numDimensions());
		affine = Common.removeDimension(vectorAxisIndex, affine);
		if( affine != null )
			return RealViews.affine(fieldInterp, affine);;

		throw new N5Exception("Warning: only invertible affine pixel to physical transforms are currently supported");

		// TODO should we eventually support the general case below?
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

		final CoordinateTransform<?>[] transforms = n5.getAttribute(group, "ome/"+CoordinateTransform.KEY, CoordinateTransform[].class);
		if (transforms == null)
			return null;

		for (final CoordinateTransform<?> ct : transforms) {
			// TODO properly handle reference
			if (ct.getOutput().getName().equals(output) ) {
				return ct;
			}
		}
		return null;
	}

	private static Gson multiscalesGson(final N5Reader n5) {

		final boolean reverse = OmeNgffMetadataParser.reverse(n5);
		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(reverse))
				.registerTypeAdapter(Axis.class, new AxisAdapter())
				.registerTypeAdapter(OmeNgffMultiScaleMetadata.class, new MultiscalesAdapter(reverse))
				.create();
	}

	/**
	 * Reads the {@code ome/multiscales} metadata at a group, if present. The
	 * reverse-aware gson un-reverses coordinate-system axes and transform
	 * parameters for zarr.
	 *
	 * @param n5 the reader
	 * @param group the group
	 * @return the multiscales metadata, or {@code null} if none is present
	 */
	private static OmeNgffMultiScaleMetadata[] readMultiscales(final N5Reader n5, final String group) {

		final JsonElement el;
		try {
			el = n5.getAttribute(group, "ome/multiscales", JsonElement.class);
		} catch (final N5Exception e) {
			return null;
		}
		if (el == null || !el.isJsonArray())
			return null;

		return multiscalesGson(n5).fromJson(el, OmeNgffMultiScaleMetadata[].class);
	}

	/**
	 * Returns the coordinate systems declared in the group's {@code ome/multiscales}
	 * metadata (imglib2 order), or {@code null} if there are none.
	 */
	private static CoordinateSystem[] readMultiscalesCoordinateSystems(final N5Reader n5, final String group) {

		final OmeNgffMultiScaleMetadata[] ms = readMultiscales(n5, group);
		if (ms == null)
			return null;

		for (final OmeNgffMultiScaleMetadata m : ms)
			if (m.coordinateSystems != null && m.coordinateSystems.length > 0)
				return m.coordinateSystems;

		return null;
	}

	/**
	 * Finds the pixel to physical transform for the field array in the group's
	 * {@code ome/multiscales} metadata: the transform (on the self-referencing
	 * dataset) whose output coordinate system matches {@code output}.
	 *
	 * @param n5 the reader
	 * @param group the field array group (also the multiscales group)
	 * @param output the name of the output (field) coordinate system
	 * @return the transform, or {@code null} if not found
	 */
	public static CoordinateTransform<?> findPixelToPhysicalFromMultiscales(final N5Reader n5, final String group, final String output ) {

		final OmeNgffMultiScaleMetadata[] ms = readMultiscales(n5, group);
		if (ms == null)
			return null;

		for (final OmeNgffMultiScaleMetadata m : ms) {
			final OmeNgffDataset[] datasets = m.getDatasets();
			if (datasets == null)
				continue;

			for (final OmeNgffDataset d : datasets) {
				if (!datasetIsSelf(group, d.path))
					continue;
				if (d.coordinateTransformations == null)
					continue;

				for (final CoordinateTransform<?> ct : d.coordinateTransformations)
					if (ct.getOutput() != null && output.equals(ct.getOutput().getName()))
						return ct;
			}
		}
		return null;
	}

	/**
	 * Whether a multiscales dataset path refers to its own group. The field
	 * exporter writes a single self-referencing dataset (path {@code "."}); on
	 * read the constructor's relativization collapses it to the empty string.
	 */
	private static boolean datasetIsSelf(final String group, final String path) {

		if (path == null || path.isEmpty() || path.equals("."))
			return true;

		return N5URI.normalizeGroupPath(group).equals(N5URI.normalizeGroupPath(path));
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

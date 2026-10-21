package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.PositionFieldTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

public class CoordinateFieldCoordinateTransform<T extends RealType<T>> extends AbstractParametrizedFieldTransform<PositionFieldTransform, T> {

	public transient static final String TYPE = "coordinates";

	protected transient PositionFieldTransform transform;

	protected static final transient String vectorAxisType = "coordinate";

	public CoordinateFieldCoordinateTransform() {
		super(TYPE);
	}

	public CoordinateFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> field, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, null, interpolation, input, output);
		buildTransform( field );
	}

	public CoordinateFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> field, final String interpolation,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef) {
		super(TYPE, name, null, interpolation, inputRef, outputRef);
		buildTransform( field );
	}

	public CoordinateFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef) {
		super(TYPE, name, path, interpolation, inputRef, outputRef);
	}

	public CoordinateFieldCoordinateTransform(final String name, final N5Reader n5, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
		bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
	}

	/**
	 * A transform whose parameters are at {@code path}, with its transform already built,
	 * e.g. from the field that was just written there.
	 */
	public CoordinateFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String input, final String output, final PositionFieldTransform transform ) {
		super(TYPE, name, path, interpolation, input, output);
		this.transform = transform;
	}

	public CoordinateFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
	}

	public CoordinateFieldCoordinateTransform( final String name, final String path, final String interpolation) {
		this( name, path, interpolation, (String)null, (String)null );
	}

	public CoordinateFieldCoordinateTransform(final String path, final String interpolation) {
		this( null, path, interpolation, (String)null, (String)null );
	}

	@Override
	public String getVectorAxisType() {
		return vectorAxisType;
	}

	@Override
	public PositionFieldTransform buildTransform(final RealRandomAccessible<RealComposite<T>> field) {

		this.field = field;
		if (field == null)
			return null;

		transform = new PositionFieldTransform(field);
		return transform;
	}

	@Override
	public PositionFieldTransform getTransform() {

		if (transform != null)
			return transform;
		else if (field != null)
			return buildTransform(field);
		else if (path != null)
			return buildTransform(requireParameters());

		return null;
	}

	public static <T extends RealType<T> & NativeType<T>> CoordinateFieldCoordinateTransform<?> writeCoordinateField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> coordinateField,
			final DatasetAttributes datasetAttributes,
			final CoordinateSystem input, final CoordinateSystem output,
			final CoordinateTransform<?>[] transforms) {

		CoordinateSystem pfieldCoordinateSystem = createPositionFieldCoordinateSystem(input);
		String[] axisNames = pfieldCoordinateSystem.getAxisNames();

		final CoordinateFieldCoordinateTransform<T> cf = writtenTransform(dataset, coordinateField, input, output, transforms);

		final RandomAccessibleInterval<T> storedField = OmeNgffMetadataParser.storesDimensionsReversed(n5)
				? FieldTransformParsing.reverseCoordinates(coordinateField)
				: coordinateField;
		n5.createDataset(dataset, datasetAttributes);
		N5Utils.saveBlock(storedField, n5, dataset, datasetAttributes);

		N5MetadataUtils.writeDimensionNamesIfZarr3(n5, dataset, axisNames);
		n5.setAttribute(dataset, "ome/" + CoordinateSystem.KEY, new CoordinateSystem[]{pfieldCoordinateSystem});
		n5.setAttribute(dataset, "ome/" + CoordinateTransform.KEY, transforms);

		return cf;
	}

	public static <T extends RealType<T> & NativeType<T>> CoordinateFieldCoordinateTransform<?> writeCoordinateField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> coordinateField,
			final int[] blockSize, final Compression compression,
			 final CoordinateSystem input, final CoordinateSystem output,
			 final CoordinateTransform<?>[] transforms ) {

		CoordinateSystem pfieldCoordinateSystem = createPositionFieldCoordinateSystem(input);
		final CoordinateFieldCoordinateTransform<T> cf = writtenTransform(dataset, coordinateField, input, output, transforms);

		final RandomAccessibleInterval<T> storedField = OmeNgffMetadataParser.storesDimensionsReversed(n5)
				? FieldTransformParsing.reverseCoordinates(coordinateField)
				: coordinateField;
		N5Utils.save(storedField, n5, dataset, blockSize, compression);
		n5.setAttribute(dataset, "ome/" + CoordinateSystem.KEY, new CoordinateSystem[]{pfieldCoordinateSystem});
		n5.setAttribute(dataset, "ome/" + CoordinateTransform.KEY, transforms);

		return cf;
	}

	/**
	 * Builds the transform for a field written to {@code dataset}, from the field in memory.
	 * It is built the same way as when the field is read back
	 * (see {@link FieldTransformParsing#buildField}).
	 */
	private static <T extends RealType<T>> CoordinateFieldCoordinateTransform<T> writtenTransform(final String dataset,
			final RandomAccessibleInterval<T> coordinateField, final CoordinateSystem input, final CoordinateSystem output,
			final CoordinateTransform<?>[] transforms) {

		final String interpolation = LINEAR_INTERPOLATION;
		final RealRandomAccessible<RealComposite<T>> field = FieldTransformParsing.buildField(
				coordinateField, 0, interpolation, FieldTransformParsing.selectPixelToPhysical(transforms, input.getName()));

		return new CoordinateFieldCoordinateTransform<>("", dataset, interpolation, input.getName(), output.getName(),
				new PositionFieldTransform(field));
	}

	public static CoordinateSystem createPositionFieldCoordinateSystem(final CoordinateSystem output) {

		final Axis[] vecAxes = new Axis[output.getAxes().length + 1];
		vecAxes[0] = new Axis("coordinate", "c", null, true);
		for (int i = 1; i < vecAxes.length; i++)
			vecAxes[i] = output.getAxes()[i - 1];

		return new CoordinateSystem(output.getName(), vecAxes);
	}

}

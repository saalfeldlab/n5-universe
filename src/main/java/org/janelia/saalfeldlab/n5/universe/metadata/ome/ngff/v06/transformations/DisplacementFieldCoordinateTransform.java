package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

public class DisplacementFieldCoordinateTransform<T extends RealType<T>> extends AbstractParametrizedFieldTransform<DisplacementFieldTransform,T> {

	public transient static final String TYPE = "displacements";

	protected transient DisplacementFieldTransform transform;

	protected static final transient String vectorAxisType = "displacement";
	
	public DisplacementFieldCoordinateTransform() {
		super(TYPE);
	}

	private DisplacementFieldCoordinateTransform(final Options<T> options) {
		super(TYPE, options);
		buildTransform(options.field);
	}

	protected DisplacementFieldCoordinateTransform(final DisplacementFieldCoordinateTransform<T> other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
		// the field can not be replaced, since a path that is set refers to the field
		if (copyParams instanceof Options && ((Options<?>) copyParams).isFieldSet())
			throw new UnsupportedOperationException("The field of a displacement field transform can not be changed when copying");

		// the built transform is not shared, since transforms are not thread safe
		buildTransform(other.field);
	}

	/**
	 * Options for a displacement field transform. A transform is given either the
	 * {@link #field field} itself, or a {@link #path path} to read it from, or both
	 * (e.g. for a field that was just written to that path).
	 *
	 * @param <T> the type of the field's values
	 */
	public static class Options<T extends RealType<T>> extends AbstractFieldOptions<Options<T>> {

		private boolean fieldSet = false;
		private RealRandomAccessible<RealComposite<T>> field;

		public Options<T> field(final RealRandomAccessible<RealComposite<T>> field) {
			this.field = field;
			this.fieldSet = true;
			return this;
		}

		public boolean isFieldSet() {
			return fieldSet;
		}

		public RealRandomAccessible<RealComposite<T>> getField() {
			return field;
		}

		public DisplacementFieldCoordinateTransform<T> build() {
			return new DisplacementFieldCoordinateTransform<>(this);
		}
	}

	public static <T extends RealType<T>> Options<T> options() {
		return new Options<>();
	}

	@Override
	public DisplacementFieldCoordinateTransform<T> copy() {

		return copy(new BasicOptions());
	}

	@Override
	public DisplacementFieldCoordinateTransform<T> copy(final TransformOptions<?> copyParams) {

		return new DisplacementFieldCoordinateTransform<>(this, copyParams);
	}

	@Override
	public String getVectorAxisType() {
		return vectorAxisType;
	}

	@Override
	public DisplacementFieldTransform buildTransform( final RealRandomAccessible<RealComposite<T>> field ) {

		this.field = field;
		if (field == null)
			return null;

		transform = new DisplacementFieldTransform(field);
		return transform;
	}

	@Override
	public DisplacementFieldTransform getTransform() {

		if (transform != null)
			return transform;
		else if (field != null)
			return buildTransform(field);
		else if (path != null)
			return buildTransform(requireParameters());

		return null;
	}

	public static <T extends RealType<T> & NativeType<T>> DisplacementFieldCoordinateTransform<?> writeDisplacementField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> displacementField,
			final DatasetAttributes datasetAttributes,
			 final CoordinateSystem input, final CoordinateSystem output,
			 final CoordinateTransform<?>[] transforms ) {

		CoordinateSystem dfieldCoordinateSystem = createVectorFieldCoordinateSystem(input);

		String[] axisNames = dfieldCoordinateSystem.getAxisNames();

		final DisplacementFieldCoordinateTransform<T> df = writtenTransform(dataset, displacementField, input, output, transforms);

		final RandomAccessibleInterval<T> storedField = OmeNgffMetadataParser.storesDimensionsReversed(n5)
				? FieldTransformParsing.reverseCoordinates(displacementField)
				: displacementField;
		n5.createDataset(dataset, datasetAttributes);
		N5Utils.saveBlock(storedField, n5, dataset, datasetAttributes);

		N5MetadataUtils.writeDimensionNamesIfZarr3(n5, dataset, axisNames);
		n5.setAttribute(dataset, "ome/" + CoordinateSystem.KEY, new CoordinateSystem[]{dfieldCoordinateSystem});
		n5.setAttribute(dataset, "ome/" + CoordinateTransform.KEY, transforms);

		return df;

	}

	public static <T extends RealType<T> & NativeType<T>> DisplacementFieldCoordinateTransform<?> writeDisplacementField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> displacementField,
			final int[] blockSize, final Compression compression,
			 final CoordinateSystem input, final CoordinateSystem output,
			 final CoordinateTransform<?>[] transforms ) {

		CoordinateSystem dfieldCoordinateSystem = createVectorFieldCoordinateSystem(input);
		String[] axisNames = dfieldCoordinateSystem.getAxisNames();

		final DisplacementFieldCoordinateTransform<T> df = writtenTransform(dataset, displacementField, input, output, transforms);

		final RandomAccessibleInterval<T> storedField = OmeNgffMetadataParser.storesDimensionsReversed(n5)
				? FieldTransformParsing.reverseCoordinates(displacementField)
				: displacementField;
		N5Utils.save(storedField, n5, dataset, blockSize, compression);

		N5MetadataUtils.writeDimensionNamesIfZarr3(n5, dataset, axisNames);
		n5.setAttribute(dataset, "ome/" + CoordinateSystem.KEY, new CoordinateSystem[]{dfieldCoordinateSystem});
		n5.setAttribute(dataset, "ome/" + CoordinateTransform.KEY, transforms);

		return df;
	}

	/**
	 * Builds the transform for a field written to {@code dataset}, from the field in memory.
	 * It is built the same way as when the field is read back
	 * (see {@link FieldTransformParsing#buildField}).
	 */
	private static <T extends RealType<T>> DisplacementFieldCoordinateTransform<T> writtenTransform(final String dataset,
			final RandomAccessibleInterval<T> displacementField, final CoordinateSystem input, final CoordinateSystem output,
			final CoordinateTransform<?>[] transforms) {

		final String interpolation = LINEAR_INTERPOLATION;
		final RealRandomAccessible<RealComposite<T>> field = FieldTransformParsing.buildField(
				displacementField, 0, interpolation, FieldTransformParsing.selectPixelToPhysical(transforms, input.getName()));

		return DisplacementFieldCoordinateTransform.<T>options()
				.name("")
				.path(dataset)
				.interpolation(interpolation)
				.input(input.getName())
				.output(output.getName())
				.field(field)
				.build();
	}

	public static CoordinateSystem createVectorFieldCoordinateSystem(final CoordinateSystem output) {

		final Axis[] vecAxes = new Axis[output.getAxes().length + 1];
		vecAxes[0] = new Axis("displacement", "d", null, true);
		for (int i = 1; i < vecAxes.length; i++)
			vecAxes[i] = output.getAxes()[i - 1];

		return new CoordinateSystem(output.getName(), vecAxes);
	}

}

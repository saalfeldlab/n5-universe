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

	public DisplacementFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> fields, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, null, interpolation, input, output);
		buildTransform( fields );
	}

	public DisplacementFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> fields, final String interpolation,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef) {
		super(TYPE, name, null, interpolation, inputRef, outputRef);
		buildTransform( fields );
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef) {
		super(TYPE, name, path, interpolation, inputRef, outputRef);
	}

	public DisplacementFieldCoordinateTransform(final String name, final N5Reader n5, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
		bindParameters(n5, OmeNgffMetadataParser.reverse(n5));
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final int[] inputAxes, final int[] outputAxes ) {
		super(TYPE, name, path, interpolation, inputAxes, outputAxes );
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation) {
		this( name, path, interpolation, (String)null, (String)null );
	}

	public DisplacementFieldCoordinateTransform(final String path, final String interpolation) {
		this( null, path, interpolation, (String)null, (String)null );
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
			return buildTransform(getParameters());

		return null;
	}

	public static <T extends RealType<T> & NativeType<T>> DisplacementFieldCoordinateTransform<?> writeDisplacementField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> displacementField,
			final DatasetAttributes datasetAttributes,
			 final CoordinateSystem input, final CoordinateSystem output,
			 final CoordinateTransform<?>[] transforms ) {

		CoordinateSystem dfieldCoordinateSystem = createVectorFieldCoordinateSystem(input);

		String[] axisNames = dfieldCoordinateSystem.getAxisNames();

		final DisplacementFieldCoordinateTransform<T> df = new DisplacementFieldCoordinateTransform<>("",
				dataset, "linear", input.getName(), output.getName());

		final RandomAccessibleInterval<T> storedField = OmeNgffMetadataParser.reverse(n5)
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

		final DisplacementFieldCoordinateTransform<T> df = new DisplacementFieldCoordinateTransform<>("", 
				dataset, "linear", input.getName(), output.getName());

		final RandomAccessibleInterval<T> storedField = OmeNgffMetadataParser.reverse(n5)
				? FieldTransformParsing.reverseCoordinates(displacementField)
				: displacementField;
		N5Utils.save(storedField, n5, dataset, blockSize, compression);

		N5MetadataUtils.writeDimensionNamesIfZarr3(n5, dataset, axisNames);
		n5.setAttribute(dataset, "ome/" + CoordinateSystem.KEY, new CoordinateSystem[]{dfieldCoordinateSystem});
		n5.setAttribute(dataset, "ome/" + CoordinateTransform.KEY, transforms);

		return df;
	}

	public static CoordinateSystem createVectorFieldCoordinateSystem(final CoordinateSystem output) {

		final Axis[] vecAxes = new Axis[output.getAxes().length + 1];
		vecAxes[0] = new Axis("displacement", "d", null, true);
		for (int i = 1; i < vecAxes.length; i++)
			vecAxes[i] = output.getAxes()[i - 1];

		return new CoordinateSystem(output.getName(), vecAxes);
	}

}

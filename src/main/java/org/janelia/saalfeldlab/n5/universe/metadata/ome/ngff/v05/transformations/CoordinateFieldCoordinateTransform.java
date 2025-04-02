package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.PositionFieldTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

@NameConfig.Name("coordinates")
public class CoordinateFieldCoordinateTransform<T extends RealType<T>> extends AbstractParametrizedFieldTransform<PositionFieldTransform, T> {

	public transient static final String TYPE = "coordinates";

	protected transient PositionFieldTransform transform;

	protected transient int positionAxisIndex = 0;

	protected static final transient String vectorAxisType = "coordinate";

	public CoordinateFieldCoordinateTransform() {
		super(TYPE);
	}

	public CoordinateFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> field, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, null, interpolation, input, output);
		buildTransform( field );
	}

	public CoordinateFieldCoordinateTransform(final String name, final N5Reader n5, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
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
	public int getVectorAxisIndex() {
		return positionAxisIndex;
	}

	@Override
	public String getVectorAxisType() {
		return vectorAxisType;
	}

	@Override
	public PositionFieldTransform buildTransform(final RealRandomAccessible<RealComposite<T>> field) {

		return new PositionFieldTransform(field);
	}

	@Override
	public PositionFieldTransform getTransform() {
		if( field != null && transform == null )
			buildTransform(field);

		return transform;
	}

	public static <T extends RealType<T> & NativeType<T>> CoordinateFieldCoordinateTransform<?> writeCoordinateField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> coordinateField,
			final DatasetAttributes datasetAttributes,
			final CoordinateSystem input, final CoordinateSystem output,
			final CoordinateTransform<?>[] transforms) {

		CoordinateSystem pfieldCoordinateSystem = createPositionFieldCoordinateSystem(input);
		pfieldCoordinateSystem.reverseInPlace();

		String[] axisNames = pfieldCoordinateSystem.getAxisNames();

		final CoordinateFieldCoordinateTransform<T> cf = new CoordinateFieldCoordinateTransform<>("",
				dataset, "linear", input.getName(), output.getName());

		final RandomAccessibleInterval<T> reversedField = AbstractParametrizedFieldTransform.reverseCoordinates(coordinateField);
		n5.createDataset(dataset, datasetAttributes);
		N5Utils.saveBlock(reversedField, n5, dataset, datasetAttributes);

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
		pfieldCoordinateSystem.reverseInPlace();

		final CoordinateFieldCoordinateTransform<T> cf = new CoordinateFieldCoordinateTransform<>("", 
				dataset, "linear", input.getName(), output.getName());

		final RandomAccessibleInterval<T> reversedField = AbstractParametrizedFieldTransform.reverseCoordinates(coordinateField);
		N5Utils.save(reversedField, n5, dataset, blockSize, compression);
		n5.setAttribute(dataset, "ome/" + CoordinateSystem.KEY, new CoordinateSystem[]{pfieldCoordinateSystem});
		n5.setAttribute(dataset, "ome/" + CoordinateTransform.KEY, transforms);

		return cf;
	}

	public static CoordinateSystem createPositionFieldCoordinateSystem(final CoordinateSystem output) {

		final Axis[] vecAxes = new Axis[output.getAxes().length + 1];
		vecAxes[0] = new Axis("coordinate", "c", null, true);
		for (int i = 1; i < vecAxes.length; i++)
			vecAxes[i] = output.getAxes()[i - 1];

		return new CoordinateSystem(output.getName(), vecAxes);
	}

}

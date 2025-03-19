package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;


import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

@NameConfig.Name("displacements")
public class DisplacementFieldCoordinateTransform<T extends RealType<T>> extends AbstractParametrizedFieldTransform<DisplacementFieldTransform,T> {

	public transient static final String TYPE = "displacements";

	protected transient DisplacementFieldTransform transform;

	protected transient int positionAxisIndex = 0;

	protected static final transient String vectorAxisType = "displacement";
	
	public DisplacementFieldCoordinateTransform() {
		super(TYPE);
	}

	public DisplacementFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> fields, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, null, interpolation, input, output);
		buildTransform( fields );
	}

	public DisplacementFieldCoordinateTransform(final String name, final N5Reader n5, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String input, final String output) {
		super(TYPE, name, path, interpolation, input, output);
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String[] inputAxes, final String[] outputAxes ) {
		super(TYPE, name, path, interpolation, inputAxes, outputAxes );
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation) {
		this( name, path, interpolation, (String)null, (String)null );
	}

	public DisplacementFieldCoordinateTransform(final String path, final String interpolation) {
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
	public DisplacementFieldTransform buildTransform( final RealRandomAccessible<RealComposite<T>> field ) {
		return new DisplacementFieldTransform(field);
	}

	@Override
	public DisplacementFieldTransform getTransform() {
		if( field != null && transform == null )
			buildTransform(field);

		return transform;
	}

	public static <T extends RealType<T> & NativeType<T>> DisplacementFieldCoordinateTransform<?> writeDisplacementField(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> displacementField,
			final int[] blockSize, final Compression compression,
			 final CoordinateSystem input, final CoordinateSystem output,
			 final CoordinateTransform<?>[] transforms ) {

		CoordinateSystem dfieldCoordinateSystem = createVectorFieldCoordinateSystem(input);
		dfieldCoordinateSystem.reverseInPlace();

		final DisplacementFieldCoordinateTransform<T> df = new DisplacementFieldCoordinateTransform<>("", 
				dataset, "linear", input.getName(), output.getName());

		final RandomAccessibleInterval<T> reversedField = AbstractParametrizedFieldTransform.reverseCoordinates(displacementField);
		N5Utils.save(reversedField, n5, dataset, blockSize, compression);
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

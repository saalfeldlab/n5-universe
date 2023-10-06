package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.PositionFieldTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

public class PositionFieldCoordinateTransform<T extends RealType<T>> extends AbstractParametrizedFieldTransform<PositionFieldTransform, T> {

	public transient static final String KEY = "coordinates";

	protected transient PositionFieldTransform transform;

	protected transient int positionAxisIndex = 0;

	protected static final transient String vectorAxisType = "position";

	public PositionFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> field, final String interpolation,
			final String input, final String output) {
		super("position_field", name, null, interpolation, input, output);
		buildTransform( field );
	}

	public PositionFieldCoordinateTransform(final String name, final N5Reader n5, final String path, final String interpolation,
			final String input, final String output) {
		super("position_field", name, path, interpolation, input, output);
	}

	public PositionFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String input, final String output) {
		super("position_field", name, path, interpolation, input, output);
	}

	@Override
	public int getVectorAxisIndex() {
		return positionAxisIndex;
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

	@Override
	public int parseVectorAxisIndex( final N5Reader n5 )
	{
		CoordinateSystem[] spaces;
		try {
			spaces = n5.getAttribute(getParameterPath(), "spaces", CoordinateSystem[].class);

			final CoordinateSystem space = spaces[0];
			for( int i = 0; i < space.numDimensions(); i++ )
				if( space.getAxisTypes()[i].equals(vectorAxisType))
					return i;

		} catch (final N5Exception e) { }

		return -1;
	}

	public static <T extends RealType<T> & NativeType<T>> PositionFieldCoordinateTransform writePositionFieldTransform(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> posField,
			final int[] blockSize, final Compression compression,
			 final CoordinateSystem[] spaces, final CoordinateTransform[] transforms ) {

		try {
			N5Utils.save(posField, n5, dataset, blockSize, compression);
			n5.setAttribute(dataset, "spaces", spaces);
			n5.setAttribute(dataset, "transformations", transforms);
		} catch (final N5Exception e) {
			e.printStackTrace();
		}

		return null;
	}


}

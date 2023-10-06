package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.composite.RealComposite;

public class DisplacementFieldCoordinateTransform<T extends RealType<T>> extends AbstractParametrizedFieldTransform<DisplacementFieldTransform,T> implements RealCoordinateTransform<DisplacementFieldTransform>{

	public transient static final String KEY = "displacements";

	protected transient DisplacementFieldTransform transform;

	protected transient int positionAxisIndex = 0;

	protected static final transient String vectorAxisType = "displacement";

	public DisplacementFieldCoordinateTransform( final String name, final RealRandomAccessible<RealComposite<T>> fields, final String interpolation,
			final String input, final String output) {
		super(KEY, name, null, interpolation, input, output);
		buildTransform( fields );
	}

	public DisplacementFieldCoordinateTransform(final String name, final N5Reader n5, final String path, final String interpolation,
			final String input, final String output) {
		super(KEY, name, path, interpolation, input, output);
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String input, final String output) {
		super(KEY, name, path, interpolation, input, output);
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation,
			final String[] inputAxes, final String[] outputAxes ) {
		super(KEY, name, path, interpolation, inputAxes, outputAxes );
	}

	public DisplacementFieldCoordinateTransform( final String name, final String path, final String interpolation) {
		this( name, path, interpolation, "", "" );
	}

	@Override
	public int getVectorAxisIndex() {
		return positionAxisIndex;
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

	@Override
	public int parseVectorAxisIndex( final N5Reader n5 )
	{
		CoordinateSystem[] spaces;
		try {
			spaces = n5.getAttribute(getParameterPath(), CoordinateSystem.KEY, CoordinateSystem[].class);
			if( spaces == null || spaces.length == 0)
				return -1;

			final CoordinateSystem space = spaces[0];
			for( int i = 0; i < space.numDimensions(); i++ )
				if( space.getAxis(i).getType().equals(vectorAxisType))
					return i;
//				if( space.getAxisTypes()[i].equals(vectorAxisType))
//					return i;

		} catch (final N5Exception e) { }

		return -1;
	}

	public static <T extends RealType<T> & NativeType<T>> DisplacementFieldCoordinateTransform<?> writeDisplacementFieldTransform(
			final N5Writer n5, final String dataset, final RandomAccessibleInterval<T> posField,
			final int[] blockSize, final Compression compression,
			 final CoordinateSystem[] spaces, final CoordinateTransform<?>[] transforms ) {

		N5Utils.save(posField, n5, dataset, blockSize, compression);
		n5.setAttribute(dataset, CoordinateSystem.KEY, spaces);
		n5.setAttribute(dataset, CoordinateTransform.KEY, transforms);

		return null;
	}

}

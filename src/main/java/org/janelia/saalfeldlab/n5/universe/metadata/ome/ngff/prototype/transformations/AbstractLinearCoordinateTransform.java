package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public abstract class AbstractLinearCoordinateTransform<T extends AffineGet,P> extends AbstractParametrizedTransform<T,P> implements LinearCoordinateTransform<T> {

	public AbstractLinearCoordinateTransform( final String type ) {
		super( type );
	}

	public AbstractLinearCoordinateTransform( final String type, final String name,
			final String inputSpace, final String outputSpace ) {
		super( type, name, inputSpace, outputSpace );
	}

	public AbstractLinearCoordinateTransform( final String type, final String name, final String parameterPath,
			final String inputSpace, final String outputSpace ) {
		super( type, name, parameterPath, inputSpace, outputSpace );
	}

	public AbstractLinearCoordinateTransform( final String type, final String name, final String parameterPath,
			final String[] inputAxes, final String[] outputAxes ) {
		super( type, name, parameterPath, inputAxes, outputAxes );
	}

	@Override
	public abstract T getTransform();

	@Override
	public abstract T buildTransform( P parameters );

	protected static <T extends RealType<T> & NativeType<T>> double[] getDoubleArray(final N5Reader n5, final String path) {
		if (n5.exists(path)) {
			try {
				@SuppressWarnings("unchecked")
				final
				CachedCellImg<T, ?> data = (CachedCellImg<T, ?>) N5Utils.open(n5, path);
				if (data.numDimensions() != 1 || !(Util.getTypeFromInterval(data) instanceof RealType))
					return null;

				final double[] params = new double[(int) data.dimension(0)];
				final CellCursor<T, ?> c = data.cursor();
				int i = 0;
				while (c.hasNext())
					params[i++] = c.next().getRealDouble();

				return params;
			} catch (final N5Exception e) { }
		}
		return null;
	}

	protected static <T extends RealType<T> & NativeType<T>> double[][] getDoubleArray2(final N5Reader n5, final String path) {
		if (n5.exists(path)) {
			try {
				@SuppressWarnings("unchecked")
				final
				CachedCellImg<T, ?> data = (CachedCellImg<T, ?>) N5Utils.open(n5, path);
				if (data.numDimensions() != 2 || !(Util.getTypeFromInterval(data) instanceof RealType))
					return null;

				final double[][] params = new double[(int) data.dimension(0)] [(int) data.dimension(1)];
				final CellCursor<T, ?> c = data.cursor();
				while (c.hasNext()) {
					c.fwd();
					params[c.getIntPosition(0)][c.getIntPosition(1)] = c.get().getRealDouble();
				}

				return params;
			} catch (final N5Exception e) { }
		}
		return null;
	}

}

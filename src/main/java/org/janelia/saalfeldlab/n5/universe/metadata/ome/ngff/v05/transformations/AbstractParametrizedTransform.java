package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class AbstractParametrizedTransform<T extends RealTransform,P> extends AbstractCoordinateTransform<T> implements ParametrizedTransform<T,P> {

	protected final String path;

	protected transient String absolutePath;

	public AbstractParametrizedTransform() {
		super();
		path = null;
	}

	public AbstractParametrizedTransform( String type ) {
		this( type, null );
	}

	public AbstractParametrizedTransform( String type, String name ) {
		super( type, name );
		this.path = null;
	}

	public AbstractParametrizedTransform( String type, String name, String inputSpace, String outputSpace ) {
		this( type, name, null, inputSpace, outputSpace );
	}

	public AbstractParametrizedTransform( String type, String name, String parameterPath,
			String inputSpace, String outputSpace ) {
		super( type, name, inputSpace, outputSpace );
		this.path = parameterPath;
	}

	public AbstractParametrizedTransform( String type, String name, OmeNgffReference inputRef, OmeNgffReference outputRef ) {
		this( type, name, null, inputRef, outputRef );
	}

	public AbstractParametrizedTransform( String type, String name, String parameterPath,
			OmeNgffReference inputRef, OmeNgffReference outputRef ) {
		super( type, name, inputRef, outputRef );
		this.path = parameterPath;
	}

	public AbstractParametrizedTransform( String type, String name, String parameterPath,
			int[] inputAxes, int[] outputAxes ) {
		super( type, name, inputAxes, outputAxes );
		this.path = parameterPath;
	}

	public AbstractParametrizedTransform( AbstractParametrizedTransform<T,P> other )
	{
		super( other );
		this.path = other.getParameterPath();
	}

	public AbstractParametrizedTransform( AbstractParametrizedTransform<T,P> other, int[] inputAxes, int[] outputAxes )
	{
		super( other, inputAxes, outputAxes );
		this.path = other.getParameterPath();
	}

	@Override
	public String getParameterPath() {

		return absolutePath != null ? absolutePath : path;
	}

	@Override
	public void resolveAbsoluePath(final String groupPath) {

		absolutePath = N5URI.normalizeGroupPath(groupPath + "/" + path);
	}
	
	protected static <T extends RealType<T> & NativeType<T>> double[] getDoubleArray(final N5Reader n5, final String path) {

		if (n5 == null)
			return null;

		if (n5.exists(path)) {
			try {
				@SuppressWarnings("unchecked")
				final CachedCellImg<T, ?> data = (CachedCellImg<T, ?>)N5Utils.open(n5, path);
				if (data.numDimensions() != 1 || !(data.getType() instanceof RealType))
					return null;

				final double[] params = new double[(int)data.dimension(0)];
				final CellCursor<T, ?> c = data.cursor();
				int i = 0;
				while (c.hasNext())
					params[i++] = c.next().getRealDouble();

				return params;
			} catch (final N5Exception e) {}
		}
		return null;
	}

	protected static <T extends RealType<T> & NativeType<T>> double[][] getDoubleArray2(final N5Reader n5, final String path) {

		if (n5 == null)
			return null;

		if (n5.exists(path)) {
			try {

				RandomAccessibleInterval<T> matrix;
				final CachedCellImg<T, ?> data = (CachedCellImg<T, ?>)N5Utils.open(n5, path);

				// TODO
				if (n5 instanceof ZarrKeyValueReader)
					matrix = Views.moveAxis(data, 0, 1);
				else
					matrix = data;

				if (data.numDimensions() != 2 || !(data.getType() instanceof RealType))
					return null;

				final double[][] params = new double[(int)matrix.dimension(0)][(int)matrix.dimension(1)];
				final Cursor<T> c = Views.flatIterable(matrix).cursor();
				while (c.hasNext()) {
					c.fwd();
					params[c.getIntPosition(0)][c.getIntPosition(1)] = c.get().getRealDouble();
				}

				return params;
			} catch (final N5Exception e) {}
		}
		return null;
	}

}

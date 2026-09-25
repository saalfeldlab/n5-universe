package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.function.Supplier;

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

	private Supplier<P> parameterSupplier;

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
	
	/**
	 * Generally, do not call this method yourself. This is made public for testing
	 * purposes.
	 *
	 * @param parameterSupplier
	 *            supplies the parameters
	 */
	public void setParameterSupplier(Supplier<P> parameterSupplier) {
		this.parameterSupplier = parameterSupplier;
	}

	public P getParameters() {
		return parameterSupplier.get();
	}

	@Override
	public String getParameterPath() {

		return absolutePath != null ? absolutePath : path;
	}

	@Override
	public void resolveAbsoluePath(final String groupPath) {

		absolutePath = N5URI.normalizeGroupPath(groupPath + "/" + path);
	}

}

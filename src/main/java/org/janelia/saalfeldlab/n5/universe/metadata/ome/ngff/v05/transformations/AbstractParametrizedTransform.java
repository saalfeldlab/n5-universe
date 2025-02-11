package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5URI;

import net.imglib2.realtransform.RealTransform;

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

	public AbstractParametrizedTransform( String type, String name, String parameterPath,
			String[] inputAxes, String[] outputAxes ) {
		super( type, name, inputAxes, outputAxes );
		this.path = parameterPath;
	}

	public AbstractParametrizedTransform( AbstractParametrizedTransform<T,P> other )
	{
		super( other );
		this.path = other.getParameterPath();
	}

	public AbstractParametrizedTransform( AbstractParametrizedTransform<T,P> other, String[] inputAxes, String[] outputAxes )
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

}

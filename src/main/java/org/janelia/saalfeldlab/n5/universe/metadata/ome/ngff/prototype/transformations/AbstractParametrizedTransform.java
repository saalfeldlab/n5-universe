package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractParametrizedTransform<T extends RealTransform,P> extends AbstractCoordinateTransform<T> implements ParametrizedTransform<T,P> {

	private final String path;
	
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

	@Override
	public String getParameterPath() {
		return path;
	}

}

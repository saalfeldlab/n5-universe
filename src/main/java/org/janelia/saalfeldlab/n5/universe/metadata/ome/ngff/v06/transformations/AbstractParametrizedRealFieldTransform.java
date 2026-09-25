package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.NumericType;

public abstract class AbstractParametrizedRealFieldTransform<T extends RealTransform,P,V extends NumericType<V>> extends AbstractParametrizedTransform<T,RealRandomAccessible<V>[]> {

	protected transient RealRandomAccessible<V>[] fields;

	protected int vectorAxisIndex;

	public AbstractParametrizedRealFieldTransform( final String type, final String name, final String inputSpace, final String outputSpace ) {
		this( type, name, null, inputSpace, outputSpace );
	}

	public AbstractParametrizedRealFieldTransform( final String type, final String name, final String parameterPath,
			final String inputSpace, final String outputSpace ) {
		super( type, name, inputSpace, outputSpace );
	}

	public int getVectorAxisIndex() {
		return vectorAxisIndex;
	}

	public abstract int parseVectorAxisIndex( N5Reader n5 );

	public RealRandomAccessible<V>[] getFields() {
		return fields;
	}

}

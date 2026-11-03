package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.NumericType;

public abstract class AbstractParametrizedRealFieldTransform<T extends RealTransform,P,V extends NumericType<V>> extends AbstractParametrizedTransform<T,RealRandomAccessible<V>[]> {

	protected transient RealRandomAccessible<V>[] fields;

	protected int vectorAxisIndex;

	protected AbstractParametrizedRealFieldTransform( final String type, final TransformOptions<?> options ) {
		super( type, options );
	}

	protected AbstractParametrizedRealFieldTransform( final AbstractParametrizedRealFieldTransform<T,P,V> other, final TransformOptions<?> copyParams ) {
		super( other, copyParams );
		// the fields are immutable, so they can be shared. Subclasses rebuild their transform from them.
		this.fields = other.fields;
		this.vectorAxisIndex = other.vectorAxisIndex;
	}

	public int getVectorAxisIndex() {
		return vectorAxisIndex;
	}

	public abstract int parseVectorAxisIndex( N5Reader n5 );

	public RealRandomAccessible<V>[] getFields() {
		return fields;
	}

}

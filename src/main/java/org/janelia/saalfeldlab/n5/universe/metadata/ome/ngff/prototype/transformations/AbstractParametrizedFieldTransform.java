package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.PositionFieldTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

public abstract class AbstractParametrizedFieldTransform<T extends RealTransform, S extends RealType<S>>
	extends AbstractParametrizedTransform<T,RealRandomAccessible<RealComposite<S>>> {

	protected transient RealRandomAccessible<RealComposite<S>> field;

	// TODO allow explicit 1d parameters ?
//	protected double[] parameters;

	protected transient int vectorAxisIndex;

	public AbstractParametrizedFieldTransform( final String type, final String name, final String inputSpace, final String outputSpace ) {
		this( type, name, null, inputSpace, outputSpace );
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String parameterPath,
			final String inputSpace, final String outputSpace ) {
		super( type, name, parameterPath, inputSpace, outputSpace );
	}

	public AbstractParametrizedFieldTransform( final String type, final String name, final String parameterPath,
			final String[] inputAxes, final String[] outputAxes ) {
		super( type, name, parameterPath, inputAxes, outputAxes );
	}

	public int getVectorAxisIndex() {
		return vectorAxisIndex;
	}

	public abstract int parseVectorAxisIndex( N5Reader n5 );

	public RealRandomAccessible<RealComposite<S>> getField() {
		return field;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RealRandomAccessible<RealComposite<S>> getParameters(final N5Reader n5) {

		final String path = getParameterPath();
		vectorAxisIndex = parseVectorAxisIndex( n5 );

		InvertibleRealTransform ixfm = null;
		CoordinateTransform<?>[] transforms = null;
		try {
			transforms = n5.getAttribute(path, "transformations", CoordinateTransform[].class);
		} catch (final N5Exception e1) { }

		System.out.println( transforms[0] instanceof InvertibleCoordinateTransform );
		if( transforms != null && transforms.length >= 1  &&
			 transforms[0] instanceof InvertibleCoordinateTransform ) {

			ixfm = ((InvertibleCoordinateTransform<?>)transforms[0]).getInverseTransform();
		}

		final RandomAccessibleInterval<S> fieldRaw;
		try {
			fieldRaw = (RandomAccessibleInterval<S>)N5Utils.open(n5, path );
		} catch (final N5Exception e) {
			return null;
		}

		final CompositeIntervalView< S, RealComposite< S > >  collapsedFirst =
				Views.collapseReal(
						Views.moveAxis( fieldRaw, 0, fieldRaw.numDimensions() - 1 ) );

		field = Views.interpolate(
				Views.extendBorder(collapsedFirst),
				new NLinearInterpolatorFactory<>());

//		RandomAccessible<V>[] rawfields;
//		int nv = 1;
//		if( vectorAxisIndex < 0 ) {
//			rawfields = new RandomAccessible[]{ fieldRaw };
//		}
//		else {
//			nv = (int)fieldRaw.dimension(getVectorAxisIndex());
//			rawfields = new RandomAccessible[nv];
//			for( int i = 0; i < nv; i++ )
//			{
//				rawfields[i] = Views.extendZero(Views.hyperSlice( fieldRaw, vectorAxisIndex, i));
//			}
//		}
//
//		fields = new RealRandomAccessible[ rawfields.length ];
//		for( int i = 0; i < nv; i++ ) {
//			if( ixfm == null )
//			{
//				fields[i] = Views.interpolate( rawfields[i], new NLinearInterpolatorFactory<>());
//			}
//			else {
//				fields[i] = new RealTransformRealRandomAccessible(
//						Views.interpolate( rawfields[i], new NLinearInterpolatorFactory<>()),
//						ixfm );
//			}
//		}

		return field;
	}

}

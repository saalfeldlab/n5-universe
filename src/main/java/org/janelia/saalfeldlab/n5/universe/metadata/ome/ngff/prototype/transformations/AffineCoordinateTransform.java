package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.prototype.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class AffineCoordinateTransform extends AbstractLinearCoordinateTransform<AffineGet,double[]> 
	implements InvertibleCoordinateTransform<AffineGet> {

	public double[] affine;

	public transient AffineGet transform;
	
	public AffineCoordinateTransform( final double[] affine) {
		super("affine");
		this.affine = affine;
		buildTransform( affine );
	}

	public AffineCoordinateTransform( final String name, final String inputSpace, final String outputSpace ,
			final double[] affine) {
		super("affine", name, null, inputSpace, outputSpace );
		this.affine = affine;
		buildTransform( affine );
	}

	public AffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super("affine", name, path, inputSpace, outputSpace );
		this.affine = getParameters(n5);
		buildTransform( affine );
	}

	public AffineCoordinateTransform( final String name, 
			final String[] inputAxes, final String[] outputAxes,
			final double[] affine ) {
		super("affine", name, null, inputAxes, outputAxes  );
		this.affine = affine;
	}

	public AffineCoordinateTransform( final String name, final String path,
			final String inputSpace, final String outputSpace) {
		super("affine", name, path, inputSpace, outputSpace  );
	}

	@Override
	public AffineGet buildTransform( double[] parameters ) {
		this.affine = parameters;
		if( parameters.length == 6 ) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set( parameters );
			transform = tmp;
		}
		else if( parameters.length == 12 ) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set( parameters );
			transform = tmp;
		}
		else {
			int nd = (int)Math.floor( Math.sqrt( parameters.length ));
			AffineTransform tmp = new AffineTransform( nd );
			tmp.set(parameters);
			transform = tmp;
		}
		return transform;
	}

	@Override
	public AffineGet getTransform() {
		if( affine != null && transform == null )
			buildTransform(affine);
		return transform;
	}

	@Override
	public double[] getParameters(N5Reader n5) {
		return getDoubleArray( n5 , getParameterPath() );
	}
	
}

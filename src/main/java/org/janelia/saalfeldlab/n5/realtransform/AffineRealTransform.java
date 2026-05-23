package org.janelia.saalfeldlab.n5.realtransform;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.RealTransform;

/**
 * An affine (linear + translation) {@link RealTransform} between points of
 * arbitrary dimension.
 * <p>
 * Is not invertible because source and target dimensionality may differ.a This
 * class can can also represent non-invertible affine transofrmations between
 * points of the same dimension.
 */
public class AffineRealTransform implements RealTransform {

    protected final int numSourceDimensions;

    protected final int numTargetDimensions;

    protected final double[] matrix; // stored row major

    public AffineRealTransform( int numSourceDimensions, int numTargetDimensions ) {
        this.numSourceDimensions = numSourceDimensions;
        this.numTargetDimensions = numTargetDimensions;
        matrix = new double[ (numSourceDimensions + 1) * numTargetDimensions ];
    }

	public AffineRealTransform( int numSourceDimensions, int numTargetDimensions, double[] matrix ) {

		int expectedLength = (numSourceDimensions + 1) * numTargetDimensions;
		if (matrix.length != expectedLength)
			throw new IllegalArgumentException(
					"matrix length " + matrix.length + " != (numSourceDimensions + 1) * numTargetDimensions (" +
							expectedLength + ")");

		this.numSourceDimensions = numSourceDimensions;
		this.numTargetDimensions = numTargetDimensions;
		this.matrix = matrix;
    }

    @Override
    public int numSourceDimensions() {
        return numSourceDimensions;
    }

    @Override
    public int numTargetDimensions() {
        return numTargetDimensions;
    }

    @Override
    public void apply(double[] src, double[] dst) {

        double[] tgt;
        if( src == dst )
            tgt = new double[ numTargetDimensions ];
        else
            tgt = dst;

        int k = 0;
        for( int j = 0; j < numTargetDimensions; j++ )
        {
            tgt[j] = 0;
            for( int i = 0; i < numSourceDimensions; i++ )
                tgt[j] += matrix[k++] * src[i];

            tgt[j] += matrix[k++]; // translation component
        }

        if( tgt != dst )
            System.arraycopy( tgt, 0, dst, 0, tgt.length );

    }

    @Override
    public void apply(RealLocalizable src, RealPositionable dst) {

        RealPositionable tgt;
        if( src == dst )
            tgt = new RealPoint( numTargetDimensions );
        else
            tgt = dst;

        int k = 0;
        double val = 0;
        for( int j = 0; j < numTargetDimensions; j++ )
        {
            val = 0;
            for( int i = 0; i < numSourceDimensions; i++ ) {
                val += matrix[k++] * src.getDoublePosition(i);
            }
            val += matrix[k++]; // translation component
            tgt.setPosition( val, j );
        }

        if( tgt != dst )
            dst.setPosition( (RealPoint) tgt);
    }

	@Override
	public RealTransform copy() {

		return new AffineRealTransform(numSourceDimensions, numTargetDimensions, matrix);
	}

}

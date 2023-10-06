package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.io.IOException;

public class MatrixCoordinateTransform //extends AbstractParametrizedTransform<LinearRealTransform,double[]>
{

//    protected int numSourceDimensions;
//
//    protected int numTargetDimensions;
//
//    protected double[] matrix;
//
//    protected transient LinearRealTransform transform;
//
//    public MatrixCoordinateTransform( final String name, final String inputSpace, final String outputSpace ,
//                                      final double[] matrix, int numIn, int numOut ) {
//        super("matrix", name, null, inputSpace, outputSpace );
//        this.matrix = matrix;
//        this.numSourceDimensions = numIn;
//        this.numTargetDimensions = numOut;
//        buildTransform( matrix );
//    }
//
//    public MatrixCoordinateTransform( final String name, final String[] inputAxes, final String[] outputAxes ,
//                                      final double[] matrix, int numIn, int numOut ) {
//        super("matrix", name, null, inputAxes, outputAxes );
//        this.matrix = matrix;
//        this.numSourceDimensions = numIn;
//        this.numTargetDimensions = numOut;
//        buildTransform( matrix );
//    }
//
//    public MatrixCoordinateTransform(final String name, final N5Reader n5, final String path,
//                                     final String inputSpace, final String outputSpace) {
//        super("matrix", name, path, inputSpace, outputSpace );
//        this.matrix = getParameters(n5);
//        buildTransform( matrix );
//    }
//
//    public MatrixCoordinateTransform(final String name, final N5Reader n5, final String path,
//                                     final String[] inputAxes, final String[] outputAxes) {
//        super("matrix", name, path, inputAxes, outputAxes );
//        this.matrix = getParameters(n5);
//        buildTransform( matrix );
//    }
//
//    public MatrixCoordinateTransform( final String name, final String path,
//                                      final String inputSpace, final String outputSpace) {
//        super("matrix", name, path, inputSpace, outputSpace  );
//        this.matrix = null;
//    }
//
//    @Override
//    public LinearRealTransform buildTransform( double[] parameters ) {
//        this.matrix = parameters;
//        transform = new LinearRealTransform( numSourceDimensions, numTargetDimensions, matrix );
//        return transform;
//    }
//
//    @Override
//    public LinearRealTransform getTransform() {
//        if( matrix != null && transform == null )
//            buildTransform(matrix);
//        return transform;
//    }
//
//    @Override
//    public double[] getParameters(N5Reader n5) {
//        return getDoubleArray( n5 , getParameterPath() );
//    }
//
//    protected static <T extends RealType<T> & NativeType<T>> double[] getDoubleArray(final N5Reader n5, final String path) {
//        if (n5.exists(path)) {
//            try {
//                @SuppressWarnings("unchecked")
//                CachedCellImg<T, ?> data = (CachedCellImg<T, ?>) N5Utils.open(n5, path);
//                if (data.numDimensions() != 1 || !(Util.getTypeFromInterval(data) instanceof RealType))
//                    return null;
//
//                double[] params = new double[(int) data.dimension(0)];
//                CellCursor<T, ?> c = data.cursor();
//                int i = 0;
//                while (c.hasNext())
//                    params[i++] = c.next().getRealDouble();
//
//                return params;
//            } catch (IOException e) { }
//        }
//        return null;
//    }

}

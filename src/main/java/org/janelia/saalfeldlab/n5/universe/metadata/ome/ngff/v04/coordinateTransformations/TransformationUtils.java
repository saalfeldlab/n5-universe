package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;

import net.imglib2.realtransform.AffineTransform3D;

public class TransformationUtils {
	
	public static AffineTransform3D tranformsToAffine(OmeNgffDataset dataset, CoordinateTransformation<?>[] transforms )
	{
		if( dataset.coordinateTransformations == null )
			return new AffineTransform3D(); // identity

		final AffineTransform3D out = buildTransform(dataset.coordinateTransformations);
		if( transforms != null )
			out.preConcatenate(buildTransform(transforms));

		return out;
	}

	public static AffineTransform3D buildTransform( CoordinateTransformation<?>[] transforms ) {
		
		final AffineTransform3D out = new AffineTransform3D();
		for( CoordinateTransformation<?> ct : transforms )
			out.preConcatenate(ct.getTransform());

		return out;
	}

}

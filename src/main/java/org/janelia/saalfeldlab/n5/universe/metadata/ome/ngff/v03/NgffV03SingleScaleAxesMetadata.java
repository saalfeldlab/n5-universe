package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.Scale;

public class NgffV03SingleScaleAxesMetadata extends NgffSingleScaleAxesMetadata {

	private transient final AffineGet transform;
	
	private static final String[] defaultAxisLabels = {"x", "y", "z", "c", "t"};

	public NgffV03SingleScaleAxesMetadata(final String path,
			final double[] scale,
			final DatasetAttributes datasetAttributes) {

		this(path, scale,
				defaultAxes(scale != null ? scale.length : 5),
				datasetAttributes);
	}

	public NgffV03SingleScaleAxesMetadata(final String path,
			final double[] scale,
			final Axis[] axes,
			final DatasetAttributes datasetAttributes) {
		
		super( path, scale, 
				new double[scale.length], // translation
				axes,
				datasetAttributes);

		this.transform = new Scale(scale);
	}
	
	private static Axis[] defaultAxes(int N) {

		return AxisUtils.defaultAxes(Arrays
				.stream(defaultAxisLabels).limit(N).toArray(num -> new String[num]));
	}

	@Override
	public AffineGet spatialTransform() {

		return transform;
	}

	@Override
	public NgffV03SingleScaleAxesMetadata modifySpatialTransform(final String newPath, final AffineGet relativeTransformation) {

		final int nd = getAxes().length;
		final AffineTransform newTransform = new AffineTransform(nd);
		newTransform.preConcatenate(spatialTransform());
		newTransform.preConcatenate(relativeTransformation);

		final double[] newScale = new double[nd];
		int j = 0;
		for (int i = 0; i < nd; i++) {
			newScale[i] = newTransform.get(j, j);
			j++;
		}
		return new NgffV03SingleScaleAxesMetadata(newPath, newScale, getAxes(), getAttributes());
	}

}

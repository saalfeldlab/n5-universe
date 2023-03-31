package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Interface for a metadata whose children are each {@link SpatialMetadata}, and is itslef {@link SpatialMetadata}.
 * <p>
 * The children metadata are usually related in some way. For example, a
 * {@link MultiscaleMetadata} is a set of SpatialMetadata where each
 * child is a resampling of the same underlying data at a different spatial
 * resolution.
 * <p>
 * By default, the Group itself will delegate the {@link SpatialMetadata} calls to it's first child (e.g. s0)
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface SpatialMetadataGroup<T extends SpatialMetadata> extends N5MetadataGroup<T>, SpatialMetadata {

  default AffineGet[] spatialTransforms() {

	return Arrays.stream(getChildrenMetadata()).map(SpatialMetadata::spatialTransform).toArray(AffineGet[]::new);
  }

  String[] units();

  default AffineTransform3D[] spatialTransforms3d() {

	final List<AffineTransform3D> transforms = new ArrayList<>();
	for (final AffineGet transform : spatialTransforms()) {
	  // return identity if null
	  if (transform == null) {
		transforms.add(new AffineTransform3D());
	  } else if (transform instanceof AffineTransform3D)
		transforms.add((AffineTransform3D)transform);
	  else {
		final int N = transform.numSourceDimensions();

		int k = 0;
		final AffineTransform3D transform3d = new AffineTransform3D();
		final double[] params = transform3d.getRowPackedCopy();
		for (int i = 0; i < 3; i++) {
		  for (int j = 0; j < 3; j++) {
			if (i < N && j < N)
			  params[k] = transform.get(i, j);

			k++;
		  }

		  // j == 4
		  if (i < N)
			params[k] = transform.get(i, N);

		  k++;
		}

		transform3d.set(params);
		transforms.add(transform3d);
	  }
	}
	return transforms.stream().map(AffineTransform3D::copy).toArray(AffineTransform3D[]::new);
  }

  @Override default AffineGet spatialTransform() {

	return getChildrenMetadata()[0].spatialTransform();
  }

  @Override default String unit() {

	return getChildrenMetadata()[0].unit();
  }

  @Override default AffineTransform3D spatialTransform3d() {

	return getChildrenMetadata()[0].spatialTransform3d();
  }
}

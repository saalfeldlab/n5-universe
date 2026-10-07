package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

/**
 * Prepares parsed transforms to read their parameters: resolves each parameter
 * path against the group holding the metadata, and binds it to a reader.
 */
public class ParameterBinding {

	private ParameterBinding() {}

	/**
	 * Resolves and binds every parametrized transform in {@code ct}, including those
	 * nested in sequences, byDimension, bijections and inverses.
	 *
	 * @param ct the transform, may be null
	 * @param n5 the reader to read parameters from
	 * @param group the group holding the metadata {@code ct} was read from
	 */
	public static void bind(final CoordinateTransform<?> ct, final N5Reader n5, final String group) {

		if (ct == null)
			return;

		if (ct instanceof AbstractParametrizedTransform) {
			final AbstractParametrizedTransform<?, ?> pt = (AbstractParametrizedTransform<?, ?>)ct;
			if (pt.getParameterPath() != null) {
				pt.resolveAbsoluePath(group);
				pt.bindParameters(n5, OmeNgffMetadataParser.reverse(n5));
			}
		} else if (ct instanceof SequenceCoordinateTransform) {
			bindAll(((SequenceCoordinateTransform)ct).getTransformations(), n5, group);
		} else if (ct instanceof ByDimensionCoordinateTransform) {
			bindAll(((ByDimensionCoordinateTransform)ct).getTransformations(), n5, group);
		} else if (ct instanceof BijectionCoordinateTransform) {
			bind(((BijectionCoordinateTransform)ct).getForward(), n5, group);
			bind(((BijectionCoordinateTransform)ct).getInverse(), n5, group);
		} else if (ct instanceof InverseCoordinateTransform) {
			bind(((InverseCoordinateTransform<?, ?>)ct).getWrappedCoordinateTransform(), n5, group);
		}
	}

	public static void bindAll(final CoordinateTransform<?>[] cts, final N5Reader n5, final String group) {

		if (cts != null)
			for (final CoordinateTransform<?> ct : cts)
				bind(ct, n5, group);
	}
}

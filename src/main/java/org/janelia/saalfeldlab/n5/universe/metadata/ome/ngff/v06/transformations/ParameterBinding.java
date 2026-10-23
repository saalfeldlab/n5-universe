package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.function.Consumer;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;

/**
 * Prepares parsed transforms to read their parameters: resolves each parameter
 * path against the group holding the metadata, and binds it to a reader.
 * <p>
 * Transforms read with {@link CoordinateTransforms#read} or the ome-ngff parsers are
 * already bound; this is for transforms that came from elsewhere, e.g. a reader's own gson.
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

		forEachParametrized(ct, pt -> {
			pt.resolveAbsolutePath(group);
			pt.bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
		});
	}

	public static void bindAll(final CoordinateTransform<?>[] cts, final N5Reader n5, final String group) {

		if (cts != null)
			for (final CoordinateTransform<?> ct : cts)
				bind(ct, n5, group);
	}

	/**
	 * Binds every parametrized transform in {@code ct} that is not bound yet to
	 * {@code n5}, without resolving its path. For callers that have a reader but
	 * not the group the metadata came from.
	 *
	 * @param ct the transform, may be null
	 * @param n5 the reader to read parameters from
	 */
	public static void bindUnbound(final CoordinateTransform<?> ct, final N5Reader n5) {

		if (n5 == null)
			return;

		forEachParametrized(ct, t -> {
			if (!t.isBound())
				t.bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
		});
	}

	private static void forEachParametrized(final CoordinateTransform<?> ct,
			final Consumer<AbstractParametrizedTransform<?, ?>> action) {

		if (ct == null)
			return;

		if (ct instanceof AbstractParametrizedTransform) {
			final AbstractParametrizedTransform<?, ?> pt = (AbstractParametrizedTransform<?, ?>)ct;
			if (pt.getParameterPath() != null)
				action.accept(pt);
		} else if (ct instanceof SequenceCoordinateTransform) {
			for (final CoordinateTransform<?> t : ((SequenceCoordinateTransform)ct).getTransformations())
				forEachParametrized(t, action);
		} else if (ct instanceof ByDimensionCoordinateTransform) {
			for (final CoordinateTransform<?> t : ((ByDimensionCoordinateTransform)ct).getTransformations())
				forEachParametrized(t, action);
		} else if (ct instanceof BijectionCoordinateTransform) {
			forEachParametrized(((BijectionCoordinateTransform)ct).getForward(), action);
			forEachParametrized(((BijectionCoordinateTransform)ct).getInverse(), action);
		} else if (ct instanceof InverseCoordinateTransform) {
			forEachParametrized(((InverseCoordinateTransform<?, ?>)ct).getWrappedCoordinateTransform(), action);
		}
	}
}

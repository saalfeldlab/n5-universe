package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.Objects;

/**
 * An abstract class for a {@link SpatialMetadataGroup} whose children contain
 * the same data sampled at different resolutions.
 *
 * @param <T> metadata type
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public abstract class SpatialMultiscaleMetadata<T extends N5SpatialDatasetMetadata> extends MultiscaleMetadata<T> implements SpatialMetadataGroup<T> {

	private String[] units;

	public SpatialMultiscaleMetadata(final String basePath, final T[] childrenMetadata) {

		super(basePath, childrenMetadata);

		final int N = childrenMetadata.length;
		units = new String[N];

		int i = 0;
		for (T meta : childrenMetadata) {
			Objects.requireNonNull(meta);
			units[i] = meta.unit();
			i++;
		}
	}

	@Override
	public String[] units() {

		return units;
	}

	@Override public String unit() {

		return units[0];
	}
}

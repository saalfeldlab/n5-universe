package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Arrays;

/**
 * Default implementation of {@link IndexedAxisMetadata}.
 *
 * @author John Bogovic
 *
 */
public class IndexedAxisListMetadata implements IndexedAxisMetadata {

	protected IndexedAxis[] axes;

	public IndexedAxisListMetadata( final IndexedAxis[] axes ) {
		this.axes = axes;
	}

	@Override
	public Axis[] getAxes() {

		return axes;
	}

	@Override
	public int[] getIndexes() {
		return Arrays.stream(axes).mapToInt(IndexedAxis::getIndex).toArray();
	}
}

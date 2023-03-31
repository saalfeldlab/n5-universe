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

	public IndexedAxisListMetadata( IndexedAxis[] axes ) {
		this.axes = axes;
	}

	// consder moving the overriding methods to an abstract class

	@Override
	public String[] getAxisLabels() {
		return Arrays.stream(axes).map(IndexedAxis::getLabel).toArray(String[]::new);
	}

	@Override
	public String[] getAxisTypes() {
		return Arrays.stream(axes).map(IndexedAxis::getType).toArray(String[]::new);
	}

	@Override
	public String[] getUnits() {
		return Arrays.stream(axes).map(IndexedAxis::getUnit).toArray(String[]::new);
	}

	@Override
	public int[] getIndexes() {
		return Arrays.stream(axes).mapToInt(IndexedAxis::getIndex).toArray();
	}
}

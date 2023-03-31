package org.janelia.saalfeldlab.n5.universe.metadata.axes;

/**
 * Default implementation of {@link IndexedAxisMetadata}.
 *
 * @author John Bogovic
 *
 */
public class DefaultIndexedAxisMetadata extends DefaultAxisMetadata implements IndexedAxisMetadata {

	protected int[] indexes;

	public DefaultIndexedAxisMetadata(String path, String[] labels, String[] types, String[] units,
			int[] indexes ) {
		super( path, labels, types, units );
		this.indexes = indexes;
	}

	@Override
	public int[] getIndexes() {
		return indexes;
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.stream.IntStream;

/**
 * Metadata that labels and assigns types to axes. 
 *
 * @author John Bogovic
 *
 */
public interface IndexedAxisMetadata extends AxisMetadata
{

	public default int[] getIndexes() {
		return IntStream.range(0, getAxisTypes().length ).toArray();
	}

	public default IndexedAxis getIndexedAxis( int i ) {
		return new IndexedAxis(getAxisTypes()[i], getAxisLabels()[i], getUnits()[i],getIndexes()[i]);
	}

	public default IndexedAxis[] getIndexedAxes() {
		return IntStream.range(0, getAxisTypes().length ).mapToObj( i -> getIndexedAxis(i)).toArray( IndexedAxis[]::new );
	}
}

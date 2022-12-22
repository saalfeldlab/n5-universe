package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.stream.IntStream;

/**
 * Metadata that labels and assigns types to axes. 
 *
 * @author John Bogovic
 *
 */
public interface AxisMetadata {

	public String[] getAxisLabels();

	public String[] getAxisTypes();

	public String[] getUnits();

	public default Axis getAxis( int i ) {
		return new Axis(getAxisTypes()[i], getAxisLabels()[i], getUnits()[i]);
	}

	public default Axis[] getAxes() {
		return IntStream.range(0, getAxisTypes().length ).mapToObj( i -> getAxis(i)).toArray( Axis[]::new );
	}

	/**
	 * 
	 * @param label the label
	 * @return index corresponding to that label
	 */
	public default int indexOf(String label) {
		for (int i = 0; i < getAxisLabels().length; i++)
			if (getAxisLabels()[i].equals(label))
				return i;

		return -1;
	}

	public default int[] indexesOfType( final String type ) {
		String[] types = getAxisTypes();
		return IntStream.range(0, getAxisTypes().length )
			.filter( i -> types[i].equals(type))
			.toArray();
	}

}

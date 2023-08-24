package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Metadata that labels and assigns types to axes.
 *
 * @author John Bogovic
 *
 */
public interface AxisMetadata {

	public Axis[] getAxes();

	public default Axis getAxis(final int i) {

		return getAxes()[i];
	}

	public default String[] getAxisLabels() {

		return Arrays.stream(getAxes()).map(Axis::getLabel).toArray(String[]::new);
	}

	public default String[] getAxisTypes() {

		return Arrays.stream(getAxes()).map(Axis::getType).toArray(String[]::new);
	}

	public default String[] getUnits() {

		return Arrays.stream(getAxes()).map(Axis::getUnit).toArray(String[]::new);
	}

	/**
	 *
	 * @param label the label
	 * @return index corresponding to that label
	 */
	public default int indexOf(final String label) {
		for (int i = 0; i < getAxisLabels().length; i++)
			if (getAxisLabels()[i].equals(label))
				return i;

		return -1;
	}

	public default int[] indexesOfType( final String type ) {
		final String[] types = getAxisTypes();
		return IntStream.range(0, getAxisTypes().length )
			.filter( i -> types[i].equals(type))
			.toArray();
	}

}

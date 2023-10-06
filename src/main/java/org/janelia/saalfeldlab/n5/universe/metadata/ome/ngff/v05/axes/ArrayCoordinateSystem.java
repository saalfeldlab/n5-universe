package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.axes;

import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;

public class ArrayCoordinateSystem extends CoordinateSystem {

	public ArrayCoordinateSystem() {

		this(5);
	}

	public ArrayCoordinateSystem(final int nd) {

		super("", arrayAxes(nd));
	}

	public ArrayCoordinateSystem(final String name, final int nd) {

		super(name, arrayAxes(nd));
	}

	@Override
	public String toString() {

		return "<array space " + numDimensions() + ">";
	}

	public static Axis[] arrayAxes(final int nd) {

		return IntStream.range(0, nd).mapToObj(i -> Axis.defaultArray(i)).toArray(Axis[]::new);
	}

}

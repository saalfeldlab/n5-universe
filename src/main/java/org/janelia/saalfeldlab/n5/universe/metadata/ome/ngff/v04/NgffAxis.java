package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;

public class NgffAxis extends Axis {

	private String name;

	private String type;

	private String unit;

	public NgffAxis(final String type, final String name, final String unit) {

		super(type, name, unit);
	}

}

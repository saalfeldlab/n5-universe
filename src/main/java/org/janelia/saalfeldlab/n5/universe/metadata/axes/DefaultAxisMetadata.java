package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;

/**
 * Default implementation of {@link AxisMetadata}.
 *
 * @author John Bogovic
 *
 */
public class DefaultAxisMetadata implements AxisMetadata, N5Metadata {

	private final String path;

	private final String[] labels;

	private final String[] types;

	private final String[] units;

	private final Axis[] axes;

	public DefaultAxisMetadata(final String path, final String[] labels, final String[] types, final String[] units ) {

		assert (labels.length == types.length);
		assert (labels.length == units.length);

		this.path = path;
		this.labels = labels;
		this.types = types;
		this.units = units;

		axes = new Axis[labels.length];
		for (int i = 0; i < labels.length; i++)
			axes[i] = new Axis(types[i], labels[i], units[i]);
	}

	@Override
	public Axis[] getAxes() {

		return axes;
	}

	@Override
	public String[] getAxisLabels() {
		return labels;
	}

	@Override
	public String[] getAxisTypes() {
		return types;
	}

	@Override
	public String[] getUnits() {
		return units;
	}

	@Override
	public String getPath() {
		return path;
	}

}

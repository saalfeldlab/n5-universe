package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.util.HashMap;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;

public class OmeNgffMultiscalesBuilder {

	int nd;
	String name = "multiscales";
	String type;
	HashMap<String,OmeNgffDataset> datasets;
	HashMap<String,N5Metadata> metadata;
	CoordinateTransformation<?>[] transforms;
	Axis[] axes;

	public OmeNgffMultiscalesBuilder() {
		datasets = new HashMap<>();
		metadata = new HashMap<>();
	}
	
	public OmeNgffMultiscalesBuilder numDimensions(int nd) {
		this.nd = nd;
		axes = AxisUtils.defaultAxes(nd);
		return this;
	}

	public OmeNgffMultiscalesBuilder axes(Axis[] axes) {
		this.nd = axes.length;
		this.axes = axes;
		return this;
	}

	public OmeNgffMultiscalesBuilder name(String name) {
		this.name = name;
		return this;
	}
	
	public OmeNgffMultiscalesBuilder type(String type) {
		this.type = type;
		return this;
	}

	public OmeNgffMultiscalesBuilder transforms(CoordinateTransformation<?>[] transforms) {
		this.transforms = transforms;
		return this;
	}

	public OmeNgffMultiscalesBuilder put(
			final String path, 
			final CoordinateTransformation<?>[] transforms) {

		final OmeNgffDataset dset = new OmeNgffDataset();
		dset.path = path;
		dset.coordinateTransformations = transforms;
		datasets.put(path, dset);
		return this;
	}

	public OmeNgffMultiScaleMetadata build() {
		final OmeNgffDataset[] dsets = datasets.entrySet().stream().map(e -> e.getValue())
				.toArray(n -> new OmeNgffDataset[n]);

		return new OmeNgffMultiScaleMetadata(nd, null, name, type, "0.4", axes, dsets, null, transforms, null);
	}

}

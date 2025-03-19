package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations.OmeNgffV06MultiScaleMetadata.OmeNgffDataset;

public class OmeNgffMultiscalesBuilder {

	int nd;
	String path = "";
	String name = "multiscales";
	String type;
	HashMap<String,OmeNgffDataset> datasets;
	HashMap<String,N5Metadata> metadata;
	List<CoordinateSystem> coordinateSystems;

	public OmeNgffMultiscalesBuilder() {
		datasets = new HashMap<>();
		coordinateSystems = new ArrayList<>();
	}
	
	public OmeNgffMultiscalesBuilder numDimensions(int nd) {
		this.nd = nd;
		return this;
	}

	public OmeNgffMultiscalesBuilder addCoordinateSystem(CoordinateSystem... coordinateSystems) {
		if( coordinateSystems.length > 0 )
			nd = coordinateSystems[0].getAxes().length;

		Arrays.stream(coordinateSystems).forEach(cs -> {
			this.coordinateSystems.add(cs);
		});
		return this;
	}

	public OmeNgffMultiscalesBuilder path(String path) {
		this.path = path;
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

	public OmeNgffMultiscalesBuilder put(
			final String path, 
			final CoordinateTransform<?>...  transforms) {

		final OmeNgffDataset dset = new OmeNgffDataset();
		dset.path = path;
		dset.coordinateTransformations = transforms;
		datasets.put(path, dset);
		return this;
	}

	public OmeNgffV06MultiScaleMetadata build() {

		final OmeNgffDataset[] dsets = datasets.entrySet().stream().map(e -> e.getValue())
				.toArray(n -> new OmeNgffDataset[n]);
		
		CoordinateSystem[] css = coordinateSystems.stream().toArray( n -> new CoordinateSystem[n]);

		return new OmeNgffV06MultiScaleMetadata(nd, path, name, type, css, dsets, null, null, null);
	}

}

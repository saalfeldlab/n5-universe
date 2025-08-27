package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06dev2;

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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06dev2.OmeNgffV06dev2MultiScaleMetadata.OmeNgffV06dev2Dataset;

public class OmeNgffV06dev2MultiscalesBuilder {

	int nd;
	String path = "";
	String name = "multiscales";
	String type;
	HashMap<String,OmeNgffV06dev2Dataset> datasets;
	HashMap<String,N5Metadata> metadata;
	List<CoordinateSystem> coordinateSystems;

	public OmeNgffV06dev2MultiscalesBuilder() {
		datasets = new HashMap<>();
		coordinateSystems = new ArrayList<>();
	}
	
	public OmeNgffV06dev2MultiscalesBuilder numDimensions(int nd) {
		this.nd = nd;
		return this;
	}

	public OmeNgffV06dev2MultiscalesBuilder addCoordinateSystem(CoordinateSystem... coordinateSystems) {
		if( coordinateSystems.length > 0 )
			nd = coordinateSystems[0].getAxes().length;

		Arrays.stream(coordinateSystems).forEach(cs -> {
			this.coordinateSystems.add(cs);
		});
		return this;
	}

	public OmeNgffV06dev2MultiscalesBuilder path(String path) {
		this.path = path;
		return this;
	}

	public OmeNgffV06dev2MultiscalesBuilder name(String name) {
		this.name = name;
		return this;
	}
	
	public OmeNgffV06dev2MultiscalesBuilder type(String type) {
		this.type = type;
		return this;
	}

	public OmeNgffV06dev2MultiscalesBuilder put(
			final String path, 
			final CoordinateTransform<?>...  transforms) {

		final OmeNgffV06dev2Dataset dset = new OmeNgffV06dev2Dataset();
		dset.path = path;
		dset.coordinateTransformations = transforms;
		datasets.put(path, dset);
		return this;
	}

	public OmeNgffV06dev2MultiScaleMetadata build() {

		final OmeNgffV06dev2Dataset[] dsets = datasets.entrySet().stream().map(e -> e.getValue())
				.toArray(n -> new OmeNgffV06dev2Dataset[n]);
		
		CoordinateSystem[] css = coordinateSystems.stream().toArray( n -> new CoordinateSystem[n]);

		return new OmeNgffV06dev2MultiScaleMetadata(nd, path, name, type, css, dsets, null, null, null);
	}

}

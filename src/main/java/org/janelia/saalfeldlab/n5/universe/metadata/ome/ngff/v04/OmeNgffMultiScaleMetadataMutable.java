package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;

public class OmeNgffMultiScaleMetadataMutable extends OmeNgffMultiScaleMetadata {

	private List<OmeNgffDataset> datasets;

	private List<DatasetAttributes> attributes;

	private List<NgffSingleScaleAxesMetadata> children;

	private Axis[] axes;

	private String path;

	public OmeNgffMultiScaleMetadataMutable() {

		this("");
	}

	public OmeNgffMultiScaleMetadataMutable( final String path) {

		super(-1, MetadataUtils.normalizeGroupPath(path), null, null, null, null, new OmeNgffDataset[]{}, new DatasetAttributes[]{}, null, null, false);

		setPath( super.basePath );
		datasets = new ArrayList<>();
		attributes = new ArrayList<>();
		children = new ArrayList<>();
	}

	@Override
	public String getPath() {

		return path;
	}

	public void setPath(final String path) {

		this.path = path;
	}

	public void addChild(final NgffSingleScaleAxesMetadata child) {

		addChild(-1, child);
	}

	public void addChild(final int idx, final NgffSingleScaleAxesMetadata child) {

		final OmeNgffDataset dset = new OmeNgffDataset();
		// paths are relative to this object
		try {
			dset.path = N5URI.normalizeGroupPath(
					N5URI.from("", getPath(), "").resolve(N5URI.from("", child.getPath(), ""))
					.getGroupPath());
		} catch (URISyntaxException e) { }

		dset.coordinateTransformations = child.getCoordinateTransformations();
		if (idx < 0)
		{
			children.add(child);
			attributes.add(child.getAttributes());
			datasets.add( dset );
		}
		else
		{
			children.add(idx, child);
			attributes.add(idx,child.getAttributes());
			datasets.add(idx, dset );
		}
	}

	public void clear() {

		datasets.clear();
		attributes.clear();
		children.clear();
	}

	@Override
	public NgffSingleScaleAxesMetadata[] getChildrenMetadata()
	{
		if( children == null )
			return null;

		return children.toArray(new NgffSingleScaleAxesMetadata[children.size()]);
	}

	@Override
	public OmeNgffDataset[] getDatasets()
	{
		return datasets.toArray(new OmeNgffDataset[datasets.size()]);
	}

	@Override
	public Axis[] getAxes()
	{
		if( children.size() > 0 )
			return children.get(0).getAxes();

		return null;
	}

}

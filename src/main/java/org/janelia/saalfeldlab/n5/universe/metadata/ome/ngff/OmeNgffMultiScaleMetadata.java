package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;

public class OmeNgffMultiScaleMetadata extends SpatialMultiscaleMetadata<NgffSingleScaleAxesMetadata> {

	// may be the empty string to indicate unknown version
	// or that the version is specified elsewhere
	public final String version;

	public final String name;
	public final String type;
	public final Axis[] axes;
	public final OmeNgffDataset[] datasets;
	public final CoordinateSystem[] coordinateSystems;
	public final CoordinateTransform<?>[] coordinateTransformations;
	public final OmeNgffDownsamplingMetadata metadata;

	public transient DatasetAttributes[] childrenAttributes;

	public OmeNgffMultiScaleMetadata( final OmeNgffMultiScaleMetadata other, final NgffSingleScaleAxesMetadata[] children )
	{
		super( MetadataUtils.normalizeGroupPath(other.getPath()), children );
		this.name = other.name;
		this.type = other.type;
		this.version = other.version;
		this.axes = other.axes;

		final OmeNgffDataset[] dset = other.datasets;
		this.datasets = dset != null ? dset : other.datasets;
		this.coordinateTransformations = other.coordinateTransformations;
		this.coordinateSystems = other.coordinateSystems;

		this.metadata = other.metadata;
		this.childrenAttributes = other.childrenAttributes;
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, 
			final CoordinateSystem[] coordinateSystems,
			final CoordinateTransform<?>[] coordinateTransformations,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata) {

		super( MetadataUtils.normalizeGroupPath(path), 
				buildMetadata(nd, path, datasets, childrenAttributes, coordinateTransformations, metadata, axes));
		
		if (datasets != null) {
			this.datasets = relativizeDatasets(path, datasets);
		} else {
			final OmeNgffDataset[] dset = buildDatasets(getPath(), getChildrenMetadata());
			this.datasets = dset != null ? dset : datasets;
		}

		this.name = name;
		this.type = type;
		this.version = version;
		this.axes = axes;
		this.coordinateSystems = coordinateSystems;
		this.coordinateTransformations = coordinateTransformations;
		this.metadata = metadata;
		this.childrenAttributes = childrenAttributes;
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, 
			final CoordinateTransform<?>[] coordinateTransformations,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata) {

		this(nd, path, name, type, version, axes,
				datasets, null, coordinateTransformations,
				childrenAttributes, metadata);
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, 
			final CoordinateSystem[] coordinateSystems,
			final CoordinateTransform<?>[] coordinateTransformations,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata,
			final NgffSingleScaleAxesMetadata[] childrenMetadata) {

		super( MetadataUtils.normalizeGroupPath(path), childrenMetadata);
		
		if (datasets != null) {
			this.datasets = relativizeDatasets(path, datasets);
		} else {
			final OmeNgffDataset[] dset = buildDatasets(getPath(), getChildrenMetadata());
			this.datasets = dset != null ? dset : datasets;
		}

		this.name = name;
		this.type = type;
		this.version = version;
		this.axes = axes;
		this.coordinateSystems = coordinateSystems;
		this.coordinateTransformations = coordinateTransformations;
		this.metadata = metadata;
		this.childrenAttributes = childrenAttributes;
	}
	
	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, 
			final CoordinateTransform<?>[] coordinateTransformations,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata,
			final NgffSingleScaleAxesMetadata[] childrenMetadata) {

		this( nd, path, name, type, version, axes,
				datasets, null, coordinateTransformations, childrenAttributes, metadata,
				childrenMetadata);
	}
	
	private OmeNgffDataset[] relativizeDatasets(final String path, OmeNgffDataset[] datasets) {

		final URI bu = URI.create(path);
		final OmeNgffDataset[] dsetsOut = new OmeNgffDataset[datasets.length];
		for (int i = 0; i < datasets.length; i++) {

			final OmeNgffDataset dset = datasets[i];
			final URI du = URI.create(dset.path);
			final OmeNgffDataset relDset = new OmeNgffDataset();
			relDset.path = bu.relativize(du).toString();
			relDset.coordinateTransformations = dset.coordinateTransformations;
			dsetsOut[i] = relDset;
		}
		return dsetsOut;
	}

	public Axis[] getAxes() {

		return axes;
	}

	public N5SingleScaleMetadata buildChild( final int nd, final N5DatasetMetadata datasetMeta )
	{
		final AffineTransform3D id = new AffineTransform3D();
		final double[] pixelRes = DoubleStream.of( 1 ).limit( nd ).toArray();
		final double[] offset = DoubleStream.of( 0 ).limit( nd ).toArray();

		final double[] factors;
		if ( metadata == null || metadata.scale == null )
			factors = DoubleStream.of( 2 ).limit( nd ).toArray();
		else
			factors = metadata.scale;

		final N5SingleScaleMetadata childrenMetadata = new N5SingleScaleMetadata( getPath(), id, factors, pixelRes, offset, "pixel", datasetMeta.getAttributes() );
		return childrenMetadata;
	}
	
	private static OmeNgffDataset[] buildDatasets( final String path, final NgffSingleScaleAxesMetadata[] children) {

		if( children == null )
			return null;

		final OmeNgffDataset[] datasets = new OmeNgffDataset[ children.length ];
		for( int i = 0; i < children.length; i++ )
		{
			datasets[i] = new OmeNgffDataset();
			datasets[i].path = MetadataUtils.relativePath(path, children[i].getPath());
			datasets[i].coordinateTransformations = children[i].getCoordinateTransformations();

		}
		return datasets;
	}
	
	public static NgffSingleScaleAxesMetadata[] buildMetadata( final int nd, final String path,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffMultiScaleMetadata multiscales) {

		return buildMetadata(nd, path, multiscales.datasets, childrenAttributes, multiscales.coordinateTransformations, multiscales.metadata,
				multiscales.axes);
	}
	
	public static NgffSingleScaleAxesMetadata[] buildMetadata(
			final int nd, final String path, final OmeNgffDataset[] datasets,
			final DatasetAttributes[] childrenAttributes,
			final CoordinateTransform<?>[] transforms,
			final OmeNgffDownsamplingMetadata metadata,
			final Axis[] axes)
	{
			
		return buildMetadata( nd, path, datasets, 
				childrenAttributes, 
				transforms,
				metadata,
				axes, 
				false);
	}

	public static NgffSingleScaleAxesMetadata[] buildMetadata(
			final int nd, final String path, final OmeNgffDataset[] datasets,
			final DatasetAttributes[] childrenAttributes,
			final CoordinateTransform<?>[] transforms,
			final OmeNgffDownsamplingMetadata metadata,
			final Axis[] axes, 
			boolean reverseParameters)
	{
		final String normPath = MetadataUtils.normalizeGroupPath(path);
		final int N = datasets.length;
		final Axis[] axesToWrite = reverseParameters ? MetadataUtils.reversedCopy(axes) : axes;

		final NgffSingleScaleAxesMetadata[] childrenMetadata = new NgffSingleScaleAxesMetadata[ N ];
		for ( int i = 0; i < N; i++ )
		{
			AffineGet affineTransform = tranformsToAffine(datasets[i], transforms);
			if( affineTransform == null )
				affineTransform = new AffineTransform( nd );

			final double[] offset = DoubleStream.generate( () -> 0 ).limit( nd ).toArray();
			offsetFromAffine(affineTransform, offset);

			final double[] scale = DoubleStream.generate( () -> 1 ).limit( nd ).toArray();
			scaleFromAffine(affineTransform, scale);

			NgffSingleScaleAxesMetadata meta;
			if (childrenAttributes == null) {
				meta = new NgffSingleScaleAxesMetadata(MetadataUtils.canonicalPath(normPath, datasets[i].path),
						scale, offset, axesToWrite, null);
			} else {
				meta = new NgffSingleScaleAxesMetadata(MetadataUtils.canonicalPath(normPath, datasets[i].path),
						scale, offset, axesToWrite, childrenAttributes[i]);
			}
			childrenMetadata[i] = meta;
		}
		return childrenMetadata;
	}

	public NgffSingleScaleAxesMetadata[] buildChildren( final int nd,
			final DatasetAttributes[] datasetAttributes,
			final CoordinateTransform<?>[] coordinateTransformations,
			final Axis[] axes)
	{
		return buildMetadata(nd, getPath(), datasets, datasetAttributes, coordinateTransformations, metadata, axes);
	}
	
	public NgffSingleScaleAxesMetadata[] buildChildren()
	{
		return buildMetadata( axes.length, getPath(), datasets,
				childrenAttributes, coordinateTransformations,
				metadata, axes );
	}

	public OmeNgffDataset[] getDatasets() {
		return datasets;
	}

	public CoordinateSystem[] getCoordinateSystems() {
		return coordinateSystems;
	}

	public CoordinateTransform<?>[] getCoordinateTransformations() {
		return coordinateTransformations;
	}

	public String[] getCanonicalPaths() {

		return Arrays.stream(getPaths()).map((x) -> {
			if (x.startsWith("/"))
				return x;
			 else {
				N5URI url;
				try {
					url = new N5URI("?" + getPath() + "/" + x);
				} catch (final URISyntaxException e) {
					return null;
				}
				return url.getGroupPath();
			}
		}).toArray(String[]::new);
	}

	@Override
	public NgffSingleScaleAxesMetadata[] getChildrenMetadata()
	{
		return this.childrenMetadata;
	}

	@Override
	public String[] units()
	{
		return Arrays.stream( axes ).map( x -> x.getUnit() ).toArray( String[]::new );
	}

	public static class OmeNgffDataset {
		public String path;
		public CoordinateTransform<?>[] coordinateTransformations;
	}

	public static class OmeNgffDownsamplingMetadata {
		public int order;
		public boolean preserve_range;
		public double[] scale;
		public String method;
		public String version;
		public String args;
		public JsonObject kwargs;
	}

	public static boolean cOrder(final DatasetAttributes datasetAttributes) {

		if (datasetAttributes instanceof ZarrDatasetAttributes) {
			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return zattrs.isRowMajor();
		}
		return false;
	}

	public static boolean fOrder(final DatasetAttributes datasetAttributes) {

		if (datasetAttributes instanceof ZarrDatasetAttributes) {
			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return !zattrs.isRowMajor();
		}
		return false;
	}

	public static boolean allSameAxisOrder(final DatasetAttributes[] multiscaleDatasetAttributes) {

		if (multiscaleDatasetAttributes == null)
			return true;

		boolean unknown = true;
		boolean cOrder = true;
		for (final DatasetAttributes ds : multiscaleDatasetAttributes) {
			if (ds instanceof ZarrDatasetAttributes) {
				final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)ds;

				if (unknown) {
					cOrder = zattrs.isRowMajor();
					unknown = true;
				} else if (cOrder != zattrs.isRowMajor()) {
					return false;
				}
			}
		}
		return true;
	}

	public static Axis[] defaultAxes() {
		return AxisUtils.defaultAxes("x", "y", "z", "c", "t");
	}

	private static void offsetFromAffine(final AffineGet affine, final double[] offset) {

		final int nd = affine.numTargetDimensions();
		for( int i = 0; i < nd; i++ )
			offset[i] = affine.get(i, nd);
	}

	private static void scaleFromAffine(final AffineGet affine, final double[] scale) {

		final int nd = affine.numTargetDimensions();
		for (int i = 0; i < nd; i++)
			scale[i] = affine.get(i, i);
	}
	
	private static AffineGet tranformsToAffine(final OmeNgffDataset dataset, final CoordinateTransform<?>[] transforms )
	{
		Stream<CoordinateTransform<?>> s = Stream.empty();
		if( dataset.coordinateTransformations != null )
			s = Streams.concat(s, Arrays.stream(dataset.coordinateTransformations));

		if( transforms != null )
			s = Streams.concat(s, Arrays.stream(transforms));

		return buildTransform(s.toArray(CoordinateTransformation[]::new));
	}
	
	private static AffineGet buildTransform( final CoordinateTransformation<?>[] transforms ) {

		AffineTransform out = null;
		for( final CoordinateTransformation<?> ct : transforms )
		{
			if (out == null)
				out = new AffineTransform(ct.getTransform().numSourceDimensions());

			out.preConcatenate(ct.getTransform());
		}

		if( out == null )
			return null;
		else
			return TransformUtils.simplifyAffineGet( out );
	}
}

/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDownsamplingMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * The multiscales metadata for the OME NGFF specification.
 * <p>
 * See <a href="https://ngff.openmicroscopy.org/0.3/#multiscale-md">https://ngff.openmicroscopy.org/0.4/#multiscale-md</a>
 *
 * @author John Bogovic
 */
public class OmeNgffV06MultiScaleMetadata extends SpatialMultiscaleMetadata<NgffV06SingleScaleAxesMetadata> implements AxisMetadata {

	public final String name;
	public final String type;
	public final CoordinateSystem[] coordinateSystems;
	public final OmeNgffDataset[] datasets;
	public final OmeNgffDownsamplingMetadata metadata;
	public final CoordinateTransform<?>[] coordinateTransformations;

	public transient DatasetAttributes[] childrenAttributes;

	public OmeNgffV06MultiScaleMetadata( final OmeNgffV06MultiScaleMetadata other, final NgffV06SingleScaleAxesMetadata[] children )
	{
		super( MetadataUtils.normalizeGroupPath(other.getPath()), children );
		this.name = other.name;
		this.type = other.type;
		this.coordinateSystems = other.coordinateSystems;

		final OmeNgffDataset[] dset = buildDatasets( other.getPath(), getChildrenMetadata() );
		this.datasets = dset != null ? dset : other.datasets;

		this.coordinateTransformations = other.coordinateTransformations;
		this.metadata = other.metadata;
		this.childrenAttributes = other.childrenAttributes;
	}

	public OmeNgffV06MultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final CoordinateSystem[] coordinateSystems,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransform<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata) {

		this(nd, path, name, type, coordinateSystems, datasets, childrenAttributes, coordinateTransformations, metadata, true);
	}

	public OmeNgffV06MultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final CoordinateSystem[] coordinateSystems,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransform<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata,
			final boolean buildDatasetsFromChildren) {

		super( MetadataUtils.normalizeGroupPath(path), 
				buildMetadata(nd, path, datasets, childrenAttributes, coordinateTransformations, metadata, coordinateSystems));

		this.name = name;
		this.type = type;
		this.coordinateSystems = coordinateSystems;

		if( buildDatasetsFromChildren )
		{
			final OmeNgffDataset[] dset = buildDatasets( getPath(), getChildrenMetadata() );
			this.datasets = dset != null ? dset : datasets;
		}
		else
			this.datasets = datasets;

		this.coordinateTransformations = coordinateTransformations;
		this.metadata = metadata;
		this.childrenAttributes = childrenAttributes;
	}

	public static NgffV06SingleScaleAxesMetadata[] buildMetadata( final int nd, final String path,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffV06MultiScaleMetadata multiscales) {

		return buildMetadata(nd, path, multiscales.datasets, childrenAttributes, multiscales.coordinateTransformations, multiscales.metadata,
				multiscales.coordinateSystems);
	}

	private static OmeNgffDataset[] buildDatasets( final String path, final NgffV06SingleScaleAxesMetadata[] children) {

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

	public static NgffV06SingleScaleAxesMetadata[] buildMetadata(
			final int nd, final String path, final OmeNgffDataset[] datasets,
			final DatasetAttributes[] childrenAttributes,
			final CoordinateTransform<?>[] transforms,
			final OmeNgffDownsamplingMetadata metadata,
			final CoordinateSystem[] coordinateSystems)
	{
		final String normPath = MetadataUtils.normalizeGroupPath(path);
		final int N = datasets.length;
		final CoordinateSystem[] axesToWrite = coordinateSystems;

		final NgffV06SingleScaleAxesMetadata[] childrenMetadata = new NgffV06SingleScaleAxesMetadata[ N ];
		for ( int i = 0; i < N; i++ )
		{
			final NgffV06SingleScaleAxesMetadata meta = new NgffV06SingleScaleAxesMetadata(
					MetadataUtils.canonicalPath(normPath, datasets[i].path), datasets[i].coordinateTransformations,
					axesToWrite, childrenAttributes == null ? null : childrenAttributes[i]);

			childrenMetadata[i] = meta;
		}
		return childrenMetadata;
	}

	public CoordinateSystem[] getCoordinateSystems() {

		return coordinateSystems;
	}

	public NgffV06SingleScaleAxesMetadata[] buildChildren( final int nd,
			final DatasetAttributes[] datasetAttributes,
			final CoordinateTransform<?>[] coordinateTransformations,
			final CoordinateSystem[] coordinateSystems)
	{
		return buildMetadata(nd, getPath(), datasets, datasetAttributes, coordinateTransformations, metadata, coordinateSystems);
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

	@Override
	public String[] getPaths() {
		return Arrays.stream( datasets ).map( x -> { return x.path; }).toArray( String[]::new );
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
	public NgffV06SingleScaleAxesMetadata[] getChildrenMetadata() {
		return this.childrenMetadata;
	}

	@Override
	public String[] units() {

		// TODO do better than using the first coordinate system
		return Arrays.stream(coordinateSystems[0].getAxes()).map(x -> x.getUnit()).toArray(String[]::new);
	}

	public OmeNgffDataset[] getDatasets() {
		return datasets;
	}

	public static class OmeNgffDataset {
		public String path;
		public CoordinateTransform<?>[] coordinateTransformations;
	}

	public static OmeNgffMultiscalesBuilder builder() {
		return new OmeNgffMultiscalesBuilder();
	}

	@Override
	public Axis[] getAxes() {
		return coordinateSystems[0].getAxes();
	}

}

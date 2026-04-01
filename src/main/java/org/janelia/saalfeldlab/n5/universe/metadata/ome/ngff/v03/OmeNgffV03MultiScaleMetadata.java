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
package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.MultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;

import com.google.gson.JsonObject;

/**
 * The multiscales metadata for the OME NGFF specification.
 * <p>
 * See <a href="https://ngff.openmicroscopy.org/0.3/#multiscale-md">https://ngff.openmicroscopy.org/0.3/#multiscale-md</a>
 * 
 * @author John Bogovic
 */
public class OmeNgffV03MultiScaleMetadata extends OmeNgffMultiScaleMetadata {

	public OmeNgffV03MultiScaleMetadata( final OmeNgffV03MultiScaleMetadata other, final NgffV03SingleScaleAxesMetadata[] children )
	{
		super( MetadataUtils.normalizeGroupPath(other.getPath()), children );

		this.name = other.name;
		this.type = other.type;
		this.version = other.version;
		this.axes = other.axes;

		final OmeNgffDataset[] dset = buildDatasets( other.getPath(), getChildrenMetadata() );
		this.datasets = dset != null ? dset : other.datasets;

		this.metadata = other.metadata;
		this.childrenAttributes = other.childrenAttributes;
	}

	public OmeNgffV03MultiScaleMetadata( final int nd, final String path, final String name,
			final String type, final String version, final String[] axes,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata )
	{
		super( path, buildMetadata( nd, path, datasets, axes, childrenAttributes, metadata ) );
		this.name = name;
		this.type = type;
		this.version = version;
		this.axes = axes;
		this.datasets = datasets;
		this.childrenAttributes = childrenAttributes;
		this.metadata = metadata;
	}

	public static NgffV03SingleScaleAxesMetadata[] buildMetadata( 
			final int nd, final String path, 
			final OmeNgffDataset[] datasets, final String[] axes,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata )
	{
		final int N = datasets.length;

		final double[] factors;
		if ( metadata == null || metadata.scale == null )
			factors = DoubleStream.generate( () -> 2 ).limit( nd ).toArray();
		else
			factors = metadata.scale;

		final NgffV03SingleScaleAxesMetadata[] childrenMetadata = new NgffV03SingleScaleAxesMetadata[ N ];
		for ( int i = 0; i < N; i++ )
		{
			final double[] factorsi = MetadataUtils.pow(factors, i);
			childrenMetadata[i] = new NgffV03SingleScaleAxesMetadata(
					MetadataUtils.canonicalPath(path, datasets[i].path), factorsi, childrenAttributes[i]);
		}
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
		}
		return datasets;
	}

	public NgffSingleScaleAxesMetadata[] buildChildren( final int nd, DatasetAttributes[] datasetAttributes )
	{
		return buildMetadata( nd, path, datasets, axes, datasetAttributes, metadata );
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
					url = new N5URI("?" + this.path + "/" + x);
				} catch (URISyntaxException e) {
					return null;
				}
				return url.getGroupPath();
			}
		}).toArray(String[]::new);
	}

	@Override
	public String getPath()
	{
		return path;
	}

	@Override
	public String[] units()
	{
		return Arrays.stream( datasets ).map( x -> "pixel" ).toArray( String[]::new );
	}


}

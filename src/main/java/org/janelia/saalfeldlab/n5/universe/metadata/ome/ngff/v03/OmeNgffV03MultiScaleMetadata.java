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

import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;

/**
 * The multiscales metadata for the OME NGFF specification.
 * <p>
 * See <a href="https://ngff.openmicroscopy.org/0.3/#multiscale-md">https://ngff.openmicroscopy.org/0.3/#multiscale-md</a>
 * 
 * @author John Bogovic
 */
public class OmeNgffV03MultiScaleMetadata extends OmeNgffMultiScaleMetadata {
	
	private OmeNgffV03Dataset[] datasets;

	public OmeNgffV03MultiScaleMetadata( final OmeNgffV03MultiScaleMetadata other, final NgffSingleScaleAxesMetadata[] children )
	{
		super( other, children );
	}
	
	public OmeNgffV03MultiScaleMetadata( final int nd, final String path, final String name,
			final String type, final Axis[] axes,
			final OmeNgffV03Dataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata)
	{
		super(nd, path, name, type, "0.3", axes,
//				datasets, 
				childrenAttributes, metadata,
				buildMetadata(nd, path, datasets, childrenAttributes, metadata));

		this.datasets = datasets;
		
	}


	public OmeNgffV03MultiScaleMetadata( final int nd, final String path, final String name,
			final String type, final String[] axes,
			final OmeNgffV03Dataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata)
	{
		super(nd, path, name, type, "0.3", AxisUtils.defaultAxes(axes),
//				datasets, 
				childrenAttributes, metadata,
				buildMetadata(nd, path, datasets, childrenAttributes, metadata));

		this.datasets = datasets;
		
	}

	public static NgffSingleScaleAxesMetadata[] buildMetadata( 
			final int nd, final String path, 
			final OmeNgffV03Dataset[] datasets,
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

	@Override
	public String[] getPaths() {
		return Arrays.stream( datasets ).map( x -> { return x.path; }).toArray( String[]::new );
	}

	@Override
	public OmeNgffV03Dataset[] getDatasets() {
		return datasets;
	}

	@Override
	public CoordinateTransformation<?>[] getCoordinateTransformations() {
		return null;
	}

	public static class OmeNgffV03Dataset implements OmeNgffDataset {
		public String path;

		@Override
		public String getPath() {
			return path;
		}
	}

}

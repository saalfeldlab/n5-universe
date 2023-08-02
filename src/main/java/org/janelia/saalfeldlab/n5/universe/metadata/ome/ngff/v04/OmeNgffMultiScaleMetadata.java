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
package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TransformationUtils;

import com.google.gson.JsonObject;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * The multiscales metadata for the OME NGFF specification.
 * <p>
 * See <a href="https://ngff.openmicroscopy.org/0.3/#multiscale-md">https://ngff.openmicroscopy.org/0.4/#multiscale-md</a>
 *
 * @author John Bogovic
 */
public class OmeNgffMultiScaleMetadata {

	public final String name;
	public final String type;
	public final String version;
	public final Axis[] axes;
	public final OmeNgffDataset[] datasets;
	public final OmeNgffDownsamplingMetadata metadata;
	public final CoordinateTransformation<?>[] coordinateTransformations;

	public transient String path;
	public transient N5SingleScaleMetadata[] childrenMetadata;
	public transient DatasetAttributes[] childrenAttributes;

	public OmeNgffMultiScaleMetadata( final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata )
	{

		this.name = name;
		this.type = type;
		this.version = version;
		this.axes = axes;
		this.datasets = datasets;
		this.coordinateTransformations = coordinateTransformations;
		this.metadata = metadata;
		this.childrenAttributes = childrenAttributes;
		this.childrenMetadata = buildMetadata( nd, path, datasets, childrenAttributes, coordinateTransformations, metadata, axes );
	}

	public static N5SingleScaleMetadata[] buildMetadata( final int nd, final String path,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffMultiScaleMetadata multiscales ) {

		return buildMetadata(nd, path, multiscales.datasets, childrenAttributes, multiscales.coordinateTransformations, multiscales.metadata, multiscales.axes);
	}

	private static N5SingleScaleMetadata[] buildMetadata(
			final int nd, final String path, final OmeNgffDataset[] datasets,
			final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] transforms,
			final OmeNgffDownsamplingMetadata metadata,
			final Axis[] axes )
	{
		final int N = datasets.length;
		final double[] dsFactors = DoubleStream.generate( () -> 1 ).limit( nd ).toArray();

		final N5SingleScaleMetadata[] childrenMetadata = new N5SingleScaleMetadata[ N ];
		for ( int i = 0; i < N; i++ )
		{
			final AffineTransform3D affineTransform = TransformationUtils.tranformsToAffine(datasets[i], transforms);
			final double[] offset = DoubleStream.generate( () -> 0 ).limit( nd ).toArray();
			offsetFromAffine(affineTransform, offset);

			final double[] scale = DoubleStream.generate( () -> 1 ).limit( nd ).toArray();
			scaleFromAffine(affineTransform, scale);

			N5SingleScaleMetadata meta;
			if( childrenAttributes == null )
			{
				meta = new N5SingleScaleMetadata(MetadataUtils.canonicalPath(path, datasets[i].path),
						affineTransform, dsFactors, scale, offset, axes[0].getUnit(), null );
			}
			else {
				meta = new N5SingleScaleMetadata(MetadataUtils.canonicalPath(path, datasets[i].path),
						affineTransform, dsFactors, scale, offset, axes[0].getUnit(), childrenAttributes[i] );
			}
			childrenMetadata[i] = meta;
		}
		return childrenMetadata;
	}

	private static void offsetFromAffine(final AffineTransform3D affine, final double[] offset) {

		offset[0] = affine.get(0, 3);
		offset[1] = affine.get(1, 3);
		offset[2] = affine.get(2, 3);
	}

	private static void scaleFromAffine(final AffineTransform3D affine, final double[] scale) {

		scale[0] = affine.get(0, 0);
		scale[1] = affine.get(1, 1);
		scale[2] = affine.get(2, 2);
	}

	public N5SingleScaleMetadata[] buildChildren( final int nd,
			final DatasetAttributes[] datasetAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final Axis[] unit )
	{
		return buildMetadata(nd, path, datasets, datasetAttributes, coordinateTransformations, metadata, unit);
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

		final N5SingleScaleMetadata childrenMetadata = new N5SingleScaleMetadata( path, id, factors, pixelRes, offset, "pixel", datasetMeta.getAttributes() );
		return childrenMetadata;
	}

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
				} catch (final URISyntaxException e) {
					return null;
				}
				return url.getGroupPath();
			}
		}).toArray(String[]::new);
	}

	public N5SingleScaleMetadata[] getChildrenMetadata()
	{
		return this.childrenMetadata;
	}

	public String getPath()
	{
		return path;
	}

	public String[] units()
	{
		return Arrays.stream( axes ).map( x -> x.getUnit() ).toArray( String[]::new );
	}

	public static class OmeNgffDataset
	{
		public String path;
		public CoordinateTransformation<?>[] coordinateTransformations;
	}

	public static class OmeNgffDownsamplingMetadata
	{
		public int order;
		public boolean preserve_range;
		public double[] scale;
		public String method;
		public String version;
		public String args;
		public JsonObject kwargs;
	}
}

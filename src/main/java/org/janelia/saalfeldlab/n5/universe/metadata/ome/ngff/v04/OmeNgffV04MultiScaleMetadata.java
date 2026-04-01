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

import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TransformationUtils;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * The multiscales metadata for the OME NGFF specification.
 * <p>
 * See <a href="https://ngff.openmicroscopy.org/0.3/#multiscale-md">https://ngff.openmicroscopy.org/0.4/#multiscale-md</a>
 *
 * @author John Bogovic
 */
public class OmeNgffV04MultiScaleMetadata extends OmeNgffMultiScaleMetadata {

	public final CoordinateTransformation<?>[] coordinateTransformations;
	public final OmeNgffV04Dataset[] datasets;

	public OmeNgffV04MultiScaleMetadata( final OmeNgffV04MultiScaleMetadata other, final NgffSingleScaleAxesMetadata[] children )
	{
		super(other, children);
		this.coordinateTransformations = other.coordinateTransformations;
		this.datasets = other.datasets;
	}

	public OmeNgffV04MultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final Axis[] axes,
			final OmeNgffV04Dataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata) {

		this(nd, path, name, type, axes, datasets, childrenAttributes, coordinateTransformations, metadata, true);
	}

	public OmeNgffV04MultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final Axis[] axes,
			final OmeNgffV04Dataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata,
			final boolean buildDatasetsFromChildren) {

		super(nd, path, name, type, "0.4", axes,
				datasets, childrenAttributes, metadata,
				buildMetadata(nd, path, datasets, childrenAttributes, coordinateTransformations, metadata, axes),
				buildDatasetsFromChildren);

		if( buildDatasetsFromChildren )
		{
			final OmeNgffV04Dataset[] dset = buildDatasets( getPath(), getChildrenMetadata() );
			this.datasets = dset != null ? dset : datasets;
		}
		else
			this.datasets = datasets;

		this.coordinateTransformations = coordinateTransformations;
		this.childrenAttributes = childrenAttributes;
	}

	public static NgffSingleScaleAxesMetadata[] buildMetadata( final int nd, final String path,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffV04MultiScaleMetadata multiscales) {

		return buildMetadata(nd, path, multiscales.datasets, childrenAttributes, multiscales.coordinateTransformations, multiscales.metadata,
				multiscales.axes);
	}

	private static OmeNgffV04Dataset[] buildDatasets( final String path, final NgffSingleScaleAxesMetadata[] children) {

		if( children == null )
			return null;

		final OmeNgffV04Dataset[] datasets = new OmeNgffV04Dataset[ children.length ];
		for( int i = 0; i < children.length; i++ )
		{
			datasets[i] = new OmeNgffV04Dataset();
			datasets[i].path = MetadataUtils.relativePath(path, children[i].getPath());
			datasets[i].coordinateTransformations = children[i].getCoordinateTransformations();

		}
		return datasets;
	}

	public static NgffSingleScaleAxesMetadata[] buildMetadata(
			final int nd, final String path, final OmeNgffV04Dataset[] datasets,
			final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] transforms,
			final OmeNgffDownsamplingMetadata metadata,
			final Axis[] axes)
	{
		final String normPath = MetadataUtils.normalizeGroupPath(path);
		final int N = datasets.length;
		final Axis[] axesToWrite = axes;

		final NgffSingleScaleAxesMetadata[] childrenMetadata = new NgffSingleScaleAxesMetadata[ N ];
		for ( int i = 0; i < N; i++ )
		{
			AffineGet affineTransform = TransformationUtils.tranformsToAffine(datasets[i], transforms);
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

	public NgffSingleScaleAxesMetadata[] buildChildren( final int nd,
			final DatasetAttributes[] datasetAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final Axis[] axes)
	{
		return buildMetadata(nd, getPath(), datasets, datasetAttributes, coordinateTransformations, metadata, axes);
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
	public OmeNgffV04Dataset[] getDatasets()
	{
		return this.datasets;
	}

	public static class OmeNgffV04Dataset extends OmeNgffDataset
	{
		public CoordinateTransformation<?>[] coordinateTransformations;
	}


}

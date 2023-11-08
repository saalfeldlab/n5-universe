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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TransformationUtils;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import com.google.gson.JsonObject;

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
public class OmeNgffMultiScaleMetadata extends SpatialMultiscaleMetadata<NgffSingleScaleAxesMetadata> {

	public final String name;
	public final String type;
	public final String version;
	public final Axis[] axes;
	public final OmeNgffDataset[] datasets;
	public final OmeNgffDownsamplingMetadata metadata;
	public final CoordinateTransformation<?>[] coordinateTransformations;

	public transient DatasetAttributes[] childrenAttributes;

	public OmeNgffMultiScaleMetadata( final OmeNgffMultiScaleMetadata other, final NgffSingleScaleAxesMetadata[] children )
	{
		super( MetadataUtils.normalizeGroupPath(other.getPath()), children );
		this.name = other.name;
		this.type = other.type;
		this.version = other.version;
		this.axes = other.axes;

		final OmeNgffDataset[] dset = buildDatasets( other.getPath(), getChildrenMetadata() );
		this.datasets = dset != null ? dset : other.datasets;

		this.coordinateTransformations = other.coordinateTransformations;
		this.metadata = other.metadata;
		this.childrenAttributes = other.childrenAttributes;
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata) {

		this(nd, path, name, type, version, axes, datasets, childrenAttributes, coordinateTransformations, metadata, true);
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final CoordinateTransformation<?>[] coordinateTransformations,
			final OmeNgffDownsamplingMetadata metadata,
			final boolean buildDatasetsFromChildren) {

		super( MetadataUtils.normalizeGroupPath(path), buildMetadata(nd, path, datasets, childrenAttributes, coordinateTransformations, metadata, axes));
		if (!allSameAxisOrder(childrenAttributes))
			throw new RuntimeException("All ome-zarr arrays must have same array order");

		this.name = name;
		this.type = type;
		this.version = version;
		this.axes = axes;

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

	public static NgffSingleScaleAxesMetadata[] buildMetadata( final int nd, final String path,
			final DatasetAttributes[] childrenAttributes,
			final OmeNgffMultiScaleMetadata multiscales) {

		return buildMetadata(nd, path, multiscales.datasets, childrenAttributes, multiscales.coordinateTransformations, multiscales.metadata,
				multiscales.axes);
	}

	private static OmeNgffDataset[] buildDatasets( final String path, final NgffSingleScaleAxesMetadata[] children) {

		if( children == null )
			return null;

		final OmeNgffDataset[] datasets = new OmeNgffDataset[ children.length ];
		for( int i = 0; i < children.length; i++ )
		{
			datasets[i] = new OmeNgffDataset();
//			final Path p = Paths.get(path);
//			final Path c = Paths.get(children[i].getPath());
//			datasets[i].path = p.relativize(c).toString();
			datasets[i].path = Paths.get(path).relativize(Paths.get(children[i].getPath())).toString();
			datasets[i].coordinateTransformations = children[i].getCoordinateTransformations();
		}
		return datasets;
	}

	public static NgffSingleScaleAxesMetadata[] buildMetadata(
			final int nd, final String path, final OmeNgffDataset[] datasets,
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

	public Axis[] getAxes() {

		return axes;
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
	public NgffSingleScaleAxesMetadata[] getChildrenMetadata()
	{
		return this.childrenMetadata;
	}
//
//	@Override
//	public String getPath()
//	{
//		return path;
//	}

	@Override
	public String[] units()
	{
		return Arrays.stream( axes ).map( x -> x.getUnit() ).toArray( String[]::new );
	}

	public OmeNgffDataset[] getDatasets()
	{
		return datasets;
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

	public static boolean cOrder( final DatasetAttributes datasetAttributes ) {

		if (datasetAttributes instanceof ZarrDatasetAttributes) {
			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return zattrs.isRowMajor();
		}
		return false;
	}

	public static <T> T[] reverseIfCorder( final DatasetAttributes datasetAttributes, final T[] arr ) {

		if (datasetAttributes == null || arr == null)
			return arr;

		if (datasetAttributes instanceof ZarrDatasetAttributes) {

			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return reverseIfCorder(zattrs.isRowMajor(), arr);
		}
		return arr;
	}

	public static <T> T[] reverseIfCorder( final boolean cOrder, final T[] arr ) {

		if (arr == null)
			return arr;

		if (cOrder) {
			final T[] arrCopy = Arrays.copyOf(arr, arr.length);
			ArrayUtils.reverse(arrCopy);
			return arrCopy;
		}
		return arr;
	}

	public static double[] reverseIfCorder(final DatasetAttributes[] datasetAttributes, final boolean cOrder, final double[] arr) {

		if( arr == null )
			return null;

		if (datasetAttributes == null || datasetAttributes.length == 0)
			return reverseIfCorder(cOrder, arr) ;

		if (datasetAttributes[0] instanceof ZarrDatasetAttributes) {

			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes[0];
			return reverseIfCorder(zattrs.isRowMajor(), arr);
		}
		return arr;
	}

	public static double[] reverseIfCorder(final DatasetAttributes datasetAttributes, final double[] arr) {

		if (datasetAttributes == null || arr == null)
			return arr;

		if (datasetAttributes instanceof ZarrDatasetAttributes) {

			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return reverseIfCorder(zattrs.isRowMajor(), arr);
		}
		return arr;
	}

	public static double[] reverseIfCorder(final boolean cOrder, final double[] arr) {

		if (cOrder) {
			final double[] arrCopy = Arrays.copyOf(arr, arr.length);
			ArrayUtils.reverse(arrCopy);
			return arrCopy;
		}

		return arr;
	}


	public static double[] reverseCopy(final double[] arr) {
		final double[] arrCopy = Arrays.copyOf(arr, arr.length);
		ArrayUtils.reverse(arrCopy);
		return arrCopy;
	}

	public static <T> T[] reverseCopy(final T[] arr) {
		final T[] arrCopy = Arrays.copyOf(arr, arr.length);
		ArrayUtils.reverse(arrCopy);
		return arrCopy;
	}

	public static boolean allSameAxisOrder(final DatasetAttributes[] multiscaleDatasetAttributes) {

		if( multiscaleDatasetAttributes == null )
			return true;

		boolean unknown = true;
		boolean cOrder = true;
		for (final DatasetAttributes ds : multiscaleDatasetAttributes) {
			if (ds instanceof ZarrDatasetAttributes) {
				final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)ds;

				if (unknown) {
					cOrder = zattrs.isRowMajor();
					unknown = true;
				} else if ( cOrder != zattrs.isRowMajor() ){
					return false;
				}
			}
		}
		return true;

	}

}

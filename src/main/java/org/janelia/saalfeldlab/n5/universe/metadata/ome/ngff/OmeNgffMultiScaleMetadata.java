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
package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff;

import java.net.URISyntaxException;
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
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.CoordinateTransformation;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import com.google.gson.JsonObject;

import net.imglib2.realtransform.AffineTransform3D;

public class OmeNgffMultiScaleMetadata extends SpatialMultiscaleMetadata<NgffSingleScaleAxesMetadata> {

	public final String name;
	public final String type;
	public final String version;
	public final Axis[] axes;
	public final OmeNgffDataset[] datasets;
	public final OmeNgffDownsamplingMetadata metadata;

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

		this.metadata = other.metadata;
		this.childrenAttributes = other.childrenAttributes;
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata,
			final NgffSingleScaleAxesMetadata[] childrenMetadata) {

		this(nd, path, name, type, version, axes, datasets, childrenAttributes, metadata, childrenMetadata, true);
	}

	public OmeNgffMultiScaleMetadata(final int nd, final String path, final String name,
			final String type, final String version, final Axis[] axes,
			final OmeNgffDataset[] datasets, final DatasetAttributes[] childrenAttributes,
			final OmeNgffDownsamplingMetadata metadata,
			final NgffSingleScaleAxesMetadata[] childrenMetadata,
			final boolean buildDatasetsFromChildren) {

		super( MetadataUtils.normalizeGroupPath(path), childrenMetadata);

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

		this.metadata = metadata;
		this.childrenAttributes = childrenAttributes;
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

	public static <T> T[] reverseIfCorder(final DatasetAttributes datasetAttributes, final T[] arr) {

		if (datasetAttributes == null || arr == null)
			return arr;

		if (datasetAttributes instanceof ZarrDatasetAttributes) {

			final ZarrDatasetAttributes zattrs = (ZarrDatasetAttributes)datasetAttributes;
			return reverseIfCorder(zattrs.isRowMajor(), arr);
		}
		return arr;
	}

	public static <T> T[] reverseIfCorder(final boolean cOrder, final T[] arr) {

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

		if (arr == null)
			return null;

		if (datasetAttributes == null || datasetAttributes.length == 0)
			return reverseIfCorder(cOrder, arr);

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

}

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
package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;

/**
 * Janelia COSEM's implementation of a {@link N5SingleScaleMetadata}.
 *
 * @see <a href=
 *      "https://www.janelia.org/project-team/cosem">https://www.janelia.org/project-team/cosem</a>
 *
 * @author John Bogovic
 */
public class N5CosemMetadata extends N5SingleScaleMetadata implements AxisMetadata {

	private final CosemTransform cosemTransformMeta;

	private final Axis[] axes;

	public N5CosemMetadata(final String path, final CosemTransform transform, final DatasetAttributes attributes) {

		super(
				path,
				transform.toAffineTransform3d(),
				new double[]{1.0, 1.0, 1.0},
				transform.fOrderedScale(),
				transform.fOrderedTranslation(),
				transform.units[0],
				attributes);

		this.cosemTransformMeta = transform;
		axes = transform.buildAxes();
	}

	public CosemTransform getCosemTransform() {

		return cosemTransformMeta;
	}

	@Override
	public String[] getAxisLabels() {

		return reverse(cosemTransformMeta.axes);
	}

	@Override
	public String[] getAxisTypes() {

		return Stream.generate(() -> "space").limit(cosemTransformMeta.scale.length)
				.toArray(String[]::new);
	}

	@Override
	public String[] getUnits() {

		return reverse(cosemTransformMeta.units);
	}

	@Override
	public Axis[] getAxes() {

		return axes;
	}

	private String[] reverse(final String[] in) {

		final String[] out = new String[in.length];
		int j = 0;
		for (int i = in.length - 1; i >= 0; i--)
			out[j++] = in[i];

		return out;
	}

	public static class CosemTransform {

		// COSEM scales and translations are in c-order
		public transient static final String KEY = "transform";
		public final String[] axes;
		public final double[] scale;
		public final double[] translate;
		public final String[] units;

		public CosemTransform(final String[] axes, final double[] scale, final double[] translate, final String[] units) {

			this.axes = axes;
			this.scale = scale;
			this.translate = translate;
			this.units = units;
		}

		public AffineGet getAffine() {

			assert (scale.length == 3 && translate.length == 3);

			// COSEM scales and translations are in c-order
			final double[] scaleRev = new double[scale.length];
			final double[] translateRev = new double[translate.length];

			int j = scale.length - 1;
			for (int i = 0; i < scale.length; i++) {
				scaleRev[i] = scale[j];
				translateRev[i] = translate[j];
				j--;
			}

			return new ScaleAndTranslation(scaleRev, translateRev);
		}

		public AffineTransform3D toAffineTransform3d() {

			assert (scale.length >= 2 && translate.length >= 2);

			final int[] spaceIndexes = new int[3];
			Arrays.fill(spaceIndexes, -1);

			// COSEM scales and translations are in c-order
			// but detect the axis types to be extra safe
			for( int i = 0; i < axes.length; i++ )
			{
				if( axes[i].equals("x"))
					spaceIndexes[0] = i;
				else if( axes[i].equals("y"))
					spaceIndexes[1] = i;
				else if( axes[i].equals("z"))
					spaceIndexes[2] = i;
			}

			final AffineTransform3D transform = new AffineTransform3D();
			for( int i = 0; i < 3; i++ )
			{
				if( spaceIndexes[i] > -1)
				{
					transform.set(scale[spaceIndexes[i]], i, i);
					transform.set(translate[spaceIndexes[i]], i, 3);
				}
			}

			return transform;
		}

		/**
		 * @return scale with fortran ordering (x, y, z)
		 */
		public double[] fOrderedScale() {

			return IntStream.range(0, scale.length).mapToDouble(i -> scale[scale.length - i - 1]).toArray();
		}

		/**
		 * @return translation with fortran ordering (x, y, z)
		 */
		public double[] fOrderedTranslation() {

			return IntStream.range(0, translate.length).mapToDouble(i -> translate[translate.length - i - 1]).toArray();
		}

		public Axis[] buildAxes() {

			final Axis[] out = new Axis[ axes.length];
			for( int i = 0; i < axes.length; i++ )
				out[i] = new Axis(AxisUtils.getDefaultType(axes[i]), axes[i], units[i]);

			return out;
		}

	}

	@Override
	public N5CosemMetadata modifySpatialTransform(final AffineGet relativeTransformation) {

		final int nd = axes.length;
		final int tformN = relativeTransformation.numDimensions();

		final AffineTransform newTransform = new AffineTransform();
		newTransform.preConcatenate(spatialTransform());
		newTransform.preConcatenate(relativeTransformation);

		final double[] newScale = new double[nd];
		final double[] newTranslation = new double[nd];
		int j = 0;
		for (int i = 0; i < nd; i++) {
			newScale[i] = newTransform.get(j, j);
			newTranslation[i] = newTransform.get(j, tformN);
			j++;
		}

		return new N5CosemMetadata(getPath(),
				new N5CosemMetadata.CosemTransform(getCosemTransform().axes, newScale, newTranslation, getCosemTransform().units),
				getAttributes());
	}

}

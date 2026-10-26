package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class InvertibleAffineCoordinateTransform extends AbstractAffineCoordinateTransform<AffineGet>
		implements InvertibleCoordinateTransform<AffineGet> {

	protected InvertibleAffineCoordinateTransform() {
		super();
	}

	protected InvertibleAffineCoordinateTransform(final InvertibleAffineCoordinateTransform other, final TransformOptions<?> copyParams) {
		super(other, copyParams);
	}

	@Override
	public InvertibleAffineCoordinateTransform copy() {

		return copy(new BasicOptions());
	}

	@Override
	public InvertibleAffineCoordinateTransform copy(final TransformOptions<?> copyParams) {

		return new InvertibleAffineCoordinateTransform(this, copyParams);
	}

	public InvertibleAffineCoordinateTransform(final AffineOptions options) {
		super(options);
	}

	public InvertibleAffineCoordinateTransform(final double[][] affine) {
		this(new AffineOptions().withAffine(affine));
	}

	public InvertibleAffineCoordinateTransform(final String name, final String inputSpace,
			final String outputSpace, final double[][] affine) {
		this(new AffineOptions().withName(name).withInput(inputSpace).withOutput(outputSpace).withAffine(affine));
	}

	public InvertibleAffineCoordinateTransform(final String name, final OmeNgffReference inputRef,
			final OmeNgffReference outputRef, final double[][] affine) {
		this(new AffineOptions().withName(name).withInput(inputRef).withOutput(outputRef).withAffine(affine));
	}

	public InvertibleAffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		this(new AffineOptions().withName(name).withInput(inputSpace).withOutput(outputSpace).withPath(path));
		bindParameters(n5, OmeNgffMetadataParser.storesDimensionsReversed(n5));
		buildTransform(getParameters());
	}

	public InvertibleAffineCoordinateTransform(final String name,
			final String inputSpace, final String outputSpace, final String path) {
		this(new AffineOptions().withName(name).withInput(inputSpace).withOutput(outputSpace).withPath(path));
	}

	public InvertibleAffineCoordinateTransform(final String name,
			final OmeNgffReference inputSpace, final OmeNgffReference outputSpace, final String path) {
		this(new AffineOptions().withName(name).withInput(inputSpace).withOutput(outputSpace).withPath(path));
	}

	@Override
	protected InvertibleAffineCoordinateTransform withPath(final String path) {

		return copy(new BasicOptions().withPath(path));
	}

	@Override
	protected AffineGet createTransform() {

		final double[] affineFlat = TransformUtils.flatten(affine);
		if (affineFlat.length == 6) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set(affineFlat);
			return tmp;
		} else if (affineFlat.length == 12) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set(affineFlat);
			return tmp;
		} else {
			int nd = (int) Math.floor(Math.sqrt(affineFlat.length));
			AffineTransform tmp = new AffineTransform(nd);
			tmp.set(affineFlat);
			return tmp;
		}
	}

}

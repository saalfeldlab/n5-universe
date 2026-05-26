package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.coordinateTransformations.TransformUtils;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.real.DoubleType;

public abstract class AbstractAffineCoordinateTransform<T extends RealTransform>
		extends AbstractParametrizedTransform<T, double[][]> {

	public static final String TYPE = "affine";
	public double[][] affine;
	public transient T transform;

	protected AbstractAffineCoordinateTransform() {
		super(TYPE);
	}

	protected AbstractAffineCoordinateTransform(final AbstractAffineCoordinateTransform<T> ct) {
		super(ct);
	}

	protected AbstractAffineCoordinateTransform(final double[][] affine) {
		super(TYPE);
		this.affine = affine;
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final String name, final String inputSpace,
			final String outputSpace, final double[][] affine) {
		super(TYPE, name, inputSpace, outputSpace);
		this.affine = affine;
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final String name, final OmeNgffReference inputRef,
			final OmeNgffReference outputRef, final double[][] affine) {
		super(TYPE, name, inputRef, outputRef);
		this.affine = affine;
		buildTransform();
	}

	protected AbstractAffineCoordinateTransform(final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(TYPE, name, path, inputSpace, outputSpace);
		buildTransform(getParameters(n5));
	}

	protected AbstractAffineCoordinateTransform(final String name,
			final int[] inputAxes, final int[] outputAxes, final double[][] affine) {
		super(TYPE, name, null, inputAxes, outputAxes);
		this.affine = affine;
	}

	protected AbstractAffineCoordinateTransform(final String name,
			final String inputSpace, final String outputSpace, final String path) {
		super(TYPE, name, path, inputSpace, outputSpace);
	}

	protected AbstractAffineCoordinateTransform(final String name,
			final OmeNgffReference input, final OmeNgffReference output, final String path) {
		super(TYPE, name, path, input, output);
	}

	@Override
	public T getTransform() {
		return transform;
	}

	public final T buildTransform(final double[][] params) {
		this.affine = params;
		return buildTransform();
	}

	public final T buildTransform() {
		if (affine == null)
			return null;
		transform = createTransform();
		return transform;
	}

	protected abstract T createTransform();

	public void write(final N5Writer n5, final String dataset) {

		final double[][] affineCorder = TransformUtils.reverseCoordinates(affine);
		ArrayImg<DoubleType, DoubleArray> img = ArrayImgs.doubles(
				TransformUtils.flatten(affineCorder),
				affineCorder[0].length, affineCorder.length);

		final int nd = img.numDimensions();
		final int[] blkSize = new int[nd];
		for (int i = 0; i < nd; i++)
			blkSize[i] = (int) img.dimension(i);

		N5Utils.save(img, n5, dataset, blkSize, new RawCompression());
	}

	@Override
	public double[][] getParameters(final N5Reader n5) {

		if (n5 == null)
			return null;

		// TODO JOHN this needs to be transposed!!! 
		final double[][] affineCOrder = getDoubleArray2(n5, getParameterPath());
		final double[][] affineCOrderT = TransformUtils.transpose(affineCOrder);
		final double[][] affineFOrder = TransformUtils.reverseCoordinates(affineCOrderT);
		return affineFOrder;
	}

}

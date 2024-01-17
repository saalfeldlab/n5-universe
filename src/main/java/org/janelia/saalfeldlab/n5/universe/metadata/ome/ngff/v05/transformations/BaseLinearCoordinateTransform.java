package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class BaseLinearCoordinateTransform<T extends AffineGet> extends AbstractLinearCoordinateTransform<T,double[]> 
	implements InvertibleCoordinateTransform<T> {

	protected JsonElement affine;

	public transient double[] affineFlat;

	public transient T transform;

	public BaseLinearCoordinateTransform(final String type) {
		super(type);
	}

	public BaseLinearCoordinateTransform( final BaseLinearCoordinateTransform<T> other ) {
		super(other.type, other.path);
		this.affine = other.affine;
		this.affineFlat = other.affineFlat;
		this.transform = other.transform;
		this.name = other.name;
		this.input = other.input;
		this.output = other.output;
	}

	public BaseLinearCoordinateTransform( final String type, final String name, final String inputSpace, final String outputSpace) {
		super(type, name, null, inputSpace, outputSpace );
	}

	public BaseLinearCoordinateTransform(final String type, final String name, final N5Reader n5, final String path,
			final String inputSpace, final String outputSpace) {
		super(type, name, path, inputSpace, outputSpace );
	}

	public BaseLinearCoordinateTransform( final String type, final String name, 
			final String[] inputAxes, final String[] outputAxes) {
		super(type, name, null, inputAxes, outputAxes  );
	}

	public BaseLinearCoordinateTransform( final String type, final String name, final String path,
			final String inputSpace, final String outputSpace) {
		super(type, name, path, inputSpace, outputSpace  );
	}

	public JsonElement getJsonParameter() {

		return affine;
	}

	public void interpretParameters() {

		if (!affine.isJsonArray())
			return;

		final JsonArray arr = affine.getAsJsonArray();
		final JsonElement e0 = affine.getAsJsonArray().get(0);
		if (e0.isJsonPrimitive()) {
			affineFlat = new double[arr.size()];
			for (int i = 0; i < arr.size(); i++)
				affineFlat[i] = arr.get(i).getAsDouble();

		} else if (e0.isJsonArray()) {

			double[][] nested = null;
			for (int row = 0; row < arr.size(); row++) {

				final JsonArray jsonRowArray = arr.get(row).getAsJsonArray();
				if (row == 0)
					nested = new double[arr.size()][jsonRowArray.size()];

				for (int col = 0; col < jsonRowArray.size(); col++)
					nested[row][col] = jsonRowArray.get(col).getAsDouble();
			}

			affineFlat = TransformUtils.flatten(nested);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T buildTransform(double[] parameters) {

		if (parameters == null)
			return null;

		if (parameters.length == 6) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set(parameters);
			transform = (T)tmp;
		} else if (parameters.length == 12) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set(parameters);
			transform = (T)tmp;
		} else {
			int nd = (int)Math.floor(Math.sqrt(parameters.length));
			AffineTransform tmp = new AffineTransform(nd);
			tmp.set(parameters);
			transform = (T)tmp;
		}
		return transform;
	}

	@Override
	public double[] getParameters(N5Reader n5) {

		if (n5 == null)
			return null;

		final double[] paramsFlat = getDoubleArray(n5, getParameterPath());
		if (paramsFlat != null)
			return paramsFlat;

		// TODO doc why flattenColMajor is needed
		final double[][] params2d = getDoubleArray2(n5, getParameterPath());
		return TransformUtils.flattenColMajor(params2d);
	}

	@Override
	public T getTransform() {

		if (affine != null && transform == null)
			buildTransform(affineFlat);
		return transform;
	}

	@Override
	public T getTransform(final N5Reader n5) {

		if (affineFlat != null)
			return getTransform();
		else if (affine != null) {
			interpretParameters();
			if (affineFlat != null)
				return getTransform();
		}

		return buildTransform(getParameters(n5));
	}

}

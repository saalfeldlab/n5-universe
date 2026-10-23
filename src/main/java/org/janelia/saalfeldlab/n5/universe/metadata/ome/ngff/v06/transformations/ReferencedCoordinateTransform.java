package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.Common;

import net.imglib2.realtransform.RealTransform;

public class ReferencedCoordinateTransform<T extends RealTransform> extends AbstractCoordinateTransform<T> {

	public static transient final String TYPE = "transformReference";

	private final String url;

	private final String type;

	private final transient CoordinateTransform ct;

	public ReferencedCoordinateTransform(final String url) {

		this.url = url;
		this.type = TYPE;
		ct = Common.openTransformN5(url).getA();
	}

	private ReferencedCoordinateTransform(final ReferencedCoordinateTransform<T> other) {

		this.url = other.url;
		this.type = other.type;
		this.ct = other.ct.copy();
	}

	@Override
	public ReferencedCoordinateTransform<T> copy() {

		return copy(new TransformOptions());
	}

	/**
	 * The name, input, and output of this transform are those of the transform it
	 * refers to, so they can not be replaced.
	 *
	 * @throws UnsupportedOperationException if any field is set in {@code copyParams}
	 */
	@Override
	public ReferencedCoordinateTransform<T> copy(final TransformOptions copyParams) {

		if (copyParams.isNameSet() || copyParams.isInputSet() || copyParams.isOutputSet() || copyParams.isPathSet())
			throw new UnsupportedOperationException(
					"The name, input, and output of a referenced transform are those of the transform it refers to,"
					+ " and it has no parameter path");

		return new ReferencedCoordinateTransform<>(this);
	}

	public String getUrl() {

		return url;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getTransform() {

		return (T)Common.open(url);
	}

	@Override
	public String getName() {

		return ct.getName();
	}

	@Override
	public String getType() {

		return ct.getType();
	}

	@Override
	public OmeNgffReference getInput() {

		return ct.getInput();
	}

	@Override
	public OmeNgffReference getOutput() {

		return ct.getOutput();
	}

}

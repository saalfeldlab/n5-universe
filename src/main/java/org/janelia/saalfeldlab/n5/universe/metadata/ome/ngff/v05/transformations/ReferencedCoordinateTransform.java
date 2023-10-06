package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.Common;

import net.imglib2.realtransform.RealTransform;

public class ReferencedCoordinateTransform<T extends RealTransform> implements RealCoordinateTransform<T> {

	public static transient final String TYPE = "transformReference";

	private final String url;

	private final String type;

	private final transient CoordinateTransform ct;

	public ReferencedCoordinateTransform(final String url) {

		this.url = url;
		this.type = TYPE;
		ct = Common.openTransformN5(url).getA();
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
	public String getInput() {

		return ct.getInput();
	}

	@Override
	public String getOutput() {

		return ct.getOutput();
	}

	@Override
	public String[] getInputAxes() {

		return ct.getInputAxes();
	}

	@Override
	public String[] getOutputAxes() {

		return ct.getOutputAxes();
	}

}

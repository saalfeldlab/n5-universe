package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractParametrizedTransform<T extends RealTransform,P> extends AbstractCoordinateTransform<T> implements ParametrizedTransform<T,P> {

	protected final String path;

	protected transient String absolutePath;

	private transient Supplier<P> parameterSupplier;

	public AbstractParametrizedTransform() {
		super();
		path = null;
	}

	protected AbstractParametrizedTransform( String type )
	{
		this(type, new BasicOptions());
	}

	protected AbstractParametrizedTransform( String type, TransformOptions<?> copyParams )
	{
		super(type, copyParams);
		this.path = copyParams.getPath();
		// a new path has to be resolved again, and the transform needs binding
		this.absolutePath = null;
		this.parameterSupplier = null;
	}

	protected AbstractParametrizedTransform( AbstractParametrizedTransform<T,P> other, TransformOptions<?> copyParams )
	{
		super( other, copyParams );
		this.path = copyParams.isPathSet() ? copyParams.getPath() : other.path;
		// a new path has to be resolved again
		this.absolutePath = copyParams.isPathSet() ? null : other.absolutePath;
		// the supplier reads at the path of this transform, so it follows the new path
		this.parameterSupplier = other.parameterSupplier;
	}

	/**
	 * Reads this transform's parameters.
	 *
	 * @param n5
	 *            the reader, never null
	 * @param path
	 *            the parameter path, never null
	 * @param reverse
	 *            whether to reverse the order of coordinates
	 * @return the parameters, or null if they could not be read
	 */
	protected abstract P readParameters(final N5Reader n5, final String path, final boolean reverse);

	/**
	 * Reads parameters lazily from the given reader, at this transform's
	 * {@link #getParameterPath() parameter path} at the time of reading.
	 *
	 * @param n5
	 *            the reader
	 * @param reverse
	 *            whether to reverse the order of coordinates
	 */
	public void bindParameters(final N5Reader n5, final boolean reverse) {

		parameterSupplier = () -> {
			final String parameterPath = getParameterPath();
			// TODO null check n5 before calling this method, dont null check here
			return (n5 == null || parameterPath == null) ? null : readParameters(n5, parameterPath, reverse);
		};
	}
	
	/**
	 * Generally, do not call this method yourself. This is made public for testing
	 * purposes.
	 *
	 * @param parameterSupplier
	 *            supplies the parameters
	 */
	public void setParameterSupplier(Supplier<P> parameterSupplier) {
		this.parameterSupplier = parameterSupplier;
	}

	/**
	 * @return true if this transform has a parameter source
	 */
	public boolean isBound() {
		return parameterSupplier != null;
	}

	/**
	 * @return the parameters, or null if no parameter source is bound
	 */
	public P getParameters() {
		return parameterSupplier == null ? null : parameterSupplier.get();
	}

	/**
	 * For building the transform from parameters stored in an array.
	 *
	 * @return the parameters, or null if they could not be read
	 * @throws N5Exception if this transform is not bound to a reader
	 */
	protected P requireParameters() {

		if (!isBound())
			throw new N5Exception("The parameters of " + getType() + " transform"
					+ (getName() != null ? " \"" + getName() + "\"" : "")
					+ " at path \"" + getParameterPath() + "\" are not bound to a reader."
					+ " Read transforms with CoordinateTransforms or an ome-ngff parser,"
					+ " or bind them with ParameterBinding.");

		return getParameters();
	}

	@Override
	public String getParameterPath() {

		return absolutePath != null ? absolutePath : path;
	}

	@Override
	public void resolveAbsolutePath(final String groupPath) {

		if (path == null)
			return;

		// a relative path is relative to the group holding the metadata,
		// a path starting with "/" is relative to the container root
		absolutePath = path.startsWith("/") || groupPath == null
				? N5URI.normalizeGroupPath(path)
				: N5URI.normalizeGroupPath(groupPath + "/" + path);
	}

}

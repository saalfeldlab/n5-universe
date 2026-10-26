package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractCoordinateTransform<T extends RealTransform> implements CoordinateTransform<T> {

	protected String type;

	protected String name;

	protected OmeNgffReference input;

	protected OmeNgffReference output;

	protected transient CoordinateSystem inputCoordinateSystem;

	protected transient CoordinateSystem outputCoordinateSystem;

	protected AbstractCoordinateTransform() {
		super();
	}
	
	public AbstractCoordinateTransform( final String type, final String name,
			final OmeNgffReference inputRef, final OmeNgffReference outputRef ) {
		this.type = type;
		this.name = name;
		this.input = inputRef;
		this.output = outputRef;
	}

	public AbstractCoordinateTransform( final String type, final String name,
			final String inputSpace, final String outputSpace ) {
		this.type = type;
		this.name = name;
		this.input = new OmeNgffReference(inputSpace);
		this.output = new OmeNgffReference(outputSpace);
	}

	public AbstractCoordinateTransform( final String type,
			final String name,
			final CoordinateSystem input, final CoordinateSystem output) {
		this.type = type;
		this.name = name;

		// Assumes these coordinate systems are in the same group
		this.inputCoordinateSystem = input;
		this.input = new OmeNgffReference(inputCoordinateSystem.getName());

		this.outputCoordinateSystem = output;
		this.output = new OmeNgffReference(outputCoordinateSystem.getName());
	}

	protected AbstractCoordinateTransform( final String type ) {
		this.type = type;
	}

	protected AbstractCoordinateTransform( CoordinateTransform<T> other )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.input = other.getInput();
		this.output = other.getOutput();
	}

	protected AbstractCoordinateTransform(String type, TransformOptions<?> options) {

		// creating a new transform: no original to fall back on, so unset fields are simply null
		this.type = type;
		this.name = options.name;
		this.input = options.input;
		this.output = options.output;
	}
	
	protected AbstractCoordinateTransform( CoordinateTransform<T> other, TransformOptions<?> options )
	{
		if (options.isPathSet() && !(other instanceof ParametrizedTransform))
			throw new UnsupportedOperationException(
					"A " + other.getType() + " transform does not have a parameter path");

		this.type = other.getType();
		this.name = options.isNameSet() ? options.name : other.getName();
		this.input = options.isInputSet() ? options.input : other.getInput();
		this.output = options.isOutputSet() ? options.output : other.getOutput();
	}

	@Override
	public abstract T getTransform();

	/**
	 * @param array the array, may be null
	 * @return a deep copy of the array, or null if the array is null
	 */
	protected static double[][] deepCopy(final double[][] array) {

		if (array == null)
			return null;

		final double[][] copy = new double[array.length][];
		for (int i = 0; i < array.length; i++)
			copy[i] = array[i] == null ? null : array[i].clone();

		return copy;
	}

	@Override
	public abstract AbstractCoordinateTransform<T> copy(TransformOptions<?> copyParams);

	@Override
	public AbstractCoordinateTransform<T> copy() {
		return copy(new BasicOptions());
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OmeNgffReference getInput() {
		return input;
	}

	@Override
	public OmeNgffReference getOutput() {
		return output;
	}

	public void setInput(final CoordinateSystem inputCoordinateSystem) {
		this.inputCoordinateSystem = inputCoordinateSystem;
	}

	public void setOutput(final CoordinateSystem outputCoordinateSystem) {
		this.outputCoordinateSystem = outputCoordinateSystem;
	}

	public CoordinateSystem getInputCoordinateSystem()
	{
		return inputCoordinateSystem;
	}

	public CoordinateSystem getOutputCoordinateSystem()
	{
		return outputCoordinateSystem;
	}

	public AbstractCoordinateTransform<T> setNameSpaces( final String name, final String in, final String out )
	{
		this.name = name;
		this.input = new OmeNgffReference(in);
		this.output = new OmeNgffReference(out);
		return this;
	}

	@Override
	public String toString()
	{
		return String.format("%s:(%s → %s)", name, input, output);
	}
	
	/**
	 * Builder/helper for copying coordinate transformations with modifications.
	 * calling a with(Field) method of this class indicates that the provided
	 * field should be modified when copying
	 * <p>
	 * For example
	 * ct.copy(new BasicOptions().withName(newName));
	 * <p>
	 * Fields that are not set are copied unchanged. A field explicitly set to
	 * null is copied as null. When the options are used to create a new transform
	 * rather than to copy one, there is nothing to copy from, so unset fields are null.
	 * <p>
	 * The parameter path can only be replaced for transforms that have one, see
	 * {@link ParametrizedTransform}. Copying any other transform with a path set
	 * throws an {@link UnsupportedOperationException}.
	 */
	public static abstract class TransformOptions<O extends TransformOptions<O>> {

		private boolean nameSet = false;
		private boolean inputSet = false;
		private boolean outputSet = false;
		private boolean pathSet = false;

		private String name;
		private OmeNgffReference input;
		private OmeNgffReference output;
		private String path;

		/**
		 * @return this, as the type of the concrete options class, so that the
		 *         with(Field) methods of subclasses can be chained in any order
		 */
		@SuppressWarnings("unchecked")
		private O self() {
			return (O) this;
		}

		public O withName(String name) {
			this.name = name;
			this.nameSet = true;
			return self();
		}

		public O withInput(OmeNgffReference input) {
			this.input = input;
			this.inputSet = true;
			return self();
		}

		public O withInput(String input) {
			return withInput(input == null ? null : new OmeNgffReference(input));
		}

		public O withOutput(OmeNgffReference output) {
			this.output = output;
			this.outputSet = true;
			return self();
		}

		public O withOutput(String output) {
			return withOutput(output == null ? null : new OmeNgffReference(output));
		}

		/**
		 * Replaces the path of the parameters, for transforms whose parameters are stored in an array.
		 * The copy keeps the parameters of the original that are already in memory, and reads
		 * from {@code path} only if it has none.
		 *
		 * @param path the parameter path, may be null
		 * @return this
		 */
		public O withPath(String path) {
			this.path = path;
			this.pathSet = true;
			return self();
		}

		public boolean isNameSet() {
			return nameSet;
		}

		public boolean isInputSet() {
			return inputSet;
		}

		public boolean isOutputSet() {
			return outputSet;
		}

		public boolean isPathSet() {
			return pathSet;
		}

		public String getPath() {
			return path;
		}

		public TransformOptions<BasicOptions> options() {
			return new BasicOptions();
		}
	}

	/**
	 * {@link TransformOptions} for transforms that have no parameters of their own to set.
	 */
	public static class BasicOptions extends TransformOptions<BasicOptions> {
	}

}

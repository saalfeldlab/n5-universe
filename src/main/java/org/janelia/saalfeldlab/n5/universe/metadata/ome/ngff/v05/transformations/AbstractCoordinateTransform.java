package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffReference;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractCoordinateTransform<T extends RealTransform> implements CoordinateTransform<T> {

	protected String type;

	protected String name;

	protected OmeNgffReference input;

	protected OmeNgffReference output;

	protected int[] inputAxes;

	protected int[] outputAxes;

	protected transient CoordinateSystem inputCoordinateSystem;

	protected transient CoordinateSystem outputCoordinateSystem;

	@Override
	public abstract T getTransform();

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

	public AbstractCoordinateTransform( final String type, final String name,
			final int[] inputAxes, final int[] outputAxes ) {
		this.type = type;
		this.name = name;
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
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

	public AbstractCoordinateTransform( final String type, final String name ) {
		this.type = type;
		this.name = name;
	}

	public AbstractCoordinateTransform( final String type ) {
		this.type = type;
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.input = other.getInput();
		this.input = other.getOutput();
		this.inputAxes = other.getInputAxes();
		this.outputAxes = other.getOutputAxes();
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other, int[] inputAxes, int[] outputAxes )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.input = other.getInput();
		this.output = other.getOutput();
		this.inputAxes = other.getInputAxes();
		this.outputAxes = other.getOutputAxes();
	}

	@Override
	public int[] getInputAxes() {
		return inputAxes;
	}

	public void setInputAxes(final int[] inputAxes) {
		this.inputAxes = inputAxes;
	}

	@Override
	public int[] getOutputAxes() {
		return outputAxes;
	}

	public void setOutputAxes(final int[] outputAxes) {
		this.outputAxes = outputAxes;
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
		return String.format("%s:(%s > %s)", name, input, output);
	}

}

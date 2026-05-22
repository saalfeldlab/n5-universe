package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractCoordinateTransform<T extends RealTransform> implements CoordinateTransform<T> {

	protected String type;

	protected String name;

	protected JsonElement input;

	protected JsonElement output;

	protected int[] inputAxes;

	protected int[] outputAxes;

	protected transient String inputCoordinateSystemName;

	protected transient String outputCoordinateSystemName;

	protected transient CoordinateSystem inputCoordinateSystem;

	protected transient CoordinateSystem outputCoordinateSystem;

	@Override
	public abstract T getTransform();

	protected AbstractCoordinateTransform() {
		super();
	}

	public AbstractCoordinateTransform( final String type,
			final String name,
			final String inputSpace, final String outputSpace ) {
		this.type = type;
		this.name = name;
		this.inputCoordinateSystemName = inputSpace;
		this.outputCoordinateSystemName = outputSpace;
		initialize();
	}

	public AbstractCoordinateTransform( final String type,
			final String name,
			final int[] inputAxes, final int[] outputAxes ) {
		this.type = type;
		this.name = name;
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
		initialize();
	}
	
	public AbstractCoordinateTransform( final String type,
			final String name,
			final CoordinateSystem input, final CoordinateSystem output) {
		this.type = type;
		this.name = name;

		this.inputCoordinateSystem = input;
		this.inputCoordinateSystemName = inputCoordinateSystem.getName();

		this.outputCoordinateSystem = output;
		this.outputCoordinateSystemName = outputCoordinateSystem.getName();
		initialize();
	}

	public AbstractCoordinateTransform( final String type, final String name ) {
		this.type = type;
		this.name = name;
		initialize();
	}

	public AbstractCoordinateTransform( final String type ) {
		this.type = type;
		initialize();
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.inputCoordinateSystemName = other.getInput();
		this.outputCoordinateSystemName = other.getOutput();
		this.inputAxes = other.getInputAxes();
		this.outputAxes = other.getOutputAxes();
		initialize();
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other, int[] inputAxes, int[] outputAxes )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.inputCoordinateSystemName = other.getInput();
		this.outputCoordinateSystemName = other.getOutput();
		this.inputAxes = other.getInputAxes();
		this.outputAxes = other.getOutputAxes();
		initialize();
	}

	protected void initialize() {

		if (this.inputCoordinateSystemName != null)
			input = new JsonPrimitive(inputCoordinateSystemName);

		if (this.outputCoordinateSystemName != null)
			output = new JsonPrimitive(outputCoordinateSystemName);
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
	public String getInput() {
		if( inputCoordinateSystemName != null)
			return inputCoordinateSystemName;
		else if( input != null && input.isJsonPrimitive() ) {
			inputCoordinateSystemName = input.getAsString();
			return inputCoordinateSystemName;
		}
			
		return null;	
	}

	@Override
	public String getOutput() {
		if( outputCoordinateSystemName != null ) 
			return outputCoordinateSystemName;
		else if( output != null && output.isJsonPrimitive() ) {
			outputCoordinateSystemName = output.getAsString();
			return outputCoordinateSystemName;
		}

		return null;	
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
		this.inputCoordinateSystemName = in;
		this.outputCoordinateSystemName = out;
		return this;
	}

	@Override
	public String toString()
	{
		return String.format("%s:(%s > %s)", name, input, output);
	}

}

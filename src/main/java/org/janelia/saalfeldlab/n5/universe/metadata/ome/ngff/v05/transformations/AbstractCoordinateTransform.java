package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;

import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;

import net.imglib2.realtransform.RealTransform;

@NameConfig.Prefix("coordinate-transform")
public abstract class AbstractCoordinateTransform<T extends RealTransform> implements CoordinateTransform<T> {

	protected String type;

	@NameConfig.Parameter(optional = true)
	protected String name;

	@NameConfig.Parameter(optional = true)
	protected String input;

	@NameConfig.Parameter(optional = true)
	protected String output;

	// implement
	protected transient String[] inputAxes;

	protected transient String[] outputAxes;

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
		this.input = inputSpace;
		this.output = outputSpace;
	}

	public AbstractCoordinateTransform( final String type,
			final String name,
			final String[] inputAxes, final String[] outputAxes ) {
		this.type = type;
		this.name = name;
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
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
		this.output = other.getOutput();
		this.inputAxes = other.getInputAxes();
		this.outputAxes = other.getOutputAxes();
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other, String[] inputAxes, String[] outputAxes )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.input = other.getInput();
		this.output = other.getOutput();
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
	}

	/**
	 *
	 * If this object does not have input_space or output_space defined,
	 * attempts to infer the space name, given * input_axes or output_axes, if they are defined.
	 *
	 *
	 * @param spaces the spaces object
	 * @return true if input_space and output_space are defined.
	 */
	public boolean inferSpacesFromAxes( final CoordinateSystems spaces )
	{
		if( input == null && outputAxes != null )
			input = spaceNameFromAxesLabels( spaces, inputAxes );

		if( output == null && outputAxes != null )
			output = spaceNameFromAxesLabels( spaces, outputAxes );

		if( input != null && output != null )
			return true;
		else
			return false;
	}

	@Override
	public String[] getInputAxes()
	{
		if (inputAxes != null)
			return inputAxes;
		else if (inputCoordinateSystem != null)
			return inputCoordinateSystem.getAxisNames();
		else
			return null;
	}

	@Override
	public String[] getOutputAxes()
	{
		if (outputAxes != null)
			return outputAxes;
		else if (outputCoordinateSystem != null)
			return outputCoordinateSystem.getAxisNames();
		else
			return null;
	}

	private static String spaceNameFromAxesLabels( final CoordinateSystems spaces, final String[] axes )
	{
		final ArrayList<CoordinateSystem> candidateSpaces = spaces.getSpacesFromAxes(axes);
		if( candidateSpaces.size() == 1 )
			return candidateSpaces.get(0).getName();
		else
			return null;
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
		return input;
	}

	@Override
	public String getOutput() {
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
		this.input = in;
		this.output = out;
		return this;
	}

	@Override
	public String toString()
	{
		return String.format("%s:(%s > %s)", name, input, output);
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;

import net.imglib2.realtransform.RealTransform;

@NameConfig.Prefix("coordinate-transform")
public abstract class AbstractCoordinateTransform<T extends RealTransform> implements CoordinateTransform<T> {

	protected String type;

	@NameConfig.Parameter(optional = true)
	protected String name;

	@NameConfig.Parameter(optional = true)
	protected JsonElement input;

	@NameConfig.Parameter(optional = true)
	protected JsonElement output;
	
	protected transient String inputCoordinateSystemName;

	protected transient String outputCoordinateSystemName;

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
		this.inputCoordinateSystemName = inputSpace;
		this.outputCoordinateSystemName = outputSpace;
		initialize();
	}

	public AbstractCoordinateTransform( final String type,
			final String name,
			final String[] inputAxes, final String[] outputAxes ) {
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

	public AbstractCoordinateTransform( CoordinateTransform<T> other, String[] inputAxes, String[] outputAxes )
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
		
		if( this.inputCoordinateSystemName != null )
			input = new JsonPrimitive( inputCoordinateSystemName );
		else if ( this.inputAxes != null )
			input = axesToJson( inputAxes );

		if( this.outputCoordinateSystemName != null )
			output = new JsonPrimitive( outputCoordinateSystemName );
		else if ( this.inputAxes != null )
			output = axesToJson( outputAxes );
	}

	private JsonArray axesToJson( final String[] axes ) {
		final JsonArray json = new JsonArray();
		Arrays.stream( axes ).forEach( a -> {
			json.add( a );
		});
		return json;
	}

	private String[] jsonToAxes( final JsonArray json ) {
		String[] axes = new String[ json.size() ];
		for ( int i = 0; i < json.size(); i++ )
			axes[ i ] = json.get( i ).getAsString();

		return axes;
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
			inputCoordinateSystemName = spaceNameFromAxesLabels( spaces, inputAxes );

		if( output == null && outputAxes != null )
			outputCoordinateSystemName = spaceNameFromAxesLabels( spaces, outputAxes );

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
		else if( input != null && input.isJsonArray() ) {
			inputAxes = jsonToAxes(input.getAsJsonArray());
			return inputAxes;
		}
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
		else if( input != null && input.isJsonArray() ) {
			outputAxes = jsonToAxes(output.getAsJsonArray());
			return outputAxes;
		}
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

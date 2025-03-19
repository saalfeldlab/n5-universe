package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.TransformUtils;
import org.janelia.saalfeldlab.n5.universe.serialization.NameConfig;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.transform.integer.MixedTransform;

@NameConfig.Name("mapAxis")
public class MapAxisCoordinateTransform extends AbstractCoordinateTransform<AffineGet> {

	public static final String TYPE = "mapAxis";

	@NameConfig.Parameter()
	protected JsonElement mapAxis;

	protected transient Map<String,String> axisMapping;
	protected transient String[] inputAxes;
	protected transient String[] outputAxes;

	protected MapAxisCoordinateTransform() {
		super(TYPE);
	}

	public MapAxisCoordinateTransform(MapAxisCoordinateTransform ct) {
		super(ct);
	}

//	public MapAxisCoordinateTransform( final String name, final String inputSpace, final String outputSpace,
//			final JsonElement mapAxis) {
//		super(TYPE, name, inputSpace, outputSpace, flattenRotationMatrix(matrix));
//		buildTransform(affineFlat);
//	}

	public MapAxisCoordinateTransform( final String name,
			final CoordinateSystem input, CoordinateSystem output,
			Map<String,String> axisMapping) {
		super(TYPE, name, input, output);

		inputAxes = input.getAxisNames();
		outputAxes = output.getAxisNames();
		this.axisMapping = axisMapping;

		validate(axisMapping, inputAxes, outputAxes);
		buildJsonParameters();
	}

	public MapAxisCoordinateTransform(final CoordinateSystem input, CoordinateSystem output,
			Map<String, String> axisMapping) {
		this(null, input, output, axisMapping);
	}

//	public MapAxisCoordinateTransform( final String name, final String path,
//			final String inputSpace, final String outputSpace) {
//		super(TYPE, name, path, inputSpace, outputSpace  );
//	}

	public JsonElement getJsonParameter() {
		return mapAxis;
	}
	
	protected void buildJsonParameters() {

		if (axisMapping != null)
			mapAxis = new Gson().toJsonTree(axisMapping);
	}
	
	protected void buildTransform() {

		if (mapAxis != null)
			axisMapping = new Gson().fromJson(mapAxis, new TypeToken<Map<String, String>>(){}.getType()); 
	}

	public Map<String,String> getAxisMapping() {
		return axisMapping;
	}

	public void setInputAxes(final String[] inputAxes) {
		this.inputAxes = inputAxes;
	}

	public void setOutputAxes(final String[] outputAxes) {
		this.outputAxes = outputAxes;
	}
	
	public void setInput(final CoordinateSystem inputCoordinateSystem) {
		super.setInput(inputCoordinateSystem);
		setInputAxes(inputCoordinateSystem.getAxisNames());
	}

	public void setOutput(final CoordinateSystem outputCoordinateSystem) {
		super.setOutput(outputCoordinateSystem);
		setOutputAxes(outputCoordinateSystem.getAxisNames());
	}

	@Override
	public AffineGet getTransform() {
		if (inputAxes == null || outputAxes == null)
			return null;

		if( axisMapping == null )
			buildTransform();

		return realTransformFromMapping(axisMapping, inputAxes, outputAxes);
	}

	public MixedTransform getDiscreteTransform() {
		if (inputAxes == null || outputAxes == null)
			return null;

		return integerTransformFromMapping(axisMapping, inputAxes, outputAxes);
	}

	private static AffineGet realTransformFromMapping(final Map<String, String> axisMapping,
			final String[] inputAxes, final String[] outputAxes) {

		if (inputAxes.length != outputAxes.length) {
			// TODO support the general case
			throw new N5Exception("A continuous non-invertible transform from a mapAxis is not yet supported.");
		}

		int nd = inputAxes.length;
		final HashMap<String, Integer> inputIndexes = mapToIndexes(inputAxes);
		final HashMap<String, Integer> outputIndexes = mapToIndexes(outputAxes);

		final double[][] affine = new double[nd][nd+1];
		for (Entry<String, String> entry : axisMapping.entrySet()) {
			final String output = entry.getKey();
			final String input = entry.getValue();

			final int row = outputIndexes.get(output);
			final int col = inputIndexes.get(input);
			affine[row][col] = 1;
		}

		return new AffineTransform(TransformUtils.flatten(affine));
	}

	private static MixedTransform integerTransformFromMapping(final Map<String, String> axisMapping,
			String[] inputAxes, String[] outputAxes) {

		final int ndIn = inputAxes.length;
		final int ndOut = outputAxes.length;

		final HashMap<String, Integer> inputIndexes = mapToIndexes(inputAxes);
		final HashMap<String, Integer> outputIndexes = mapToIndexes(outputAxes);

		final boolean[] componentZero = new boolean[ndOut];
		Arrays.fill(componentZero, true);

		final int[] componentMapping = new int[ndOut];
		for (Entry<String, String> entry : axisMapping.entrySet()) {	

			final String output = entry.getKey();
			final String input = entry.getValue();
			componentMapping[outputIndexes.get(output)] = inputIndexes.get(input);
			componentZero[outputIndexes.get(output)] = false;
		}

		final MixedTransform transform = new MixedTransform(ndIn, ndOut);
		transform.setComponentMapping(componentMapping);
		transform.setComponentZero(componentZero);

		return transform;
	}

	private static HashMap<String,Integer> mapToIndexes( final String[] strings ) {

		return IntStream.range(0, strings.length)
				.collect(
						HashMap::new,
						(m,i) -> m.put(strings[i],i),
						(x,y) -> x.putAll(y));
	}

	private static void validate(final AffineGet affine, final double eps) {

		// imglib2 AffineGet always ave numSourceDimensions == numTargetDimensions
		int nd = affine.numSourceDimensions();
		for( int i = 0; i < nd; i++)
			for( int j = 0; j < nd; j++) {
				double val = affine.get(i, j);
				if( Math.abs(1 - val) > eps && Math.abs(val) > eps ) {
					throw new IllegalArgumentException(
							String.format("Matrix representation for mapAxis must be a permutation matrix"));
				}
			}
	}

	public static void validate(Map<String, String> axisMapping, String[] inputAxes, String[] outputAxes) {
		
		final int numOutput = outputAxes.length;
		final boolean[] outputExists = new boolean[numOutput];
		for (Entry<String, String> entry : axisMapping.entrySet()) {

			final int i = firstIndexOf( entry.getKey(), outputAxes );
			if( i < 0 )
				throw new N5Exception("The key [" + entry.getKey() + "] is not an output axis.");
			else 
				outputExists[i] = true;

			if( firstIndexOf( entry.getValue(), inputAxes ) < 0 )
				throw new N5Exception("The value [" + entry.getKey() + "] is not an input axis.");
		}

		final ArrayList<String> outputsNotPresent = new ArrayList<>();
		for (int i = 0; i < numOutput; i++) {
			if (!outputExists[i])
				outputsNotPresent.add(outputAxes[i]);
		}

		if( outputsNotPresent.size() > 0 )
			throw new N5Exception("All output axes must be keys for mapAxis.\n"
					+ "The axes\n[" + outputsNotPresent.stream().collect(Collectors.joining(","))
					+ "]\nwere not present.");
	}

	private static int firstIndexOf(String query, String[] values) {
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(query))
				return i;
		}
		return -1;
	}

}

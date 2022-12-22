package org.janelia.saalfeldlab.n5.universe.metadata.axisTransforms;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.stream.IntStream;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.IndexedAxis;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.IdentitySpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;

public class TransformAxesMetadataAdapter implements JsonDeserializer<TransformAxes>  {

	@Override
	public TransformAxes deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		final JsonObject jsonObj = json.getAsJsonObject();

		SpatialTransform transform = null;
		if (jsonObj.has("transform")) {
			transform = context.deserialize(jsonObj.get("transform"), SpatialTransform.class);
		}
		else
			transform = new IdentitySpatialTransform();

		IndexedAxis[] inputAxesParsed = null;
		if (jsonObj.has("inputAxes")) {
			inputAxesParsed = context.deserialize(jsonObj.get("inputAxes"), IndexedAxis[].class);
		}

		IndexedAxis[] outputAxesParsed = null;
		if (jsonObj.has("outputAxes")) {
			outputAxesParsed = context.deserialize(jsonObj.get("outputAxes"), IndexedAxis[].class);
		}
		
		int[] inIdxs = null;
		if (jsonObj.has("inputIndexes")) {
			inIdxs = context.deserialize(jsonObj.get("inputIndexes"), int[].class);
		}

		int[] outIdxs = null;
		if (jsonObj.has("outputIndexes")) {
			outIdxs = context.deserialize(jsonObj.get("outputIndexes"), int[].class);
		}

		String[] outputLabels = null;
		if (jsonObj.has("outputLabels")) {
			outputLabels = context.deserialize(jsonObj.get("outputLabels"), String[].class);
		}

//		final IndexedAxis[] inputAxes = makeAxes( -1, inputAxesParsed, "none", null, inIdxs, true );
//		final IndexedAxis[] outputAxes= makeAxes( -1, outputAxesParsed, "none", outputLabels, outIdxs, false );

		final IndexedAxis[] inputAxes = makeAxes( inputAxesParsed, "none", null, inIdxs );
		final IndexedAxis[] outputAxes= makeAxes( outputAxesParsed, "none", outputLabels, outIdxs );

		final TransformAxes ta = new TransformAxes( transform, inputAxes, outputAxes );
		return ta;
	}

	private static IndexedAxis[] makeAxes( IndexedAxis[] axes, String unit, String[] axisLabels, int[] indexes )
	{
		if( axes != null )
			return axes;

		int N = 0;
		if( axisLabels != null )
			N = axisLabels.length;
		else if( indexes != null )
			N = indexes.length;

		if( N > 0 ) {
			IndexedAxis[] out = new IndexedAxis[ N ];
			for( int i = 0; i < N; i++ ) {
				out[i] = new IndexedAxis(
						null,
						axisLabels != null ? axisLabels[i] : null,
						unit != null ? unit : null,
						indexes != null ? indexes[i] : -1);
			}
			return out;
		}

		return null;
	}

	private static IndexedAxis[] makeAxes( int N, IndexedAxis[] axes, String unit, String[] axisLabels, int[] indexes, boolean input )
	{
		if( axes != null )
			return axes;

		if( axisLabels == null && indexes == null )
			return null;

		IndexedAxis[] axesout;
		if( axisLabels != null ) {
			
			if( indexes != null )
				axesout = IndexedAxis.axesFromLabels(axisLabels, indexes, unit);
			else
				axesout = IndexedAxis.axesFromLabels(axisLabels, unit);
		}
		else if( indexes != null )
			if( input )
				axesout = IndexedAxis.dataAxes(indexes);
			else
				axesout = IndexedAxis.axesFromIndexes(indexes, unit); 
		else if( N > 0 )
			if( input )
				axesout = IndexedAxis.dataAxes(N);
			else
				axesout = IndexedAxis.axesFromIndexes( IntStream.range(0, N).toArray(), unit); 
		else
			return null;

		return axesout;
	}

}

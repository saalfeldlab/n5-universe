package org.janelia.saalfeldlab.n5.universe.metadata.ngff.coordinateTransformations;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.axes.AxisAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformGraph;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.TransformPath;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations.CoordinateTransformAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.imglib2.realtransform.RealTransform;

/**
 * CLI for OME-Zarr coordinate transformation conformance testing.
 *
 * Usage: <zarr_path> <source_space> <target_space> <coordinates_json>
 */
public class NgffTransformsConformance2 {

	public static void main(final String[] args) throws JsonSyntaxException, JsonIOException, FileNotFoundException {

		if (args.length < 4) {
			exitWithError("Expected 4 arguments: <zarr_path> <source> <target> <coordinates_json>", 2);
			return;
		}

		final String zarrPath = args[0];
		final String sourceName = args[1];
		final String targetName = args[2];
		final String coordsJson = args[3];

		N5Reader n5 = new N5Factory()
				.gsonBuilder(gsonBuilder())
				.openReader(zarrPath);

		try {

			final TransformGraph graph = loadTransformGraph(n5);
			final Optional<TransformPath> pathOpt = graph.path(sourceName, targetName);
			if (!pathOpt.isPresent()) {
				exitWithError("No path from '" + sourceName + "' to '" + targetName + "'", 1);
				return;
			}
			
			int ndIn = graph.getCoordinateSystems().getSpace(sourceName).numDimensions();
			int ndOut = graph.getCoordinateSystems().getSpace(targetName).numDimensions();

			final Gson gson = buildGson();
			final double[][] inputCoords = gson.fromJson(coordsJson, double[][].class);
			final RealTransform transform = pathOpt.get().totalTransform(n5, graph);
			final double[][] outputCoords = applyTransform(ndIn, ndOut, transform, inputCoords);

			final JsonObject result = new JsonObject();
			result.add("coordinates", gson.toJsonTree(outputCoords));
			result.addProperty("message", "sucess");
			System.out.println(gson.toJson(result));
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			exitWithError(e.getMessage(), 1);
		}
	}

	public static TransformGraph loadTransformGraph(final N5Reader n5) throws JsonSyntaxException, JsonIOException, FileNotFoundException {

		final CoordinateSystem[] spaces = n5.getAttribute("", "ome/scene/" + CoordinateSystem.KEY, CoordinateSystem[].class);
		@SuppressWarnings("rawtypes")
		final CoordinateTransform[] transforms = n5.getAttribute("", "ome/scene/" + CoordinateTransform.KEY, CoordinateTransform[].class);
		return new TransformGraph(Arrays.asList(transforms), Arrays.asList(spaces));
	}
	
	private static GsonBuilder gsonBuilder() {

		return new GsonBuilder()
				.registerTypeAdapter(CoordinateTransform.class, new CoordinateTransformAdapter(null))
				.registerTypeAdapter(Axis.class, new AxisAdapter());
	}

	private static Gson buildGson() {

		return gsonBuilder().create();
	}

	private static double[][] applyTransform(int ndIn, int ndOut, final RealTransform transform, final double[][] coords) {

		// temporary variables
		final double[] p = new double[ndIn];
		final double[] q = new double[ndOut];

		final double[][] result = new double[coords.length][ndOut];
		for (int i = 0; i < coords.length; i++) {

//			System.out.println("in  : " + Arrays.toString(coords[i]));

			System.arraycopy(coords[i], 0, p, 0, ndIn);
			reverse(p);
			
//			System.out.println("p   : " + Arrays.toString(p));

			transform.apply(p, q);


//			System.out.println("q   : " + Arrays.toString(q));

			reverse(q);
			System.arraycopy(q, 0, result[i], 0, ndOut);

//			System.out.println("res : " + Arrays.toString(result[i]));

		}
		return result;
	}

	private static void reverse(final double[] a) {

		for (int i = 0, j = a.length - 1; i < j; i++, j--) {
			final double tmp = a[i];
			a[i] = a[j];
			a[j] = tmp;
		}
	}

	private static void exitWithError(final String message, final int code) {

		final JsonObject error = new JsonObject();
		if (message != null)
			error.addProperty("message", message);
		System.out.println(new Gson().toJson(error));
		System.exit(code);
	}

}

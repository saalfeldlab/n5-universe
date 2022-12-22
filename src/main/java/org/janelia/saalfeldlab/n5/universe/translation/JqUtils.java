package org.janelia.saalfeldlab.n5.universe.translation;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.universe.metadata.axisTransforms.TransformAxes;
import org.janelia.saalfeldlab.n5.universe.metadata.axisTransforms.TransformAxesMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataAdapter;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransformAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;
import org.janelia.saalfeldlab.n5.universe.translation.ImportedTranslations;

public class JqUtils {

	public static String resolveImports(String query) {
		if (query.startsWith("include")) {
			return new ImportedTranslations().getTranslation() + query.replaceFirst("^\\s*include\\s+\"n5\"\\s*;", "");
		}
		else
			return query;
	}

	public static Scope buildRootScope() {
		// First of all, you have to prepare a Scope which s a container of
		// built-in/user-defined functions and variables.
		final Scope rootScope = Scope.newEmptyScope();

		// Use BuiltinFunctionLoader to load built-in functions from the classpath.
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function() {
			@Override
			public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path,
					final PathOutput output, final Version version) throws JsonQueryException {
				args.get(0).apply(scope, in, (time) -> {
					output.emit(new TextNode(Strings.repeat(in.asText(), time.asInt())), null);
				});
			}
		});
		return rootScope;
	}

	private static class ExcludeParentGsonFromContainerMetadata implements ExclusionStrategy {

		@Override public boolean shouldSkipField(FieldAttributes f) {

			final Class<?> declaringClass = f.getDeclaringClass();
			final boolean isContainerMetadataNode = declaringClass.equals(ContainerMetadataNode.class);
			final boolean isAbstractGsonReader = declaringClass.equals(AbstractGsonReader.class);
			return isAbstractGsonReader && f.getName().equals("gson");
		}

		@Override public boolean shouldSkipClass(Class<?> clazz) {

			return false;
		}
	}

	public static GsonBuilder gsonBuilder( final N5Reader n5 ) {
		final GsonBuilder gsonBuilder;
		if (n5 instanceof AbstractGsonReader) {
			gsonBuilder = ((AbstractGsonReader)n5).getGson().newBuilder();
		} else gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(SpatialTransform.class, new SpatialTransformAdapter( n5 ));
		gsonBuilder.registerTypeAdapter(CanonicalMetadata.class, new CanonicalMetadataAdapter());
		gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeAdapter(TransformAxes.class, new TransformAxesMetadataAdapter());
		gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());

		gsonBuilder.setExclusionStrategies(new ExcludeParentGsonFromContainerMetadata());
		gsonBuilder.disableHtmlEscaping();
		return gsonBuilder;
	}

	public static GsonBuilder newBuilder( final Gson gson ) {
		final GsonBuilder gsonBuilder = gson.newBuilder();
		gsonBuilder.registerTypeAdapter(SpatialTransform.class, new SpatialTransformAdapter( null ));
		gsonBuilder.registerTypeAdapter(CanonicalMetadata.class, new CanonicalMetadataAdapter());
		gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeAdapter(TransformAxes.class, new TransformAxesMetadataAdapter());
		gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());
		gsonBuilder.setExclusionStrategies(new ExcludeParentGsonFromContainerMetadata());
		return gsonBuilder;
	}

	public static Gson buildGson( final N5Reader n5 ) {
		return gsonBuilder(n5).create();
	}

	public String transform( final String in, final String translation, final ObjectMapper objMapper, final Scope scope ) throws JsonMappingException, JsonProcessingException
	{
		JsonNode inJsonNode = objMapper.readTree( in );

		final List< JsonNode > out = new ArrayList<>();
		JsonQuery.compile( translation, Versions.JQ_1_6 ).apply( scope, inJsonNode, out::add );

		final StringBuffer stringOutput = new StringBuffer();
		for ( final JsonNode n : out )
			stringOutput.append( n.toString() + "\n" );

		return stringOutput.toString();
	}

}

package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.universe.metadata.ColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.IntColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.RGBAColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata.IntensityLimits;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.ParametrizedTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SequenceSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;

/**
 * A parser for the "canonical" metadata dialect.
 * 
 * @author John Bogovic
 */
public class CanonicalMetadataParser implements N5MetadataParser< CanonicalMetadata > {

	protected Gson gson;

	protected ContainerMetadataNode root;

	protected Predicate<CanonicalMetadata> filter;

	public CanonicalMetadataParser() {
		this(x -> true);
	}

	public CanonicalMetadataParser( final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

	public void setFilter(final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

	@Deprecated
	public CanonicalMetadataParser( final N5Reader n5, final String n5Tree, final String translation) {
		gson = JqUtils.buildGson(n5);
//		root = gson.fromJson( n5Tree, ContainerMetadataNode.class );
	}

	public Gson getGson() {
		return gson;
	}

	public void setGson( Gson gson ) {
		this.gson = gson;
	}
	
	@Deprecated
	protected void setup( final N5Reader n5 ) {
		// TODO rebuilding gson and root is the safest thing to do, but possibly inefficient

		setGson( JqUtils.buildGson( n5 ));

		root = ContainerMetadataNode.build(n5, gson);
		root.addPathsRecursive();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<CanonicalMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		final String path = node.getPath();

		DatasetAttributes attrs = null;
		SpatialMetadataCanonical spatial = null;
		MultiResolutionSpatialMetadataCanonical multiscale = null;
		MultiChannelMetadataCanonical multichannel = null;
		IntensityLimits intensityLimits = null;
		ColorMetadata color = null;
		try {
			attrs = n5.getDatasetAttributes( path );
			spatial = n5.getAttribute(path, "spatialTransform", SpatialMetadataCanonical.class);
			multiscale = n5.getAttribute(path, "multiscales", MultiResolutionSpatialMetadataCanonical.class);
			multichannel = n5.getAttribute(path, "multichannel", MultiChannelMetadataCanonical.class);
			intensityLimits = n5.getAttribute(path, "intensityLimits", IntensityLimits.class);

			color = n5.getAttribute(path, "color", IntColorMetadata.class);
			if( color == null )
				color = n5.getAttribute(path, "color", RGBAColorMetadata.class);

		} catch (IOException e) {
		}

		if( spatial != null ) {
			SpatialTransform transform = spatial.transform();
			if( transform instanceof ParametrizedTransform ) {
				@SuppressWarnings("rawtypes")
				ParametrizedTransform pt = (ParametrizedTransform)transform;
				if( pt.getParameterPath() != null ) {
					pt.buildTransform( pt.getParameters(n5));
				}
			}
			else if ( transform instanceof SequenceSpatialTransform ) {
				SequenceSpatialTransform seq = (SequenceSpatialTransform)transform;
				for( SpatialTransform t : seq.getTransformations())
				{
					if( t instanceof ParametrizedTransform ) {
						@SuppressWarnings("rawtypes")
						ParametrizedTransform pt = (ParametrizedTransform)t;
						if( pt.getParameterPath() != null ) {
							pt.buildTransform( pt.getParameters(n5));
						}
					}
				}
			}
		}

		if( spatial == null && multichannel == null && multiscale == null &&
				color == null && intensityLimits == null ) {
			return Optional.empty();
		}

		if (attrs != null ) {
			if( spatial != null )
				return Optional.of(new CanonicalSpatialDatasetMetadata(path, spatial, attrs, intensityLimits, color ));
			else
				return Optional.of(new CanonicalDatasetMetadata(path, attrs, intensityLimits, color ));
		} else if( spatial != null ) {
			return Optional.of(new CanonicalSpatialMetadata( path, spatial, intensityLimits ));
		} else if (multiscale != null && multiscale.getChildrenMetadata() != null) {
			return Optional.of(new CanonicalMultiscaleMetadata(path, multiscale ));
		} else if (multichannel != null && multichannel.getPaths() != null) {
			return Optional.of(new CanonicalMultichannelMetadata(path, multichannel ));
		} else {
			// if lots of things are present
			return Optional.empty();
		}
	}

	@Deprecated
	public Optional<CanonicalMetadata> parseMetadata(final String dataset, final String groupSep ) {
		return parseMetadata( new N5TreeNode( dataset ), groupSep );
	}

	@Deprecated
	public Optional<CanonicalMetadata> parseMetadata(N5TreeNode node, String groupSep) {
		if (root == null)
			return Optional.empty();

		return root.getNode(node.getPath())
				.map(ContainerMetadataNode::getAttributes)
				.map(this::canonicalMetadata)
				.filter(filter);
	}

	public CanonicalMetadata canonicalMetadata(final HashMap<String, JsonElement> attrMap) {
		return gson.fromJson(gson.toJson(attrMap), CanonicalMetadata.class);
	}

	/**
	 * Use {@link JqUtils} instead.
	 * 
	 * @return Scope
	 */
	@Deprecated
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
	
	public static Optional<DatasetAttributes> datasetAttributes(final Gson gson,
			HashMap<String, JsonElement> attributeMap) {

		try {

			final long[] dimensions = GsonAttributesParser.parseAttribute(attributeMap, "dimensions", long[].class,
					gson);
			if (dimensions == null)
				return Optional.empty();

			final DataType dataType = GsonAttributesParser.parseAttribute(attributeMap, "dataType", DataType.class,
					gson);
			if (dataType == null)
				return Optional.empty();

			int[] blockSize = GsonAttributesParser.parseAttribute(attributeMap, "blockSize", int[].class, gson);
			if (blockSize == null)
				blockSize = Arrays.stream(dimensions).mapToInt(a -> (int) a).toArray();

			Compression compression = GsonAttributesParser.parseAttribute(attributeMap, "compression",
					Compression.class, gson);

			/* version 0 */
			if (compression == null) {
				switch (GsonAttributesParser.parseAttribute(attributeMap, "compression", String.class, gson)) {
				case "raw":
					compression = new RawCompression();
					break;
				case "gzip":
					compression = new GzipCompression();
					break;
				case "bzip2":
					compression = new Bzip2Compression();
					break;
				case "lz4":
					compression = new Lz4Compression();
					break;
				case "xz":
					compression = new XzCompression();
					break;
				}
			}

			return Optional.of(new DatasetAttributes(dimensions, blockSize, dataType, compression));

		} catch (Exception e) {
		}

		return Optional.empty();
	}

	public static Optional<DatasetAttributes> datasetAttributes(final JsonDeserializationContext context, JsonElement elem ) {

		try {

			final long[] dimensions = context.deserialize( elem.getAsJsonObject().get("dimensions"), long[].class);
			if (dimensions == null)
				return Optional.empty();

			final DataType dataType = context.deserialize( elem.getAsJsonObject().get("dataType"), DataType.class);
			if (dataType == null)
				return Optional.empty();

			int[] blockSize = context.deserialize( elem.getAsJsonObject().get("blockSize"), int[].class );
			if (blockSize == null)
				blockSize = Arrays.stream(dimensions).mapToInt(a -> (int)a).toArray();

			Compression compression = context.deserialize( elem.getAsJsonObject().get("compression"), Compression.class );

			/* version 0 */
			if (compression == null) {
				final String compressionString = context.deserialize( elem.getAsJsonObject().get("compression"), String.class );
				switch ( compressionString ) {
				case "raw":
					compression = new RawCompression();
					break;
				case "gzip":
					compression = new GzipCompression();
					break;
				case "bzip2":
					compression = new Bzip2Compression();
					break;
				case "lz4":
					compression = new Lz4Compression();
					break;
				case "xz":
					compression = new XzCompression();
					break;
				}
			}

			return Optional.of(new DatasetAttributes(dimensions, blockSize, dataType, compression));

		} catch (Exception e) {}

		return Optional.empty();
	}

}

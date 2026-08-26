package org.janelia.saalfeldlab.n5.universe.kvr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;
import org.janelia.saalfeldlab.n5.N5Path.N5DirectoryPath;

/**
 * A compiled jq expression, rewriting one {@code JsonElement} into another.
 * <p>
 * The expression must produce exactly one output. Producing none, or more than
 * one, is an error: it usually means the expression forgot to collect a stream
 * of results into an array.
 * <p>
 * If the expression starts with {@code include "n5";}, that is replaced by the
 * shared definitions, assembled from three resources: the {@link
 * DialectInfo#jqResource() dialect accessors}, then {@code /kvr/common.jq} (value
 * helpers, dialect independent), then {@code /kvr/tree.jq} (definitions that
 * depend on the {@link Directory} tree shape). The order matters: each part may
 * only use definitions from the parts before it.
 */
class JqTranslation {

	/**
	 * Loading the jq builtins is not cheap and the result is the same every
	 * time, so the root scope is shared. {@code ObjectMapper} is thread-safe
	 * and meant to be shared as well.
	 */
	private static final Scope rootScope = newRootScope();

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static Scope newRootScope() {
		final Scope scope = Scope.newEmptyScope();
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope);
		return scope;
	}

	private final JsonQuery query;

	JqTranslation(final DialectInfo dialect, final String translation) {
		try {
			query = JsonQuery.compile(resolveImports(dialect, translation), Versions.JQ_1_6);
		} catch (final JsonQueryException e) {
			throw new N5Exception("Could not compile jq translation: " + translation, e);
		}
	}

	public JsonElement apply(final JsonElement src) {
		try {
			final JsonNode in = objectMapper.readTree(src.toString());
			final List<JsonNode> out = new ArrayList<>();
			query.apply(rootScope, in, out::add);
			if (out.size() != 1)
				throw new N5Exception("jq translation produced " + out.size() + " outputs, expected exactly one");
			return JsonParser.parseString(out.get(0).toString());
		} catch (final IOException e) {
			// NB: JsonQueryException extends IOException
			throw new N5Exception("Could not apply jq translation", e);
		}
	}


	// -- translating hierarchies and paths --

	/**
	 * Translate a hierarchy.
	 *
	 * @param dir
	 * 		root of the hierarchy to translate, or {@code null} if the hierarchy
	 * 		does not exist
	 *
	 * @return root of the translated hierarchy, or {@code null} if {@code dir}
	 * 		is {@code null}
	 */
	Directory translate(final Directory dir) {

		return dir == null ? null : Directory.fromJson(apply(dir.toJson()));
	}

	/**
	 * Translate a single path: where the node at {@code path} ends up when this
	 * translation is applied.
	 * <p>
	 * This runs the translation over a hierarchy that contains {@code path} and
	 * nothing else, so no actual hierarchy is involved and {@code path} does not
	 * have to exist in one.
	 *
	 * @param path
	 * 		a path in the hierarchy this translation is applied to
	 *
	 * @return the corresponding path in the translated hierarchy
	 *
	 * @throws N5Exception
	 * 		if this translation does not map {@code path} to exactly one path
	 */
	String translatePath(final N5DirectoryPath path) {

		return pathCache.computeIfAbsent(path.normalPath(), p -> findPath(path));
	}

	/**
	 * Caches {@link #translatePath} results. Computing one is not cheap: it runs
	 * the translation, which costs roughly as much as reading a chunk -- and
	 * every chunk read of a translated container resolves its path through it.
	 * <p>
	 * Entries never go stale: the mapping is derived from the compiled query
	 * alone, and that never changes. Reads may be concurrent, hence the {@code
	 * ConcurrentHashMap}.
	 */
	private final Map<String, String> pathCache = new ConcurrentHashMap<>();

	private String findPath(final N5DirectoryPath path) {

		final Directory marked = new Directory();
		marked.getOrCreateDirectory(path).putAttributes(PATH_MARKER, new JsonPrimitive(true));

		final String found = findMarkedPath(translate(marked), "");
		if (found == null)
			throw new N5Exception("Translation does not map \"" + path + "\" to any path");
		return found;
	}

	/**
	 * Name of the attribute file that {@link #translatePath} tags a node with, to
	 * recognize it again after translating. No dialect stores attributes under
	 * this name, so translations pass it through untouched.
	 * <p>
	 * A marker is needed because the node cannot be identified by tree shape
	 * alone: looking for the only leaf does not work, because moving a nested
	 * path leaves the (now empty, and therefore also leaf) nodes above it behind.
	 */
	private static final String PATH_MARKER = "__pathMarker__";

	/**
	 * Find the path of the node in {@code dir} that is tagged with the {@link
	 * #PATH_MARKER}.
	 *
	 * @param dir
	 * 		the tree to search
	 * @param prefix
	 * 		the path of {@code dir}. The result is relative to it, so pass {@code
	 * 		""} to get a path relative to {@code dir}.
	 *
	 * @return the marked path, or {@code null} if no node is marked. Note that
	 * 		{@code ""} (the marker sitting on {@code dir} itself) is a result, not
	 * 		a miss.
	 *
	 * @throws N5Exception
	 * 		as soon as a second marked node is found
	 */
	private static String findMarkedPath(final Directory dir, final String prefix) {

		String found = dir.getAttributes(PATH_MARKER) != null ? prefix : null;
		for (final String name : dir.list()) {
			final String inChild = findMarkedPath(dir.getChild(name),
					prefix.isEmpty() ? name : prefix + "/" + name);
			if (inChild == null)
				continue;
			if (found != null)
				throw new N5Exception("Translation is ambiguous:"
						+ " it maps one path to both \"" + found + "\" and \"" + inChild + "\"");
			found = inChild;
		}
		return found;
	}


	// -- includes --

	// TODO: these resources live under /kvr/ only to stay out of the way of the
	//  legacy /n5.jq, which universe.translation still loads. Once that package
	//  is phased out, move them up to /common.jq, /tree.jq, /dialect-*.jq and
	//  delete /n5.jq.
	//  Also fix the class javadoc.

	private static final Pattern INCLUDE_N5 = Pattern.compile("^\\s*include\\s+\"n5\"\\s*;");

	private static final Map<DialectInfo, String> preludes = loadPreludes();

	private static Map<DialectInfo, String> loadPreludes() {
		final String shared = loadResource("/kvr/common.jq") + "\n" + loadResource("/kvr/tree.jq") + "\n";
		final Map<DialectInfo, String> map = new EnumMap<>(DialectInfo.class);
		for (final DialectInfo dialect : DialectInfo.values())
			map.put(dialect, loadResource(dialect.jqResource()) + "\n" + shared);
		return map;
	}

	private static String resolveImports(final DialectInfo dialect, final String query) {
		final Matcher matcher = INCLUDE_N5.matcher(query);
		return matcher.lookingAt() ? preludes.get(dialect) + query.substring(matcher.end()) : query;
	}

	private static String loadResource(final String name) {
		try (final InputStream in = JqTranslation.class.getResourceAsStream(name)) {
			if (in == null)
				throw new N5IOException("Resource not found: " + name);
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				return reader.lines().collect(Collectors.joining("\n"));
			}
		} catch (final IOException e) {
			throw new N5IOException("Could not read resource: " + name, e);
		}
	}
}

package org.janelia.saalfeldlab.n5.universe.translation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Exception.N5IOException;

/**
 * Resolves {@code include "name";} directives in jq translations.
 * <p>
 * Directives are only recognized at the top of a script (or of an included
 * file), where only whitespace and {@code #} comments may precede them. Each
 * one is replaced by the text of the named file, and files are included at most
 * once, dependencies first.
 * <p>
 * {@code name} is looked up as {@code name.jq}, then {@code name/<last
 * component>.jq}, in:
 * <ol>
 * <li>each directory of the {@value #ENV_PATH} environment variable, in order
 * (separated like {@code PATH}),</li>
 * <li>{@code ~/.jq},</li>
 * <li>the jq files shipped with n5-universe ({@link #SHIPPED}). Only those: other
 * jars on the class path may have top-level {@code .jq} files of their own (for
 * example an old {@code n5.jq}), which must not be picked up.</li>
 * </ol>
 * The first match wins, so a file in an earlier place silently replaces one with
 * the same name in a later place. The one exception is {@code dialect}, which is
 * always the jq file of the dialect of the container ({@link
 * DialectInfo#jqResource()}), and can't be replaced. The {@code dialect-*} files
 * can't be included by their own names.
 */
final class JqIncludes {

	static final String ENV_PATH = "N5_JQ_PATH";

	static final String DIALECT = "dialect";

	/**
	 * Names of the jq files shipped in {@code src/main/resources}, other than
	 * the {@code dialect-*} files (see {@link DialectInfo#jqResource()}). A class
	 * path directory can't be listed from inside a jar, so a new file has to be
	 * added here.
	 */
	static final Set<String> SHIPPED = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
			"common", "tree", "cosem", "deprecated", "ij", "n5v", "ngff-04")));

	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*");

	/** whitespace and comments, which may precede a directive */
	private static final Pattern SKIP = Pattern.compile("(?:\\s+|#[^\\n]*)*");

	private static final Pattern INCLUDE = Pattern.compile("include\\s+\"([^\"]*)\"\\s*;");

	private JqIncludes() {}

	/**
	 * The directories that {@code include} looks in before the shipped resources:
	 * those of {@code N5_JQ_PATH}, then {@code ~/.jq}.
	 */
	static List<Path> defaultSearchPath() {

		return searchPath(System.getenv(ENV_PATH), Paths.get(System.getProperty("user.home")));
	}

	/**
	 * @param envPath
	 * 		value of {@code N5_JQ_PATH}, or {@code null}. Empty entries are skipped,
	 * 		and a leading {@code ~/} stands for {@code home}.
	 * @param home
	 * 		the home directory
	 *
	 * @return the entries of {@code envPath}, then {@code home/.jq}, without
	 * 		repeats. Directories that don't exist are kept: they are skipped on lookup.
	 */
	static List<Path> searchPath(final String envPath, final Path home) {

		final Set<Path> dirs = new LinkedHashSet<>();
		if (envPath != null) {
			for (final String entry : envPath.split(Pattern.quote(File.pathSeparator))) {
				if (entry.isEmpty())
					continue;
				if (entry.equals("~"))
					dirs.add(home);
				else if (entry.startsWith("~/"))
					dirs.add(home.resolve(entry.substring(2)));
				else
					dirs.add(Paths.get(entry));
			}
		}
		dirs.add(home.resolve(".jq"));
		return new ArrayList<>(dirs);
	}

	/**
	 * Replace the {@code include} directives at the top of {@code query} by the
	 * files they name, so that the result is a complete jq program.
	 *
	 * @param dialect
	 * 		the dialect that {@code include "dialect";} refers to
	 * @param query
	 * 		a jq program, which may start with {@code include "name";} directives
	 * @param searchPath
	 * 		directories to look in before the shipped files
	 *
	 * @return the included files (and the files they include), each once and
	 * 		dependencies first, followed by {@code query} without its directives
	 *
	 * @throws N5Exception
	 * 		if an include is not found, is not a valid name, or is circular
	 */
	static String resolve(final DialectInfo dialect, final String query, final List<Path> searchPath) {

		final StringBuilder out = new StringBuilder();
		final String body = resolveInto(dialect, query, searchPath, out, new HashSet<>(), new ArrayList<>());
		return out.append(body).toString();
	}

	/**
	 * Append the files that {@code text} includes to {@code out}, and return
	 * {@code text} without its directives.
	 */
	private static String resolveInto(
			final DialectInfo dialect,
			final String text,
			final List<Path> searchPath,
			final StringBuilder out,
			final Set<String> done,
			final List<String> stack) {

		final StringBuilder body = new StringBuilder();
		final Matcher skip = SKIP.matcher(text);
		final Matcher include = INCLUDE.matcher(text);
		int pos = 0;
		while (true) {
			skip.region(pos, text.length());
			skip.lookingAt();
			include.region(skip.end(), text.length());
			if (!include.lookingAt())
				break;
			body.append(text, pos, skip.end());
			pos = include.end();

			final String name = include.group(1);
			if (stack.contains(name))
				throw new N5Exception("Circular jq include: " + String.join(" -> ", stack) + " -> " + name);
			if (!done.add(name))
				continue;

			stack.add(name);
			final String stripped = resolveInto(dialect, load(dialect, name, searchPath), searchPath, out, done, stack);
			stack.remove(stack.size() - 1);
			out.append(stripped).append("\n");
		}
		return body.append(text, pos, text.length()).toString();
	}

	private static String load(final DialectInfo dialect, final String name, final List<Path> searchPath) {

		if (name.equals(DIALECT))
			return loadResource(dialect.jqResource());

		validate(name);
		final String last = name.substring(name.lastIndexOf('/') + 1);
		final String[] relPaths = {name + ".jq", name + "/" + last + ".jq"};

		for (final Path dir : searchPath) {
			for (final String rel : relPaths) {
				final Path file = dir.resolve(rel);
				if (Files.isRegularFile(file))
					return readFile(file);
			}
		}

		if (SHIPPED.contains(name))
			return loadShipped("/" + name + ".jq");

		throw new N5Exception("jq include \"" + name + "\" not found. Looked for " + String.join(" or ", relPaths)
				+ " in: " + searchPath.stream().map(Path::toString).collect(Collectors.joining(", "))
				+ (searchPath.isEmpty() ? "" : ", ") + "and the jq resources shipped with n5-universe");
	}

	private static void validate(final String name) {

		if (!NAME.matcher(name).matches())
			throw new N5Exception("Invalid jq include name: \"" + name + "\"");
		final String[] parts = name.split("/");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].equals(".") || parts[i].equals(".."))
				throw new N5Exception("Invalid jq include name: \"" + name + "\" (relative components are not allowed)");
			if (parts[i].startsWith("dialect-"))
				throw new N5Exception("Invalid jq include name: \"" + name + "\" (use include \"dialect\" instead)");
			if (i > 0 && parts[i].equals(parts[i - 1]))
				throw new N5Exception("Invalid jq include name: \"" + name + "\" (repeated component)");
		}
	}

	private static String readFile(final Path file) {

		try {
			return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new N5IOException("Could not read jq include: " + file, e);
		}
	}

	/** Shipped files never change, so they are read only once. */
	private static final Map<String, String> shipped = new ConcurrentHashMap<>();

	private static String loadShipped(final String resource) {

		return shipped.computeIfAbsent(resource, JqIncludes::loadResource);
	}

	static String loadResource(final String name) {

		try (final InputStream in = JqIncludes.class.getResourceAsStream(name)) {
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

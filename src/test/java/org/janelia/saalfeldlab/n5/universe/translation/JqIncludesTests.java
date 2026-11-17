package org.janelia.saalfeldlab.n5.universe.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Tests for {@code include "name";} resolution ({@link JqIncludes}), run on
 * temporary directories standing in for {@code N5_JQ_PATH} and {@code ~/.jq}.
 */
public class JqIncludesTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final JsonElement INPUT = JsonParser.parseString(
			"{\"attributes\":{\"attributes.json\":{\"a\":1}},\"children\":{}}");

	private Path dir(final String name) throws IOException {

		return tmp.newFolder(name).toPath();
	}

	private static void write(final Path dir, final String relPath, final String text) throws IOException {

		final Path file = dir.resolve(relPath);
		Files.createDirectories(file.getParent());
		Files.write(file, text.getBytes(StandardCharsets.UTF_8));
	}

	private static JsonElement run(final String query, final Path... searchPath) {

		return new JqTranslation(DialectInfo.N5, query, Arrays.asList(searchPath)).apply(INPUT);
	}

	private static JsonElement num(final int i) {

		return JsonParser.parseString(Integer.toString(i));
	}

	private static String errorOf(final String query, final Path... searchPath) {

		try {
			run(query, searchPath);
		} catch (final N5Exception e) {
			return e.getMessage();
		}
		fail("expected an N5Exception for: " + query);
		return null;
	}


	// -- shipped files --

	@Test
	public void testShippedFileByName() {

		// n5vIsResObj etc. come from n5v.jq, which pulls in tree and dialect
		final JsonElement out = run("include \"n5v\"; {\"pixelResolution\":{\"dimensions\":[1,2],\"unit\":\"mm\"}} | n5vScale");
		assertEquals(JsonParser.parseString("[1,2]"), out);
	}

	@Test
	public void testNothingIsIncludedImplicitly() {

		// attrs comes from the dialect: without an include it does not exist
		try {
			run("attrs");
			fail();
		} catch (final N5Exception e) {
			// expected: function does not exist
		}
	}

	@Test
	public void testN5HasNoSpecialMeaning() {

		final String message = errorOf("include \"n5\"; .");
		assertTrue(message, message.contains("\"n5\" not found"));
	}

	@Test
	public void testDialectIsTheContainersDialect() {

		final String q = "include \"dialect\"; attrs.a";
		assertEquals(num(1), new JqTranslation(DialectInfo.N5, q, Collections.emptyList()).apply(INPUT));

		final JsonElement zarr = JsonParser.parseString("{\"attributes\":{\".zattrs\":{\"a\":2}},\"children\":{}}");
		assertEquals(num(2), new JqTranslation(DialectInfo.ZARR_V2, q, Collections.emptyList()).apply(zarr));
	}

	@Test
	public void testDialectCannotBeReplaced() throws IOException {

		final Path user = dir("user");
		write(user, "dialect.jq", "def attrs: {\"a\": 99};");
		assertEquals(num(1), run("include \"dialect\"; attrs.a", user));
	}

	@Test
	public void testDialectFilesCannotBeIncludedByName() {

		final String message = errorOf("include \"dialect-n5\"; .");
		assertTrue(message, message.contains("use include \"dialect\""));
	}


	// -- user directories --

	@Test
	public void testUserFile() throws IOException {

		final Path user = dir("user");
		write(user, "mine.jq", "def mine: 42;");
		assertEquals(num(42), run("include \"mine\"; mine", user));
	}

	@Test
	public void testDirectoryModule() throws IOException {

		final Path user = dir("user");
		write(user, "lib/lib.jq", "def fromDir: 7;");
		assertEquals(num(7), run("include \"lib\"; fromDir", user));
	}

	@Test
	public void testNestedName() throws IOException {

		final Path user = dir("user");
		write(user, "sub/thing.jq", "def thing: 5;");
		assertEquals(num(5), run("include \"sub/thing\"; thing", user));
	}

	@Test
	public void testUserFileShadowsShippedFile() throws IOException {

		final Path user = dir("user");
		write(user, "n5v.jq", "def n5vScale: \"mine\";");
		assertEquals(JsonParser.parseString("\"mine\""), run("include \"n5v\"; n5vScale", user));
	}

	@Test
	public void testEarlierDirectoryWins() throws IOException {

		final Path first = dir("first");
		final Path second = dir("second");
		write(first, "x.jq", "def x: 1;");
		write(second, "x.jq", "def x: 2;");
		assertEquals(num(1), run("include \"x\"; x", first, second));
		assertEquals(num(2), run("include \"x\"; x", second, first));
	}

	@Test
	public void testFallsThroughToLaterDirectory() throws IOException {

		final Path first = dir("first");
		final Path second = dir("second");
		write(second, "y.jq", "def y: 3;");
		assertEquals(num(3), run("include \"y\"; y", first, second));
	}

	@Test
	public void testMissingDirectoryIsSkipped() throws IOException {

		final Path user = dir("user");
		write(user, "z.jq", "def z: 4;");
		assertEquals(num(4), run("include \"z\"; z", user.resolve("does-not-exist"), user));
	}


	// -- includes among files --

	@Test
	public void testTransitiveInclude() throws IOException {

		final Path user = dir("user");
		write(user, "a.jq", "include \"b\";\ndef a: b + 1;");
		write(user, "b.jq", "def b: 10;");
		assertEquals(num(11), run("include \"a\"; a", user));
	}

	@Test
	public void testIncludedOnceOnDiamond() throws IOException {

		final Path user = dir("user");
		write(user, "a.jq", "include \"base\";\ndef a: base;");
		write(user, "b.jq", "include \"base\";\ndef b: base;");
		write(user, "base.jq", "def base: 1;");
		assertEquals(num(2), run("include \"a\"; include \"b\"; include \"base\"; [a, b] | add", user));
	}

	@Test
	public void testCommentsMayPrecedeIncludes() throws IOException {

		final Path user = dir("user");
		write(user, "c.jq", "def c: 6;");
		assertEquals(num(6), run("# a comment\n\n  # another\ninclude \"c\";\n# between\nc", user));
	}

	@Test
	public void testIncludeAfterCodeIsNotADirective() throws IOException {

		final Path user = dir("user");
		write(user, "d.jq", "def d: 1;");
		// "def" ends the directive block; the include after it is left for jq, which rejects it
		try {
			run("def e: 2; include \"d\"; e", user);
			fail();
		} catch (final N5Exception e) {
			// expected: not valid jq
		}
	}

	@Test
	public void testCircularInclude() throws IOException {

		final Path user = dir("user");
		write(user, "p.jq", "include \"q\";\ndef p: 1;");
		write(user, "q.jq", "include \"p\";\ndef q: 2;");
		final String message = errorOf("include \"p\"; p", user);
		assertTrue(message, message.contains("Circular") && message.contains("p -> q -> p"));
	}


	// -- errors --

	@Test
	public void testMissingIncludeNamesWhatWasSearched() throws IOException {

		final Path user = dir("user");
		final String message = errorOf("include \"nope\"; .", user);
		assertTrue(message, message.contains("\"nope\" not found"));
		assertTrue(message, message.contains(user.toString()));
		assertTrue(message, message.contains("shipped"));
	}

	@Test
	public void testInvalidNames() {

		for (final String name : new String[]{"../x", "a/../x", "./x", "/abs", "a/a", "has space", ""}) {
			final String message = errorOf("include \"" + name + "\"; .");
			assertTrue(name + ": " + message, message.contains("Invalid jq include name"));
		}
	}


	// -- search path --

	@Test
	public void testSearchPathOrder() {

		final Path home = Paths.get("/home/me");
		final String env = "/one" + File.pathSeparator + "/two";
		assertEquals(
				Arrays.asList(Paths.get("/one"), Paths.get("/two"), Paths.get("/home/me/.jq")),
				JqIncludes.searchPath(env, home));
	}

	@Test
	public void testSearchPathWithoutEnv() {

		final Path home = Paths.get("/home/me");
		final List<Path> expected = Collections.singletonList(Paths.get("/home/me/.jq"));
		assertEquals(expected, JqIncludes.searchPath(null, home));
		assertEquals(expected, JqIncludes.searchPath("", home));
	}

	@Test
	public void testSearchPathSkipsEmptyAndRepeatedEntries() {

		final Path home = Paths.get("/home/me");
		final String env = File.pathSeparator + "/one" + File.pathSeparator + File.pathSeparator + "/one";
		assertEquals(
				Arrays.asList(Paths.get("/one"), Paths.get("/home/me/.jq")),
				JqIncludes.searchPath(env, home));
	}

	@Test
	public void testSearchPathExpandsTilde() {

		final Path home = Paths.get("/home/me");
		assertEquals(
				Arrays.asList(Paths.get("/home/me/libs"), Paths.get("/home/me/.jq")),
				JqIncludes.searchPath("~/libs", home));
		// an explicit ~/.jq in the variable is not searched twice
		assertEquals(
				Collections.singletonList(Paths.get("/home/me/.jq")),
				JqIncludes.searchPath("~/.jq", home));
	}
}

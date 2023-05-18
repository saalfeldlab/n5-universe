package org.janelia.saalfeldlab.n5.universe;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;
import org.janelia.saalfeldlab.n5.universe.translation.TranslatedN5Writer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

public class IdentityTranslatedN5Tests //extends AbstractN5Test {
{
	static protected N5Writer n5;

	static protected final String groupName = "/test/group";

	protected N5Writer createN5Writer() throws IOException, URISyntaxException {
		final String testDirPath = Files.createTempDirectory("n5-id-translated-test-").toFile().getCanonicalPath();
		return createN5Writer(testDirPath);
	}

	protected N5Writer createN5Writer(String location) throws IOException {
		final N5FSWriter n5Base = new N5FSWriter( location );
		return new TranslatedN5Writer(n5Base, JqUtils.buildGson(n5Base), ".", "." );
	}

	@Before
	public void setUpOnce() throws IOException, URISyntaxException {

		if (n5 != null)
			return;

		n5 = createN5Writer();
	}

	@AfterClass
	public static void rampDownAfterClass() throws IOException {

		if (n5 != null) {
			try { 
				Assert.assertTrue(n5.remove());
			} catch ( Exception e ) {}
			n5 = null;
		}
	}

	@Test
	public void testAttributes() {

		try {
			n5.createGroup(groupName);

			n5.setAttribute(groupName, "key1", "value1");
			// path
			Assert.assertEquals(2, n5.listAttributes(groupName).size());

			/* class interface */
			Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", String.class));
			/* type interface */
			Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", new TypeToken<String>(){}.getType()));

			final Map<String, String> newAttributes = new HashMap<>();
			newAttributes.put("key2", "value2");
			newAttributes.put("key3", "value3");
			n5.setAttributes(groupName, newAttributes);
			Assert.assertEquals(4, n5.listAttributes(groupName).size());
			/* class interface */
			Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", String.class));
			Assert.assertEquals("value2", n5.getAttribute(groupName, "key2", String.class));
			Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", String.class));
			/* type interface */
			Assert.assertEquals("value1", n5.getAttribute(groupName, "key1", new TypeToken<String>(){}.getType()));
			Assert.assertEquals("value2", n5.getAttribute(groupName, "key2", new TypeToken<String>(){}.getType()));
			Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", new TypeToken<String>(){}.getType()));

			// test the case where the resulting file becomes shorter
			n5.setAttribute(groupName, "key1", new Integer(1));
			n5.setAttribute(groupName, "key2", new Integer(2));
			Assert.assertEquals(4, n5.listAttributes(groupName).size());
			/* class interface */
			Assert.assertEquals(new Integer(1), n5.getAttribute(groupName, "key1", Integer.class));
			Assert.assertEquals(new Integer(2), n5.getAttribute(groupName, "key2", Integer.class));
			Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", String.class));
			/* type interface */
			Assert.assertEquals(new Integer(1), n5.getAttribute(groupName, "key1", new TypeToken<Integer>(){}.getType()));
			Assert.assertEquals(new Integer(2), n5.getAttribute(groupName, "key2", new TypeToken<Integer>(){}.getType()));
			Assert.assertEquals("value3", n5.getAttribute(groupName, "key3", new TypeToken<String>(){}.getType()));

		} catch (final IOException e) {
			fail(e.getMessage());
		}
	}

//	@Override
//	@Ignore("WIP")
//	@Test
//	public void testCreateDataset() {
//
//	}

}

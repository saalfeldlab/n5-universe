package org.janelia.saalfeldlab.n5.universe;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.AbstractN5Test;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;
import org.janelia.saalfeldlab.n5.universe.translation.TranslatedN5Writer;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

public class IdentityTranslatedN5Tests extends AbstractN5Test {


	@Override protected N5Writer createN5Writer() throws IOException {
		final String testDirPath = Files.createTempDirectory("n5-id-translated-test-").toFile().getCanonicalPath();
		return createN5Writer(testDirPath);
	}
	@Override protected N5Writer createN5Writer(String location) throws IOException {
		final N5FSWriter n5Base = new N5FSWriter( location );
		return new TranslatedN5Writer(n5Base, JqUtils.buildGson(n5Base), ".", "." );
	}

	@Override
	@Test
	public void testAttributes() {

		try {
			AbstractN5Test.n5.createGroup(AbstractN5Test.groupName);

			AbstractN5Test.n5.setAttribute(AbstractN5Test.groupName, "key1", "value1");
			// path
			Assert.assertEquals(2, AbstractN5Test.n5.listAttributes(AbstractN5Test.groupName).size());

			/* class interface */
			Assert.assertEquals("value1", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key1", String.class));
			/* type interface */
			Assert.assertEquals("value1", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key1", new TypeToken<String>(){}.getType()));

			final Map<String, String> newAttributes = new HashMap<>();
			newAttributes.put("key2", "value2");
			newAttributes.put("key3", "value3");
			AbstractN5Test.n5.setAttributes(AbstractN5Test.groupName, newAttributes);
			Assert.assertEquals(4, AbstractN5Test.n5.listAttributes(AbstractN5Test.groupName).size());
			/* class interface */
			Assert.assertEquals("value1", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key1", String.class));
			Assert.assertEquals("value2", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key2", String.class));
			Assert.assertEquals("value3", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key3", String.class));
			/* type interface */
			Assert.assertEquals("value1", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key1", new TypeToken<String>(){}.getType()));
			Assert.assertEquals("value2", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key2", new TypeToken<String>(){}.getType()));
			Assert.assertEquals("value3", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key3", new TypeToken<String>(){}.getType()));

			// test the case where the resulting file becomes shorter
			AbstractN5Test.n5.setAttribute(AbstractN5Test.groupName, "key1", new Integer(1));
			AbstractN5Test.n5.setAttribute(AbstractN5Test.groupName, "key2", new Integer(2));
			Assert.assertEquals(4, AbstractN5Test.n5.listAttributes(AbstractN5Test.groupName).size());
			/* class interface */
			Assert.assertEquals(new Integer(1), AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key1", Integer.class));
			Assert.assertEquals(new Integer(2), AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key2", Integer.class));
			Assert.assertEquals("value3", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key3", String.class));
			/* type interface */
			Assert.assertEquals(new Integer(1), AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key1", new TypeToken<Integer>(){}.getType()));
			Assert.assertEquals(new Integer(2), AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key2", new TypeToken<Integer>(){}.getType()));
			Assert.assertEquals("value3", AbstractN5Test.n5.getAttribute(AbstractN5Test.groupName, "key3", new TypeToken<String>(){}.getType()));

		} catch (final IOException e) {
			fail(e.getMessage());
		}
	}

}

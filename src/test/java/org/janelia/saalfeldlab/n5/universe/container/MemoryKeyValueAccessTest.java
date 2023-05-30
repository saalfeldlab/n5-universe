package org.janelia.saalfeldlab.n5.universe.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;


public class MemoryKeyValueAccessTest {
	
	@Test
	public void memoryKeyValueAccessTest()
	{
		final MemoryKeyValueAccess root = new MemoryKeyValueAccess();
		try {
			root.createDirectories("a");
			String[] list = root.list("");
			System.out.println( Arrays.toString(list));

			assertTrue(root.isDirectory("a"));
			assertEquals(1, list.length);
			assertEquals("a", list[0]);

			root.delete("a");
			list = root.list("");
			assertFalse(root.isDirectory("a"));
			assertEquals(0, list.length);

			// a/b/c
			root.createDirectories("a/b/c");
			list = root.list("");
			assertTrue(root.isDirectory("a"));
			assertEquals(1, list.length);
			assertEquals("a", list[0]);

			list = root.list("a");
			assertTrue(root.isDirectory("a/b"));
			assertEquals(1, list.length);
			assertEquals("b", list[0]);

			list = root.list("a/b");
			assertTrue(root.isDirectory("a/b/c"));
			assertEquals(1, list.length);
			assertEquals("c", list[0]);

			// delete a/b
			root.delete("a/b");
			list = root.list("");
			assertTrue(root.isDirectory("a"));
			assertEquals(1, list.length);
			assertEquals("a", list[0]);

			list = root.list("a");
			assertFalse(root.isDirectory("a/b"));
			assertEquals(0, list.length);

		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}

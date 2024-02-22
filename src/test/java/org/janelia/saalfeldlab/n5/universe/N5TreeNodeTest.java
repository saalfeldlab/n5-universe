package org.janelia.saalfeldlab.n5.universe;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class N5TreeNodeTest {

	@Test
	public void testStructureEquals() {

		N5TreeNode empty = new N5TreeNode("");

		N5TreeNode a = new N5TreeNode("a");
		N5TreeNode b = new N5TreeNode("b");
		N5TreeNode c = new N5TreeNode("c");

		N5TreeNode ab = new N5TreeNode("");
		ab.add(a);
		ab.add(b);

		N5TreeNode a2 = new N5TreeNode("a");
		N5TreeNode b2 = new N5TreeNode("b");
		N5TreeNode ab2 = new N5TreeNode("");
		ab2.add(b2);
		ab2.add(a2);

		assertTrue(empty.structureEquals(empty));
		assertFalse(empty.structureEquals(a));

		assertFalse(ab.structureEquals(a));
		assertTrue(ab.structureEquals(ab2));

		final N5TreeNode cab = c;
		cab.add(a);
		a.add(b);

		final N5TreeNode cab2 = new N5TreeNode("c");
		cab2.add(a2);
		a2.add(b2);

		assertTrue(cab.structureEquals(cab2));
		assertFalse(cab.structureEquals(ab));
		assertFalse(cab.structureEquals(empty));
	}

	@Test
	public void testAddingChildren() {

		final N5TreeNode control = new N5TreeNode("");
		final N5TreeNode a = new N5TreeNode("a");
		final N5TreeNode b = new N5TreeNode("a/b");
		final N5TreeNode c = new N5TreeNode("a/b/c");
		final N5TreeNode ant = new N5TreeNode("ant");
		final N5TreeNode bat = new N5TreeNode("ant/bat");
		control.add(a);
		a.add(b);
		b.add(c);

		final N5TreeNode root = new N5TreeNode("");
		root.addPath("a/b/c");
		assertTrue(root.getDescendant("a/b/c").isPresent());
		// make sure the tree has expected structure
		assertTrue(root.structureEquals(control));
		assertEquals(4, N5TreeNode.flattenN5Tree(root).count());

		root.add(new N5TreeNode("ant"));
		assertTrue(root.getDescendant("ant").isPresent());

		control.add(ant);
		ant.add(bat);

		root.addPath("ant/bat");
		assertTrue(root.getDescendant("ant/bat").isPresent());
		// make sure the tree has expected structure
		assertTrue(root.structureEquals(control));
		assertEquals(6, N5TreeNode.flattenN5Tree(root).count());

		// ensure children can be added from nodes that are not the root
		final N5TreeNode c0Node = new N5TreeNode("c0");
		root.add(c0Node);
		c0Node.addPath("c0/s0");
		assertTrue(root.getDescendant("c0/s0").isPresent());

		// ensure no new nodes are added when trying to add invalid path
		assertEquals(2, N5TreeNode.flattenN5Tree(c0Node).count());
		assertNull(c0Node.addPath("x/y/z"));
		assertEquals(2, N5TreeNode.flattenN5Tree(c0Node).count());
	}

	@Test
	public void testAddPath() {

		final N5TreeNode root = new N5TreeNode("");

		root.addPath("a");
		assertTrue(root.getDescendant("a").isPresent());
		assertFalse(root.getDescendant("a/b").isPresent());

		root.removeAllChildren();
		assertFalse(root.getDescendant("a").isPresent());
		assertFalse(root.getDescendant("a/b").isPresent());

		root.addPath("a/b");
		assertTrue(root.getDescendant("a").isPresent());
		assertTrue(root.getDescendant("a/b").isPresent());

		// add path to non-root node
		final N5TreeNode a = new N5TreeNode("a");
		a.addPath("a/b");
		assertTrue(a.getDescendant("a/b").isPresent());

		a.addPath("c");
		// not added because the added path is not a child of "a"
		assertFalse(a.getDescendant("a/c").isPresent());
	}

}

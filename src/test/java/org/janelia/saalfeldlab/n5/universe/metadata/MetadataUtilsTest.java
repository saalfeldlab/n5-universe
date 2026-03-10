package org.janelia.saalfeldlab.n5.universe.metadata;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class MetadataUtilsTest
{
	@Test
	public void testReversedCopyWithStrings()
	{
		String[] input = { "apple", "banana", "cherry" };
		String[] expected = { "cherry", "banana", "apple" };
		assertArrayEquals( expected, MetadataUtils.reversedCopy( input ) );
	}

	@Test
	public void testReversedCopyWithIntegers()
	{
		Integer[] input = { 1, 2, 3, 4, 5 };
		Integer[] expected = { 5, 4, 3, 2, 1 };
		assertArrayEquals( expected, MetadataUtils.reversedCopy( input ) );
	}

	@Test
	public void testReversedCopyWithEmptyArray()
	{
		String[] input = {};
		String[] expected = {};
		assertArrayEquals( expected, MetadataUtils.reversedCopy( input ) );
	}
}

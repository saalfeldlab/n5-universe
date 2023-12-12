package org.janelia.saalfeldlab.n5.universe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;

public class N5MetadataUtils {

	protected final static ArrayList<N5MetadataParser<?>> PARSERS;
	protected final static ArrayList<N5MetadataParser<?>> GROUP_PARSERS;
	static {
		PARSERS = new ArrayList<>();
		resetParsers();

		GROUP_PARSERS = new ArrayList<>();
		resetGroupParsers();
	};

	public static void resetParsers() {

		PARSERS.clear();
		PARSERS.add(new N5CosemMetadataParser());
		PARSERS.add(new N5SingleScaleMetadataParser());
		PARSERS.add(new CanonicalMetadataParser());
		PARSERS.add(new N5GenericSingleScaleMetadataParser());
	}

	public static void resetGroupParsers() {

		GROUP_PARSERS.clear();
		GROUP_PARSERS.add(new OmeNgffMetadataParser());
		GROUP_PARSERS.add(new N5CosemMultiScaleMetadata.CosemMultiScaleParser());
		GROUP_PARSERS.add(new N5ViewerMultiscaleMetadataParser());
		GROUP_PARSERS.add(new CanonicalMetadataParser());
	}

	/**
	 * Adds a parser to the current default list.
	 * 
	 * @param parser the parser to add 
	 * @param append appends if true, prepends otherwise
	 */
	public static void addDefaultParser(N5MetadataParser<?> parser, boolean append) {

		if (append)
			PARSERS.add(parser);
		else
			PARSERS.add(0, parser);
	}

	/**
	 * Prepends to the default parser list.
	 *
	 * @param parser
	 *            metadata parser to add
	 */
	public static void addDefaultParser(N5MetadataParser<?> parser) {

		addDefaultParser(parser, false);
	}

	/**
	 * Adds a group parser to the default list.
	 * 
	 * @param groupParser the parser to add 
	 * @param append appends if true, prepends otherwise
	 */
	public static void addDefaultGroupParser(N5MetadataParser<?> groupParser, boolean append) {

		if (append)
			GROUP_PARSERS.add(groupParser);
		else
			GROUP_PARSERS.add(0, groupParser);
	}

	/**
	 * Prepends to the default group parser list.
	 * 
	 * @param groupParser the group parser to add
	 */
	public static void addDefaultGroupParser(N5MetadataParser<?> groupParser) {

		addDefaultGroupParser(groupParser, false);
	}

	public static N5Metadata parseMetadata(final N5Reader n5, final String group) {

		return parseMetadata(n5, group, true);
	}

	public static N5Metadata parseMetadata(final N5Reader n5, final String group, final boolean parseWholeTree) {

		return parseMetadata(n5, group, parseWholeTree, Executors.newCachedThreadPool(), PARSERS, GROUP_PARSERS);
	}

	public static N5Metadata parseMetadata(final N5Reader n5, final String group, final boolean parseWholeTree,
			final ExecutorService exec, final List<N5MetadataParser<?>> parsers, final List<N5MetadataParser<?>> groupParsers) {

		final N5TreeNode root = parseMetadataTree(n5, group, exec, parsers, groupParsers);
		if (root == null)
			return null;
		else
			return root.getDescendant(group).map(N5TreeNode::getMetadata).orElse(null);
	}

	public static N5TreeNode parseMetadataTree(final N5Reader n5) {

		return parseMetadataTree(n5, Executors.newCachedThreadPool(), PARSERS, GROUP_PARSERS);
	}

	public static N5TreeNode parseMetadataTree(final N5Reader n5, final String group) {

		return parseMetadataTree(n5, Executors.newCachedThreadPool(), PARSERS, GROUP_PARSERS);
	}

	public static N5TreeNode parseMetadataTree(final N5Reader n5, final ExecutorService exec) {

		return parseMetadataTree(n5, exec, PARSERS, GROUP_PARSERS);
	}

	public static N5TreeNode parseMetadataTree(final N5Reader n5, final List<N5MetadataParser<?>> parsers, final List<N5MetadataParser<?>> groupParsers) {

		return parseMetadataTree(n5, Executors.newCachedThreadPool(), parsers, groupParsers);
	}

	public static N5TreeNode parseMetadataTree(final N5Reader n5, final ExecutorService exec, final List<N5MetadataParser<?>> parsers,
			final List<N5MetadataParser<?>> groupParsers) {

		return parseMetadataTree(n5, "", exec, parsers, groupParsers);
	}

	public static N5TreeNode parseMetadataTree(final N5Reader n5, final String group, final ExecutorService exec,
			final List<N5MetadataParser<?>> parsers, final List<N5MetadataParser<?>> groupParsers) {

		// TODO expose group path
		// any more optimization is developers responsibility

		// TODO is it worth it to cache successful parser per group? probably
		// not now.

		// TODO currently can't deal with multiple valid parsers nicely

		// TODO eliminate invalid parsers as you go (based on keys that the
		// parsers declare are required)?
		final N5DatasetDiscoverer disc = new N5DatasetDiscoverer(n5, exec, parsers, groupParsers);
		try {
			return disc.discoverAndParseRecursive(group);
		} catch (IOException e) {}

		return null;
	}

}

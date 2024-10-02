/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.universe;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataGroup;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sawano.java.text.AlphanumericComparator;

/**
 * This class aids in detecting and parsing datasets in an N5 container.
 * <p>
 * An N5DatasetDiscoverer specifies the types of {@link N5MetadataParser}s to
 * attempt, and an {@link ExecutorService} that enables parsing in parallel. The
 * parsers are passed to the constructor in a list. Group parsers are called
 * after all others are called, and should be used when a parsers result depends
 * on its children.
 * <p>
 * The {@link discoverAndParseRecursive} method returns a {@link N5TreeNode}
 * containing all child nodes, each of which contains parsed metadata. For each
 * group/dataset, the parsers will be called in order, and will return the first
 * non-empty result. As such parsers should be ordered from most- to
 * least-strict.
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 *
 */
public class N5DatasetDiscoverer {

	private static final Logger LOG = LoggerFactory.getLogger(N5DatasetDiscoverer.class);

	public static final N5MetadataParser<?>[] DEFAULT_PARSERS = new N5MetadataParser<?>[]{
			new N5CosemMetadataParser(),
			new N5SingleScaleMetadataParser(),
			new CanonicalMetadataParser(),
			new N5GenericSingleScaleMetadataParser()
	};

	public static final N5MetadataParser<?>[] DEFAULT_GROUP_PARSERS = new N5MetadataParser<?>[]{
			new OmeNgffMetadataParser(),
			new N5CosemMultiScaleMetadata.CosemMultiScaleParser(),
			new N5ViewerMultiscaleMetadataParser(),
			new CanonicalMetadataParser(),
	};

	private final List<N5MetadataParser<?>> metadataParsers;
	private final List<N5MetadataParser<?>> groupParsers;

	private final Comparator<? super String> comparator;

	private final Predicate<N5TreeNode> filter;

	private final ExecutorService executor;

	private N5TreeNode root;

	private String groupSeparator;

	private N5Reader n5;

	/**
	 * Creates an N5 discoverer with alphanumeric sorting order of
	 * groups/datasets (such as, s9 goes before s10).
	 *
	 * @param executor
	 *            the executor
	 * @param metadataParsers
	 *            metadata parsers
	 * @param groupParsers
	 *            group parsers
	 */
	public N5DatasetDiscoverer(final ExecutorService executor,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(executor,
				Optional.of(new AlphanumericComparator(Collator.getInstance())),
				null,
				metadataParsers,
				groupParsers);
	}

	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(n5,
				executor,
				Optional.of(new AlphanumericComparator(Collator.getInstance())),
				null,
				metadataParsers,
				groupParsers);
	}

	/**
	 * Creates an N5 discoverer.
	 *
	 * @param metadataParsers
	 *            metadata parsers
	 * @param groupParsers
	 *            group parsers
	 */
	public N5DatasetDiscoverer(
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(Executors.newSingleThreadExecutor(),
				Optional.of(new AlphanumericComparator(Collator.getInstance())),
				null,
				metadataParsers,
				groupParsers);
	}

	/**
	 * Creates an N5 discoverer.
	 *
	 * @param n5
	 *            n5 reader
	 * @param metadataParsers
	 *            metadata parsers
	 * @param groupParsers
	 *            group parsers
	 */
	public N5DatasetDiscoverer(final N5Reader n5,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(n5,
				Executors.newSingleThreadExecutor(),
				Optional.of(new AlphanumericComparator(Collator.getInstance())),
				null,
				metadataParsers,
				groupParsers);
	}

	public N5DatasetDiscoverer(
			final ExecutorService executor,
			final Predicate<N5TreeNode> filter,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(executor,
				Optional.of(new AlphanumericComparator(Collator.getInstance())),
				filter,
				metadataParsers,
				groupParsers);
	}

	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final Predicate<N5TreeNode> filter,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(n5,
				executor,
				Optional.of(new AlphanumericComparator(Collator.getInstance())),
				filter,
				metadataParsers,
				groupParsers);
	}

	public N5DatasetDiscoverer(
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(executor, comparator, null, metadataParsers, groupParsers);
	}

	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this(n5, executor, comparator, null, metadataParsers, groupParsers);
	}

	/**
	 * Creates an N5 discoverer.
	 * <p>
	 * If the optional parameter {@code comparator} is specified, the groups and
	 * datasets will be listed in the order determined by this comparator.
	 *
	 * @param executor
	 *            the executor
	 * @param comparator
	 *            optional string comparator
	 * @param filter
	 *            the dataset filter
	 * @param metadataParsers
	 *            metadata parsers
	 * @param groupParsers
	 *            group parsers
	 */
	public N5DatasetDiscoverer(
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
			final Predicate<N5TreeNode> filter,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this.executor = executor;
		this.comparator = comparator.orElseGet(null);
		this.filter = filter;
		this.metadataParsers = metadataParsers;
		this.groupParsers = groupParsers;
	}

	/**
	 * Creates an N5 discoverer.
	 * <p>
	 * If the optional parameter {@code comparator} is specified, the groups and
	 * datasets will be listed in the order determined by this comparator.
	 *
	 * @param n5
	 *            the n5 reader
	 * @param executor
	 *            the executor
	 * @param comparator
	 *            optional string comparator
	 * @param filter
	 *            the dataset filter
	 * @param metadataParsers
	 *            metadata parsers
	 * @param groupParsers
	 *            group parsers
	 */
	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
			final Predicate<N5TreeNode> filter,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) {

		this.n5 = n5;
		this.executor = executor;
		this.comparator = comparator.orElseGet(null);
		this.filter = filter;
		this.metadataParsers = metadataParsers;
		this.groupParsers = groupParsers;
	}

	public static void parseMetadata(final N5Reader n5, final N5TreeNode node,
			final List<N5MetadataParser<?>> metadataParsers) throws IOException {

		parseMetadata(n5, node, metadataParsers, new ArrayList<>());
	}

	/**
	 * Parses metadata for a node using the given parsers, stopping after the
	 * first success.
	 *
	 * @param n5
	 *            the N5Reader
	 * @param node
	 *            the tree node
	 * @param metadataParsers
	 *            list of metadata parsers
	 * @param groupParsers
	 *            list of group parsers
	 * @throws IOException
	 *             the exception
	 */
	public static void parseMetadata(final N5Reader n5, final N5TreeNode node,
			final List<N5MetadataParser<?>> metadataParsers,
			final List<N5MetadataParser<?>> groupParsers) throws IOException {

		// Go through all parsers to populate metadata
		for (final N5MetadataParser<?> parser : metadataParsers) {
			try {
				Optional<? extends N5Metadata> parsedMeta;
				parsedMeta = parser.apply(n5, node);

				parsedMeta.ifPresent(node::setMetadata);
				if (parsedMeta.isPresent())
					break;
			} catch (final Exception ignored) {}
		}

		// this may be a group (e.g. multiscale pyramid) try to parse groups
		if ((node.getMetadata() == null) && !node.childrenList().isEmpty() && groupParsers != null) {
			for (final N5MetadataParser<?> gp : groupParsers) {
				final Optional<? extends N5Metadata> groupMeta = gp.apply(n5, node);
				groupMeta.ifPresent(node::setMetadata);
				if (groupMeta.isPresent())
					break;
			}
		}
	}

	public static boolean trim(final N5TreeNode node) {

		return trim(node, x -> {});
	}

	/**
	 * Removes branches of the N5 container tree that do not contain any nodes
	 * that can be opened (nodes with metadata).
	 *
	 * @param node
	 *            the node
	 * @param callback
	 *            the callback function
	 * @return {@code true} if the branch contains a node that can be opened,
	 *         {@code false} otherwise
	 */
	public static boolean trim(final N5TreeNode node, final Consumer<N5TreeNode> callback) {

		final List<N5TreeNode> children = node.childrenList();
		if (children.isEmpty()) {
			return node.getMetadata() != null;
		}

		boolean ret = false;
		for (final Iterator<N5TreeNode> it = children.iterator(); it.hasNext();) {
			final N5TreeNode childNode = it.next();
			if (!trim(childNode, callback)) {
				it.remove();
				callback.accept(childNode);
			} else {
				ret = true;
			}
		}

		return ret || node.getMetadata() != null;
	}

	public static void sort(final N5TreeNode node, final Comparator<? super String> comparator,
			final Consumer<N5TreeNode> callback) {

		final List<N5TreeNode> children = node.childrenList();
		children.sort(Comparator.comparing(N5TreeNode::toString, comparator));

		if (callback != null) {
			callback.accept(node);
		}

		for (final N5TreeNode childNode : node.childrenList()) {
			sort(childNode, comparator, callback);
		}
	}

	public void sort(final N5TreeNode node, final Consumer<N5TreeNode> callback) {

		if (comparator != null) {
			sort(node, comparator, callback);
		}
	}

	public void sort(final N5TreeNode node) {

		if (comparator != null)
			sort(node, comparator, null);
	}

	/**
	 * Recursively discovers and parses metadata for datasets that are children
	 * of the given base path using {@link N5Reader#deepList}. Returns an
	 * {@link N5TreeNode} that can be displayed as a JTree.
	 *
	 * @param base
	 *            the base path
	 * @return the n5 tree node
	 * @throws IOException
	 *             the io exception
	 */
	public N5TreeNode discoverAndParseRecursive(final String base) throws IOException {

		return discoverAndParseRecursive(base, x -> {});
	}

	public N5TreeNode discoverAndParseRecursive(final String base, final Consumer<N5TreeNode> callback) throws IOException {

		groupSeparator = n5.getGroupSeparator();
		root = new N5TreeNode(base);
		discoverAndParseRecursive(root, callback);
		return root;
	}

	public N5TreeNode discoverAndParseRecursive(final N5TreeNode root) throws IOException {

		return discoverAndParseRecursive(root, x -> {});
	}

	public N5TreeNode discoverAndParseRecursive(final N5TreeNode root, final Consumer<N5TreeNode> callback) throws IOException {

		groupSeparator = n5.getGroupSeparator();

		String[] datasetPaths;
		try {
			datasetPaths = n5.deepList(root.getPath(), executor);
			N5TreeNode.fromFlatList(root, datasetPaths, groupSeparator);
		} catch (final Exception e) {
			return null;
		}
		callback.accept(root);

		parseMetadataRecursive(root, callback);
		sortAndTrimRecursive(root, callback);

		return root;
	}

	public N5TreeNode discoverAndParseRecursive(final N5TreeNode root, final Predicate<String> filter,
			final Consumer<N5TreeNode> callback) throws IOException {

		groupSeparator = n5.getGroupSeparator();

		String[] datasetPaths;
		try {
			datasetPaths = n5.deepList(root.getPath(), filter, executor);
			N5TreeNode.fromFlatList(root, datasetPaths, groupSeparator);
		} catch (final Exception e) {
			return null;
		}
		callback.accept(root);

		parseMetadataRecursive(root, callback);
		sortAndTrimRecursive(root, callback);

		return root;
	}

	public N5TreeNode parse(final String dataset) {

		final N5TreeNode node = new N5TreeNode(dataset);
		return parse(node);
	}

	public N5TreeNode parse(final N5TreeNode node) {

		// Go through all parsers to populate metadata
		for (final N5MetadataParser<?> parser : metadataParsers) {
			try {
				final Optional<? extends N5Metadata> metadata = parser.apply(n5, node);
				if (metadata.isPresent()) {
					node.setMetadata(metadata.get());
					break;
				}
			} catch (final Exception e) {}
		}
		return node;
	}

	public void sortAndTrimRecursive(final N5TreeNode node) {

		sortAndTrimRecursive(node, x -> {});
	}

	public void sortAndTrimRecursive(final N5TreeNode node, final Consumer<N5TreeNode> callback) {

		trim(node, callback);

		if (comparator != null)
			sort(node, callback);

		for (final N5TreeNode c : node.childrenList())
			sortAndTrimRecursive(c, callback);
	}

	public void filterRecursive(final N5TreeNode node) {

		if (filter == null)
			return;

		if (!filter.test(node))
			node.setMetadata(null);

		for (final N5TreeNode c : node.childrenList())
			filterRecursive(c);
	}

	/**
	 * Parses metadata for the given node and all children in parallel using
	 * this object's executor.
	 *
	 * @param rootNode
	 *            the root node
	 */
	public void parseMetadataRecursive(final N5TreeNode rootNode) {

		parseMetadataRecursive(rootNode, x -> {});
	}

	/**
	 * Parses metadata for the given node and all children in parallel using
	 * this object's executor. The given function is called for every node after
	 * parsing is completed, successful or not.
	 *
	 * @param rootNode
	 *            the root node
	 * @param callback
	 *            the callback function
	 */
	public void parseMetadataRecursive(final N5TreeNode rootNode, final Consumer<N5TreeNode> callback) {

		/* depth first, check if we have children */
		final List<N5TreeNode> children = rootNode.childrenList();
		final ArrayList<Future<?>> childrenFutures = new ArrayList<Future<?>>();
		if (!children.isEmpty()) {
			/* If possible, parallelize the metadata parsing. */
			if (executor instanceof ThreadPoolExecutor) {
				final ThreadPoolExecutor threadPoolExec = (ThreadPoolExecutor)this.executor;
				for (final N5TreeNode child : children) {
					final boolean useExec;
					synchronized (executor) {
						/*
						 * Since the parents wait for the children to finish, if
						 * there aren't enough threads to parse all the children
						 * (DFS), this could lock up. So we check if there are
						 * any extra threads; if not, execute if current thread.
						 */
						useExec = (threadPoolExec.getActiveCount() < threadPoolExec.getMaximumPoolSize() - 1);
					}
					if (useExec) {
						childrenFutures.add(this.executor.submit(() -> parseMetadataRecursive(child, callback)));
					} else {
						parseMetadataRecursive(child, callback);
					}
				}
			} else {
				for (final N5TreeNode child : children) {
					parseMetadataRecursive(child, callback);
				}
			}
		}

		for (final Future<?> childrenFuture : childrenFutures) {
			try {
				childrenFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Error encountered during metadata parsing", e);
				throw new RuntimeException(e);
			}
		}

		try {
			N5DatasetDiscoverer.parseMetadata(n5, rootNode, metadataParsers, groupParsers);
		} catch (final Exception e) {}
		LOG.debug("parsed metadata for: {}:\t found: {}", rootNode.getPath(),
				rootNode.getMetadata() == null ? "NONE" : rootNode.getMetadata().getClass().getSimpleName());

		callback.accept(rootNode);

		if (rootNode.getMetadata() instanceof N5MetadataGroup) {

			// spatial metadata groups may update their children metadata, and
			// to be safe,
			// run the callback on its children
			@SuppressWarnings("unchecked")
			final N5MetadataGroup<? extends N5Metadata> grpMeta = (N5MetadataGroup<N5Metadata>)rootNode.getMetadata();
			for (final N5Metadata child : grpMeta.getChildrenMetadata()) {
				rootNode.getDescendant(child.getPath()).ifPresent(x -> {
					callback.accept(x);
				});
			}
		}
	}

	public static final List<N5MetadataParser<?>> fromParsers(final N5MetadataParser<?>[] parsers) {

		return Arrays.asList(parsers);
	}

	public static N5TreeNode discover(final N5Reader n5, final List<N5MetadataParser<?>> parsers, final List<N5MetadataParser<?>> groupParsers) {

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5,
				Executors.newCachedThreadPool(),
				parsers, groupParsers);
		try {
			return discoverer.discoverAndParseRecursive("");
		} catch (final IOException e) {}
		return null;
	}

	public static N5TreeNode discover(final N5Reader n5, final List<N5MetadataParser<?>> parsers) {

		return discover(n5, parsers, null);
	}

	public static N5TreeNode discover(final N5Reader n5) {

		return discover(n5,
				Arrays.asList(DEFAULT_PARSERS),
				Arrays.asList(DEFAULT_GROUP_PARSERS));
	}

}

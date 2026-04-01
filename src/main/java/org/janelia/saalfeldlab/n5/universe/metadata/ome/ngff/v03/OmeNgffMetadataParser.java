package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;

//public class OmeNgffMetadataParser implements N5MetadataParser< OmeNgffMetadata >, N5MetadataWriter< OmeNgffMetadata >
//{
//	protected final boolean assumeChildren;
//
//	public OmeNgffMetadataParser(final boolean assumeChildren) {
//
//		this.assumeChildren = assumeChildren;
//	}
//	
//	public OmeNgffMetadataParser() {
//
//		this(false);
//	}
//
//	@Override
//	public Optional< OmeNgffMetadata > parseMetadata( N5Reader n5, N5TreeNode node )
//	{
//		OmeNgffV03MultiScaleMetadata[] multiscales;
//		try {
//			multiscales = n5.getAttribute(node.getPath(), "multiscales", OmeNgffV03MultiScaleMetadata[].class);
//		} catch (final Exception e) {
//			return Optional.empty();
//		}
//
//		if (multiscales == null || multiscales.length == 0) {
//			return Optional.empty();
//		}		
//
//		int nd = -1;
//		final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();
//
//		DatasetAttributes[] attrs = null;
//		if( assumeChildren ) {
//
//			for (int j = 0; j < multiscales.length; j++) {
//
//				final OmeNgffV03MultiScaleMetadata ms = multiscales[j];
//
//				nd = ms.axes.length;
//				final int numScales = ms.datasets.length;
//				attrs = new DatasetAttributes[numScales];
//				for (int i = 0; i < numScales; i++) {
//
//					// TODO check existence here or elsewhere?
//					final N5TreeNode child = new N5TreeNode(
//							MetadataUtils.canonicalPath(node, ms.datasets[i].path));
//					final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(child.getPath());
//					if (dsetAttrs == null)
//						return Optional.empty();
//
//					attrs[i] = dsetAttrs;
//					node.childrenList().add(child);
//				}
//
//				final NgffV03SingleScaleAxesMetadata[] msChildrenMeta = OmeNgffV03MultiScaleMetadata.buildMetadata(
//						nd, node.getPath(), ms.datasets, ms.axes, attrs, ms.metadata);
//
//				// add to scale level nodes map
//				node.childrenList().forEach(n -> {
//					scaleLevelNodes.put(n.getPath(), n);
//				});
//			}
//
//		} else {
//
//			for (final N5TreeNode childNode : node.childrenList()) {
//				if (childNode.isDataset() && childNode.getMetadata() != null) {
//					scaleLevelNodes.put(childNode.getPath(), childNode);
//					if (nd < 0)
//						nd = ((N5DatasetMetadata) childNode.getMetadata()).getAttributes().getNumDimensions();
//				}
//			}
//
//			if (nd < 0)
//				return Optional.empty();
//
//			for (int j = 0; j < multiscales.length; j++) {
//
//				final OmeNgffV03MultiScaleMetadata ms = multiscales[j];
//				final String[] paths = ms.getPaths();
//				attrs = new DatasetAttributes[ms.getPaths().length];
//				final N5DatasetMetadata[] dsetMeta = new N5DatasetMetadata[paths.length];
//				for (int i = 0; i < paths.length; i++) {
//					dsetMeta[i] = ((N5DatasetMetadata)scaleLevelNodes.get(MetadataUtils.canonicalPath(node, paths[i])).getMetadata());
//					attrs[i] = dsetMeta[i].getAttributes();
//				}
//			}
//		}
//
//		/*
//		 * Need to replace all children with new children with the metadata from
//		 * this object
//		 */
//		for (int j = 0; j < multiscales.length; j++) {
//
//			final OmeNgffV03MultiScaleMetadata ms = multiscales[j];
//			final NgffV03SingleScaleAxesMetadata[] msChildrenMeta = OmeNgffV03MultiScaleMetadata.buildMetadata(
//					nd, node.getPath(), ms.datasets, ms.axes, attrs, ms.metadata);
//
//			MetadataUtils.updateChildrenMetadata(node, msChildrenMeta, false);
//			multiscales[j] = new OmeNgffV03MultiScaleMetadata(ms, msChildrenMeta);
//		}
//
//		return Optional.of(new OmeNgffMetadata(node.getPath(), multiscales));
//	}
//	
//	private static ScaleCoordinateTransformation getImpliedScale( final int nd, final int i ) {
//		final double[] res = new double[nd];
//		Arrays.fill(res, Math.pow(2, i));
//		return new ScaleCoordinateTransformation(res);
//	}
//
//	private static ScaleCoordinateTransformation[] getScales(final int nd, final int numScales) {
//		final ScaleCoordinateTransformation[] cts = new ScaleCoordinateTransformation[numScales];
//		for (int i = 0; i < cts.length; i++) {
//			cts[i] = getImpliedScale(nd, i);
//		}
//		return cts;
//	}
//
//	@Override
//	public void writeMetadata(OmeNgffMetadata t, N5Writer n5, String path) throws Exception {
//
//		n5.setAttribute(path, "multiscales", t.getMultiscales() );
//
//		if (t.getMultiscales().length <= 0)
//			return;
//
//		for( int i = 0; i < t.getMultiscales().length; i++ ) {
//			final OmeNgffV03MultiScaleMetadata ms = t.getMultiscales()[i];
//			for( int j = 0; j < t.getMultiscales().length; j++ ) {
//
//				final OmeNgffDataset ds = ms.datasets[j];
//				final NgffV03SingleScaleAxesMetadata meta = ms.getChildrenMetadata()[j];
//				final String childPath = path + "/" + ds.path;
//			}
//		}
//	}
//
//}

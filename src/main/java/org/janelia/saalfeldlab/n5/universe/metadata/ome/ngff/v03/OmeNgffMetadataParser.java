package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03;

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
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v03.OmeNgffMultiScaleMetadata.OmeNgffDataset;

public class OmeNgffMetadataParser implements N5MetadataParser< OmeNgffMetadata >, N5MetadataWriter< OmeNgffMetadata >
{
	@Override
	public Optional< OmeNgffMetadata > parseMetadata( N5Reader n5, N5TreeNode node )
	{
		OmeNgffMultiScaleMetadata[] multiscales;
		try
		{
			multiscales = n5.getAttribute( node.getPath(), "multiscales", OmeNgffMultiScaleMetadata[].class );
		} catch ( final Exception e )
		{
			return Optional.empty();
		}

		if ( multiscales == null || multiscales.length == 0 )
		{
			return Optional.empty();
		}

		int nd = -1;
		final Map< String, N5TreeNode > scaleLevelNodes = new HashMap<>();
		for ( final N5TreeNode childNode : node.childrenList() )
		{
			if ( childNode.isDataset() && childNode.getMetadata() != null )
			{
				scaleLevelNodes.put( childNode.getPath(), childNode );
				if( nd < 0 )
					nd = ((N5DatasetMetadata)childNode.getMetadata()).getAttributes().getNumDimensions();
			}
		}

		if( nd < 0 )
			return Optional.empty();

		/*
		 * Need to replace all children with new children with the
		 * metadata from this
		 */
		for ( final OmeNgffMultiScaleMetadata ms : multiscales )
		{
			ms.path = node.getPath();
			final String[] paths = ms.getPaths();
			final DatasetAttributes[] attrs = new DatasetAttributes[ ms.getPaths().length ];
			final N5DatasetMetadata[] dsetMeta = new N5DatasetMetadata[ paths.length ];
			for( int i = 0; i < paths.length; i++ )
			{
				dsetMeta[ i ] = ((N5DatasetMetadata)scaleLevelNodes.get( MetadataUtils.canonicalPath( node, paths[ i ] ) ).getMetadata());
				attrs[ i ] = dsetMeta[ i ].getAttributes();
			}

			final N5SingleScaleMetadata[] msChildrenMeta = ms.buildChildren( nd, attrs );
			MetadataUtils.updateChildrenMetadata( node, msChildrenMeta, false );
			ms.childrenMetadata = msChildrenMeta;

			ms.childrenAttributes = attrs;
		}
		return Optional.of( new OmeNgffMetadata( node.getPath(), multiscales ) );
	}

	@Override
	public void writeMetadata(OmeNgffMetadata t, N5Writer n5, String path) throws Exception {

		n5.setAttribute(path, "multiscales", t.getMultiscales() );

		if (t.getMultiscales().length <= 0)
			return;

		for( int i = 0; i < t.getMultiscales().length; i++ ) {
			final OmeNgffMultiScaleMetadata ms = t.getMultiscales()[i];
			for( int j = 0; j < t.getMultiscales().length; j++ ) {

				final OmeNgffDataset ds = ms.datasets[j];
				final N5SingleScaleMetadata meta = ms.childrenMetadata[j];
				final String childPath = path + "/" + ds.path;
			}
		}

	}


}

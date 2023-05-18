package org.janelia.saalfeldlab.n5.universe.metadata;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5URL;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;

public class MetadataUtils
{

	/**
	 * Returns a new {@link N5SingleScaleMetadata} equal to the baseMetadata, but with 
	 * {@link DatasetAttributes} coming from datasetMetadata.ew
	 * <p>
	 * 
	 * @param baseMetadata metadata
	 * @param datasetMetadata dataset metadata
	 */
	public static N5SingleScaleMetadata setDatasetAttributes( N5SingleScaleMetadata baseMetadata, N5DatasetMetadata datasetMetadata )
	{
		if( baseMetadata.getPath().equals( datasetMetadata.getPath() ))
			return new N5SingleScaleMetadata( baseMetadata.getPath(), baseMetadata.spatialTransform3d(), 
					baseMetadata.getDownsamplingFactors(), baseMetadata.getPixelResolution(), baseMetadata.getOffset(), 
					baseMetadata.unit(), datasetMetadata.getAttributes() );
		else
			return null;
	}
	
	public static N5SingleScaleMetadata[] updateChildrenDatasetAttributes( N5SingleScaleMetadata[] baseMetadata, N5DatasetMetadata[] datasetMetadata )
	{
		final HashMap<String,N5SingleScaleMetadata> bases = new HashMap<>();
		Arrays.stream( baseMetadata ).forEach( x -> { bases.put( x.getPath(), x ); } );

		return ( N5SingleScaleMetadata[] ) Arrays.stream( datasetMetadata ).map( x -> { 
			N5SingleScaleMetadata b = bases.get( x.getPath() );
			if( b == null )
				return null;
			else
				return setDatasetAttributes( b, x );
		} ).filter( x -> x != null ).toArray();
	}
	
	public static void updateChildrenMetadata( N5TreeNode parent, N5Metadata[] childrenMetadata )
	{
		final HashMap<String,N5Metadata> children = new HashMap<>();
		Arrays.stream( childrenMetadata ).forEach( x -> { children.put( x.getPath(), x ); } );
		parent.childrenList().forEach( c -> {
			N5Metadata m = children.get( c.getPath() );
			if( m != null )
				c.setMetadata( m );
		});
	}

	public static String canonicalPath( N5TreeNode parent, String child )
	{
		return canonicalPath( parent.getPath(), child );
	}
	
	public static String canonicalPath( final String parent, final String child )
	{
		try
		{
			final N5URL url = new N5URL( "?/" + parent + "/" + child );
			return url.normalizeGroupPath();
		}
		catch ( URISyntaxException e )
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Element-wise power. Returns an array y such that y[i] = x[i] ^ d
	 * 
	 * @param x array
	 * @param d exponent
	 * @return result
	 */
	public static double[] pow( final double[] x, final int d )
	{
		final double[] y = new double[ x.length ];
		Arrays.fill( y, 1 );
		for ( int i = 0; i < d; i++ )
			for ( int j = 0; j < x.length; j++ )
				y[ j ] *= x[ j ]; 

		return y;
	}

}
